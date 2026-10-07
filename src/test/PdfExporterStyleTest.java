package test;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import main.exception.QRCodeException;
import main.modele.PdfExporter;
import main.modele.PdfStyle;
import main.modele.QRCodeGenerator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PdfExporterStyleTest {

    private final PdfExporter exporter = new PdfExporter();

    private BufferedImage qr() throws QRCodeException {
        return new QRCodeGenerator().generate("https://www.example.com", 200);
    }

    private String firstPageText(File pdf) throws IOException {
        PdfReader reader = new PdfReader(pdf.getAbsolutePath());
        try {
            return PdfTextExtractor.getTextFromPage(reader, 1);
        } finally {
            reader.close();
        }
    }

    private File findSystemTtf() {
        String[] candidates = {
                "C:\\Windows\\Fonts\\arial.ttf",
                "C:\\Windows\\Fonts\\verdana.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/System/Library/Fonts/Supplemental/Arial.ttf",
                "/Library/Fonts/Arial.ttf"
        };
        for (String path : candidates) {
            File f = new File(path);
            if (f.isFile()) {
                return f;
            }
        }
        return null;
    }

    @Test
    void everyStandardFontFamilyProducesAPdf(@TempDir Path dir) throws Exception {
        for (PdfStyle.FontFamily family : new PdfStyle.FontFamily[]{
                PdfStyle.FontFamily.HELVETICA, PdfStyle.FontFamily.TIMES, PdfStyle.FontFamily.COURIER}) {
            PdfStyle style = new PdfStyle();
            style.setFontFamily(family);
            File file = dir.resolve(family.name() + ".pdf").toFile();

            exporter.export(qr(), "Bonjour", file, style);

            assertTrue(file.length() > 0, "PDF vide pour " + family);
            assertTrue(firstPageText(file).contains("Bonjour"));
        }
    }

    @Test
    void titleAndAccentedTextAreWrittenInThePdf(@TempDir Path dir) throws Exception {
        PdfStyle style = new PdfStyle();
        style.setTitleText("Mon événement");
        File file = dir.resolve("accents.pdf").toFile();

        exporter.export(qr(), "Café à Cambrai", file, style);

        String text = firstPageText(file);
        assertTrue(text.contains("Mon événement"), text);
        assertTrue(text.contains("Café à Cambrai"), text);
    }

    @Test
    void emptyTitleIsNotWritten(@TempDir Path dir) throws Exception {
        PdfStyle style = new PdfStyle();
        style.setTitleText("");
        File file = dir.resolve("notitle.pdf").toFile();

        exporter.export(qr(), "Bonjour", file, style);

        assertFalse(firstPageText(file).contains("QR Code généré"));
    }

    @Test
    void allStyleOptionsCombinedProduceAValidPdf(@TempDir Path dir) throws Exception {
        PdfStyle style = new PdfStyle();
        style.setFontFamily(PdfStyle.FontFamily.TIMES);
        style.setTitleSize(40);
        style.setTextSize(18);
        style.setTitleBold(true);
        style.setTitleItalic(true);
        style.setTextBold(true);
        style.setTextItalic(true);
        style.setAlignment(PdfStyle.Alignment.RIGHT);
        style.setBackgroundColor(new Color(0, 0, 80));
        style.setTitleColor(Color.WHITE);
        style.setTextColor(Color.YELLOW);
        File file = dir.resolve("full.pdf").toFile();

        exporter.export(qr(), "Test complet", file, style);

        String header = new String(Files.readAllBytes(file.toPath()), 0, 5, "ISO-8859-1");
        assertEquals("%PDF-", header);
    }

    @Test
    void leftAndCenterAlignmentsBothWork(@TempDir Path dir) throws Exception {
        for (PdfStyle.Alignment alignment : PdfStyle.Alignment.values()) {
            PdfStyle style = new PdfStyle();
            style.setAlignment(alignment);
            File file = dir.resolve(alignment.name() + ".pdf").toFile();
            exporter.export(qr(), "Alignement", file, style);
            assertTrue(file.length() > 0);
        }
    }

    @Test
    void backgroundColorIsPaintedOnThePage(@TempDir Path dir) throws Exception {
        PdfStyle style = new PdfStyle();
        style.setBackgroundColor(new Color(255, 255, 204));
        File file = dir.resolve("bg.pdf").toFile();

        exporter.export(qr(), "Fond", file, style);

        PdfReader reader = new PdfReader(file.getAbsolutePath());
        try {
            String content = new String(reader.getPageContent(1), "ISO-8859-1");
            // « rg » = instruction PDF « définir la couleur de remplissage RGB »
            assertTrue(content.contains(" rg"), content);
        } finally {
            reader.close();
        }
    }

    @Test
    void invalidStyleThrowsAndCreatesNoFile(@TempDir Path dir) throws Exception {
        PdfStyle style = new PdfStyle();
        style.setTextColor(Color.WHITE); // blanc sur blanc
        File file = dir.resolve("refuse.pdf").toFile();

        assertThrows(QRCodeException.class, () -> exporter.export(qr(), "x", file, style));
        assertFalse(file.exists());
    }

    @Test
    void nullStyleThrows(@TempDir Path dir) {
        File file = dir.resolve("null.pdf").toFile();
        assertThrows(QRCodeException.class, () -> exporter.export(qr(), "x", file, null));
    }

    @Test
    void customFontWithInvalidFileThrowsAndCreatesNoFile(@TempDir Path dir) throws Exception {
        File fakeFont = dir.resolve("fausse.ttf").toFile();
        Files.write(fakeFont.toPath(), "ceci n'est pas une police".getBytes("UTF-8"));
        PdfStyle style = new PdfStyle();
        style.setFontFamily(PdfStyle.FontFamily.CUSTOM);
        style.setCustomFontFile(fakeFont);
        File file = dir.resolve("out.pdf").toFile();

        assertThrows(QRCodeException.class, () -> exporter.export(qr(), "x", file, style));
        assertFalse(file.exists());
    }

    @Test
    void customFontIsEmbeddedInThePdf(@TempDir Path dir) throws Exception {
        File ttf = findSystemTtf();
        org.junit.jupiter.api.Assumptions.assumeTrue(ttf != null, "Aucune police .ttf système trouvée");

        PdfStyle style = new PdfStyle();
        style.setFontFamily(PdfStyle.FontFamily.CUSTOM);
        style.setCustomFontFile(ttf);
        File file = dir.resolve("custom.pdf").toFile();

        exporter.export(qr(), "Police perso", file, style);

        assertTrue(file.length() > 5000, "La police incorporée devrait alourdir le fichier");
        assertTrue(firstPageText(file).contains("Police perso"));
    }
}
