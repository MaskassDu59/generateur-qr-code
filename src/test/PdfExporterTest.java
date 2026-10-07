package test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import main.exception.QRCodeException;
import main.modele.PdfExporter;
import main.modele.QRCodeGenerator;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PdfExporterTest {

    private final PdfExporter exporter = new PdfExporter();

    @Test
    void exportCreatesValidPdfFile(@TempDir Path dir) throws Exception {
        BufferedImage image = new QRCodeGenerator().generate("https://www.example.com", 200);
        File file = dir.resolve("test.pdf").toFile();

        exporter.export(image, "https://www.example.com", file);

        assertTrue(file.exists());
        assertTrue(file.length() > 0);
        String header = new String(Files.readAllBytes(file.toPath()), 0, 5, StandardCharsets.ISO_8859_1);
        assertEquals("%PDF-", header);
    }

    @Test
    void exportWithoutImageThrows(@TempDir Path dir) {
        File file = dir.resolve("test.pdf").toFile();
        assertThrows(QRCodeException.class, () -> exporter.export(null, "texte", file));
    }

    @Test
    void exportWithoutFileThrows() throws Exception {
        BufferedImage image = new QRCodeGenerator().generate("test", 100);
        assertThrows(QRCodeException.class, () -> exporter.export(image, "test", null));
    }

    @Test
    void exportToInvalidPathThrows(@TempDir Path dir) throws Exception {
        BufferedImage image = new QRCodeGenerator().generate("test", 100);
        File impossible = dir.resolve("dossier-inexistant").resolve("test.pdf").toFile();
        assertThrows(QRCodeException.class, () -> exporter.export(image, "test", impossible));
    }
}
