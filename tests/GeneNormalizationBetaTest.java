import GNormPluslib.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

/** Exercise native GN with a synthetic species-scoped lexicon, not a model accuracy test. */
public class GeneNormalizationBetaTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("gnorm2-beta-");
        try {
            // Deliberately distinct identities for beta, alpha and the beta-less name.
            GNormPlus.PT_Gene.insertMention("gsk3b", "9606:101|10090:201");
            GNormPlus.PT_Gene.insertMention("tgfb1", "9606:102|10090:202");
            GNormPlus.PT_Gene.insertMention("gsk3a", "9606:103|10090:203");
            GNormPlus.PT_Gene.insertMention("sirt3", "9606:104|10090:204");
            GNormPlus.PT_Gene.insertMention("gsk3", "9606:301|10090:401");
            GNormPlus.PT_Gene.insertMention("tgf1", "9606:302|10090:402");
            String[] mentions = {"GSK3 β", "GSK3 b", "TGF-β 1", "TGF-b 1", "GSK3 a", "SIRT3"};
            String[] taxa = {"9606", "10090", "10116"};
            Map<String, String> expected = new TreeMap<>();
            StringBuilder xml = new StringBuilder("<collection><source>test</source><date>20261106</date><key>test</key>");
            for (int t = 0; t < taxa.length; t++) {
                String text = (t == 0 ? "Human " : t == 1 ? "Mouse " : "Rat ") + String.join("; ", mentions) + ".";
                xml.append("<document><id>").append(taxa[t]).append("</id><passage><infon key=\"type\">abstract</infon><offset>0</offset><text>")
                        .append(text).append("</text>");
                for (int m = 0; m < mentions.length; m++) {
                    String mention = mentions[m];
                    xml.append("<annotation id=\"").append(m).append("\"><infon key=\"type\">Gene</infon>")
                            .append("<infon key=\"Identifier\">Tax:").append(taxa[t])
                            .append("</infon><location offset=\"").append(text.indexOf(mention))
                            .append("\" length=\"").append(mention.length()).append("\"/><text>")
                            .append(mention).append("</text></annotation>");
                    if (t < 2) expected.put(taxa[t] + ":" + mention, String.valueOf((t + 1) * 100 + (m < 2 ? 1 : m < 4 ? 2 : m - 1)));
                }
                xml.append("</passage></document>");
            }
            xml.append("</collection>");
            Path input = root.resolve("input.xml"), output = root.resolve("output.xml");
            byte[] source = xml.toString().getBytes(StandardCharsets.UTF_8);
            Files.write(input, source);
            GNormPlus.BioCDocobj.BioCReaderWithAnnotation(input.toString());
            GN gn = new GN();
            gn.PreProcessing4GN(input.toString(), root.resolve("unused.xml").toString());
            gn.GeneNormalization(input.toString(), output.toString(), false);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            Document before = factory.newDocumentBuilder().parse(input.toFile());
            Document after = factory.newDocumentBuilder().parse(output.toFile());
            Map<String, String> observed = new TreeMap<>();
            NodeList docs = after.getElementsByTagName("document");
            if (docs.getLength() != taxa.length) throw new AssertionError("Document count changed");
            for (int d = 0; d < docs.getLength(); d++) {
                Element doc = (Element) docs.item(d);
                Element oldDoc = (Element) before.getElementsByTagName("document").item(d);
                String id = doc.getElementsByTagName("id").item(0).getTextContent();
                if (!id.equals(taxa[d])) throw new AssertionError("Document identity changed");
                String text = doc.getElementsByTagName("text").item(0).getTextContent();
                if (!text.equals(oldDoc.getElementsByTagName("text").item(0).getTextContent())) throw new AssertionError("Text changed");
                NodeList annotations = doc.getElementsByTagName("annotation");
                for (int a = 0; a < annotations.getLength(); a++) {
                    Element ann = (Element) annotations.item(a);
                    String mention = ann.getElementsByTagName("text").item(0).getTextContent();
                    Element loc = (Element) ann.getElementsByTagName("location").item(0);
                    int offset = Integer.parseInt(loc.getAttribute("offset"));
                    int length = Integer.parseInt(loc.getAttribute("length"));
                    if (offset != text.indexOf(mention) || length != mention.length() || !text.substring(offset, offset + length).equals(mention))
                        throw new AssertionError("Mention text or coordinates changed");
                    NodeList infons = ann.getElementsByTagName("infon");
                    for (int i = 0; i < infons.getLength(); i++) {
                        Element infon = (Element) infons.item(i);
                        if (infon.getAttribute("key").equals("NCBI Gene")) {
                            if (observed.put(id + ":" + mention, infon.getTextContent()) != null) throw new AssertionError("Duplicate mention");
                        }
                    }
                }
            }
            if (!Arrays.equals(Files.readAllBytes(input), source)) throw new AssertionError("Input modified");
            if (!observed.equals(expected)) throw new AssertionError("Unexpected species-scoped identities: " + observed + " expected " + expected);
            System.out.println("Native GN beta/b parity, alpha and species isolation passed: " + observed);
        } finally {
            try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
                for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) Files.delete(path);
            }
        }
    }
}
