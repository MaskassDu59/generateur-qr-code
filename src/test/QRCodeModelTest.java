package test;

import main.exception.QRCodeException;
import main.modele.QRCodeModel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class QRCodeModelTest {

    @Test
    void newModelHasNoQRCode() {
        QRCodeModel model = new QRCodeModel();
        assertFalse(model.hasQRCode());
        assertNull(model.getImage());
    }

    @Test
    void generateStoresContentAndImage() throws QRCodeException {
        QRCodeModel model = new QRCodeModel();
        model.generate("Hello");
        assertTrue(model.hasQRCode());
        assertEquals("Hello", model.getContent());
        assertNotNull(model.getImage());
    }

    @Test
    void failedGenerationKeepsPreviousQRCode() throws QRCodeException {
        QRCodeModel model = new QRCodeModel();
        model.generate("Premier");
        assertThrows(QRCodeException.class, () -> model.generate(""));
        assertEquals("Premier", model.getContent());
    }

    @Test
    void exportWithoutGenerationThrows(@TempDir Path dir) {
        QRCodeModel model = new QRCodeModel();
        File file = dir.resolve("out.pdf").toFile();
        assertThrows(QRCodeException.class, () -> model.exportToPdf(file));
    }

    @Test
    void exportAfterGenerationWorks(@TempDir Path dir) throws QRCodeException {
        QRCodeModel model = new QRCodeModel();
        model.generate("Hello");
        File file = dir.resolve("out.pdf").toFile();
        model.exportToPdf(file);
        assertTrue(file.exists());
    }
}
