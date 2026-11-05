import GNormPluslib.DocumentAbbreviations;
import GNormPluslib.GNormPlus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class DocumentAbbreviationsTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        Path table = Files.createTempFile("gnorm2-abbreviations-", ".tsv");
        try {
            Files.write(table, ("paperA_2\tTNF\ttumor necrosis factor\t0.9\n"
                    + "paperB_7\tTNF\ttransforming nuclear factor\t0.899999\n"
                    + "paperC_1\tβAR\tβ adrenergic receptor\t1.0\n").getBytes(StandardCharsets.UTF_8));
            DocumentAbbreviations.load(table.toString());
            check("tumor necrosis factor".equals(GNormPlus.PmidAbb2LF_hash.get("paperA_2\tTNF")), "paragraph A");
            check("transforming nuclear factor".equals(GNormPlus.PmidAbb2LF_hash.get("paperB_7\tTNF")), "paragraph B isolation");
            check(!GNormPlus.PmidAbb2LF_hash.containsKey("paperA\tTNF"), "summary excluded");
            check(!GNormPlus.PmidAbb2LF_hash.containsKey("paperA_1\tTNF"), "absent paragraph excluded");
            check("TNF".equals(GNormPlus.PmidLF2Abb_hash.get("paperA_2\ttumor necrosis factor")), "0.9 accepted");
            check(!GNormPlus.PmidLF2Abb_hash.containsKey("paperB_7\ttransforming nuclear factor"), "below 0.9 rejected for propagation");
            check("tumor necrosis factor".equals(GNormPlus.PmidAbb2LF_lc_hash.get("paperA_2\ttnf")), "lowercase forward");
            check("tnf".equals(GNormPlus.PmidLF2Abb_lc_hash.get("paperA_2\ttumor necrosis factor")), "lowercase reverse");
            check("Abb:SF".equals(GNormPlus.Pmid2Abb_hash.get("paperC_1\tβAR")), "unicode feature mapping");
            check("βAR".equals(GNormPlus.PmidLF2Abb_hash.get("paperC_1\tβ adrenergic receptor")), "unicode reverse");
            for (String invalid : new String[] {
                    "a_1\tSF\tlong\n", "a_1\tSF\tlong\tNaN\n",
                    "a_1\tSF\tlong\tInfinity\n", "a_1\tSF\tlong\t-0.1\n",
                    "a_1\tSF\tlong\t1.1\n", "a_1\t\tlong\t0.9\n",
                    "a_1\tSF\tlong\t0.9\na_1\tSF\tother\t0.9\n"}) {
                Files.write(table, invalid.getBytes(StandardCharsets.UTF_8));
                boolean failed = false;
                try { DocumentAbbreviations.load(table.toString()); }
                catch (IOException expected) { failed = true; }
                check(failed, "invalid input must fail: " + invalid);
            }
            Files.write(table, new byte[0]);
            DocumentAbbreviations.load(table.toString());
            check(GNormPlus.PmidAbb2LF_hash.isEmpty() && GNormPlus.PmidLF2Abb_hash.isEmpty()
                    && GNormPlus.Pmid2Abb_hash.isEmpty() && GNormPlus.PmidAbb2LF_lc_hash.isEmpty()
                    && GNormPlus.PmidLF2Abb_lc_hash.isEmpty(), "empty input clears all maps");
            System.out.println("DocumentAbbreviations contracts passed");
        } finally { Files.deleteIfExists(table); }
    }
}
