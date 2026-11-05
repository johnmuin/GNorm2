import GNormPluslib.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Exercise the shipped, unchanged GN algorithm with a controlled two-gene lexicon. */
public class GeneNormalizationAbbreviationsTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("gnorm2-normalization-");
        try {
            String[] ids = {"paperA_2", "paperA_7", "paperB_3", "low_1", "paperA", "conflict_1"};
            StringBuilder xml = new StringBuilder("<collection><source>test</source><date>20261105</date><key>test</key>");
            for (String id : ids) {
                String longForm = id.equals("paperB_3") ? "aldose reductase" : "androgen receptor";
                String text = longForm + " QARX";
                xml.append("<document><id>").append(id).append("</id><passage><infon key=\"type\">abstract</infon><offset>0</offset><text>")
                        .append(text).append("</text>");
                for (int i = 0; i < 2; i++) {
                    String mention = i == 0 ? longForm : "QARX";
                    xml.append("<annotation id=\"").append(i).append("\"><infon key=\"type\">Gene</infon>")
                            .append("<infon key=\"Identifier\">Tax:9606</infon><location offset=\"")
                            .append(i == 0 ? 0 : longForm.length() + 1).append("\" length=\"")
                            .append(mention.length()).append("\"/><text>").append(mention).append("</text></annotation>");
                }
                xml.append("</passage></document>");
            }
            xml.append("</collection>");
            Path input = root.resolve("input.xml"), output = root.resolve("output.xml"), table = root.resolve("definitions.tsv");
            Files.write(input, xml.toString().getBytes(StandardCharsets.UTF_8));
            Files.write(table, ("paperA_2\tQARX\tandrogen receptor\t0.9\n"
                    + "paperA_7\tQARX\tandrogen receptor\t0.999\n"
                    + "paperB_3\tQARX\taldose reductase\t0.95\n"
                    + "low_1\tQARX\tandrogen receptor\t0.899999\n").getBytes(StandardCharsets.UTF_8));
            GNormPlus.PT_Gene.insertMention("androgen receptor", "9606:367");
            GNormPlus.PT_Gene.insertMention("aldose reductase", "9606:231");
            DocumentAbbreviations.load(table.toString());
            GNormPlus.BioCDocobj.BioCReaderWithAnnotation(input.toString());
            new GN().GeneNormalization(input.toString(), output.toString(), false);
            Map<String, String> observed = new TreeMap<>();
            for (int i = 0; i < GNormPlus.BioCDocobj.PMIDs.size(); i++) {
                for (ArrayList<String> passage : GNormPlus.BioCDocobj.Annotations.get(i)) {
                    for (String annotation : passage) {
                        String[] fields = annotation.split("\t");
                        if (fields[2].equals("QARX")) observed.put(GNormPlus.BioCDocobj.PMIDs.get(i), fields[4]);
                    }
                }
            }
            if (observed.size() != 3 || !observed.get("paperA_2").endsWith("|367")
                    || !observed.get("paperA_7").endsWith("|367") || !observed.get("paperB_3").endsWith("|231")) {
                throw new AssertionError("Unexpected native normalization: " + observed);
            }
            System.out.println("Native GN scoped propagation and 0.9 gate passed: " + observed);
        } finally {
            try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
                for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) Files.delete(path);
            }
        }
    }
}
