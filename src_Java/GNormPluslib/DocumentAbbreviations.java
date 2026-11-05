package GNormPluslib;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

/** Load document-scoped Ab3P definitions without running the bundled extractor. */
public final class DocumentAbbreviations {
    private DocumentAbbreviations() {}

    public static void load(String filename) throws IOException {
        GNormPlus.Pmid2Abb_hash.clear();
        GNormPlus.PmidAbb2LF_lc_hash.clear();
        GNormPlus.PmidLF2Abb_lc_hash.clear();
        GNormPlus.PmidAbb2LF_hash.clear();
        GNormPlus.PmidLF2Abb_hash.clear();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filename), StandardCharsets.UTF_8)) {
            String line;
            int number = 0;
            while ((line = reader.readLine()) != null) {
                number++;
                String[] fields = line.split("\t", -1);
                if (fields.length != 4 || fields[0].trim().isEmpty()
                        || fields[1].trim().isEmpty() || fields[2].trim().isEmpty()) {
                    throw new IOException("Expected document_id, short form, long form, confidence at "
                            + filename + ":" + number);
                }
                double confidence;
                try {
                    confidence = Double.parseDouble(fields[3]);
                } catch (NumberFormatException error) {
                    throw new IOException("Invalid abbreviation confidence at " + filename + ":" + number, error);
                }
                if (!Double.isFinite(confidence) || confidence < 0 || confidence > 1) {
                    throw new IOException("Abbreviation confidence outside [0, 1] at " + filename + ":" + number);
                }
                String document = fields[0] + "\t";
                String shortForm = fields[1];
                String longForm = fields[2];
                String previous = GNormPlus.PmidAbb2LF_hash.get(document + shortForm);
                if (previous != null && !previous.equals(longForm)) {
                    throw new IOException("Conflicting abbreviation at " + filename + ":" + number);
                }
                GNormPlus.Pmid2Abb_hash.put(document + shortForm, "Abb:SF");
                GNormPlus.Pmid2Abb_hash.put(document + longForm, "Abb:LF");
                GNormPlus.PmidAbb2LF_hash.put(document + shortForm, longForm);
                GNormPlus.PmidAbb2LF_lc_hash.put(document + shortForm.toLowerCase(Locale.ROOT),
                        longForm.toLowerCase(Locale.ROOT));
                GNormPlus.PmidLF2Abb_lc_hash.put(document + longForm.toLowerCase(Locale.ROOT),
                        shortForm.toLowerCase(Locale.ROOT));
                // Preserve GNorm2's existing gate for full-name-to-abbreviation ID propagation.
                if (confidence >= 0.9) {
                    GNormPlus.PmidLF2Abb_hash.put(document + longForm, shortForm);
                }
            }
        }
    }
}
