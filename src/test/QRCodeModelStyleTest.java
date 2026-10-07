package test;

import main.exception.QRCodeException;
import main.modele.PdfStyle;
import main.modele.QRCodeModel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class QRCodeModelStyleTest {

    @Test
    void newModelHasDefaultStyle() {
        assertNotNull(new QRCodeModel().getStyle());
    }

    @Test
    void nullStyleIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new QRCodeModel().setStyle(null));
    }

    @Test
    void generateUsesQrColorOfStyle() throws QRCodeException {
        QRCodeModel model = new QRCodeModel();
        PdfStyle style = new PdfStyle();
        style.setQrColor(new Color(128, 0, 0));
        model.setStyle(style);

        model.generate("Hello");

        int expected = new Color(128, 0, 0).getRGB() & 0xFFFFFF;
        boolean found = false;
        for (int x = 0; x < model.getImage().getWidth() && !found; x++) {
            for (int y = 0; y < model.getImage().getHeight(); y++) {
                if ((model.getImage().getRGB(x, y) & 0xFFFFFF) == expected) {
                    found = true;
                    break;
                }
            }
        }
        assertTrue(found);
    }

    @Test
    void generateWithTooLightColorThrowsAndKeepsPreviousQRCode() throws QRCodeException {
        QRCodeModel model = new QRCodeModel();
        model.generate("Premier");
        PdfStyle style = new PdfStyle();
        style.setQrColor(Color.YELLOW);
        model.setStyle(style);

        assertThrows(QRCodeException.class, () -> model.generate("Second"));
        assertEquals("Premier", model.getContent());
    }

    @Test
    void refreshWithoutGenerationThrows() {
        assertThrows(QRCodeException.class, () -> new QRCodeModel().refreshQRCode());
    }

    @Test
    void refreshAppliesNewColorToExistingContent() throws QRCodeException {
        QRCodeModel model = new QRCodeModel();
        model.generate("Hello");
        PdfStyle style = new PdfStyle();
        style.setQrColor(new Color(0, 0, 128));
        model.setStyle(style);

        model.refreshQRCode();

        assertEquals("Hello", model.getContent());
        assertNotNull(model.getImage());
    }

    @Test
    void exportAppliesStyle(@TempDir Path dir) throws QRCodeException {
        QRCodeModel model = new QRCodeModel();
        model.generate("Hello");
        PdfStyle style = new PdfStyle();
        style.setFontFamily(PdfStyle.FontFamily.COURIER);
        style.setAlignment(PdfStyle.Alignment.LEFT);
        model.setStyle(style);

        File file = dir.resolve("styled.pdf").toFile();
        model.exportToPdf(file);

        assertTrue(file.exists());
    }

    @Test
    void exportWithInvalidStyleThrows(@TempDir Path dir) throws QRCodeException {
        QRCodeModel model = new QRCodeModel();
        model.generate("Hello");
        PdfStyle style = new PdfStyle();
        style.setTextSize(1);
        model.setStyle(style);

        File file = dir.resolve("bad.pdf").toFile();
        assertThrows(QRCodeException.class, () -> model.exportToPdf(file));
        assertFalse(file.exists());
    }
}
