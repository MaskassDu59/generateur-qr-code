package test;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import main.exception.QRCodeException;
import main.modele.QRCodeGenerator;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class QRCodeGeneratorTest {

    private final QRCodeGenerator generator = new QRCodeGenerator();

    @Test
    void generateReturnsImageOfRequestedSize() throws QRCodeException {
        BufferedImage image = generator.generate("https://www.example.com", 300);
        assertNotNull(image);
        assertEquals(300, image.getWidth());
        assertEquals(300, image.getHeight());
    }

    @Test
    void generatedQRCodeCanBeDecodedBack() throws Exception {
        String text = "Bonjour à tous ! https://www.example.com";
        BufferedImage image = generator.generate(text, 300);

        BinaryBitmap bitmap = new BinaryBitmap(
                new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        String decoded = new MultiFormatReader().decode(bitmap).getText();
        assertEquals(text, decoded);
    }

    @Test
    void emptyContentThrows() {
        assertThrows(QRCodeException.class, () -> generator.generate("", 300));
        assertThrows(QRCodeException.class, () -> generator.generate("   ", 300));
    }

    @Test
    void nullContentThrows() {
        assertThrows(QRCodeException.class, () -> generator.generate(null, 300));
    }

    @Test
    void invalidSizeThrows() {
        assertThrows(QRCodeException.class, () -> generator.generate("test", 10));
        assertThrows(QRCodeException.class, () -> generator.generate("test", 5000));
    }

    @Test
    void tooLongContentThrows() {
        String tooLong = "a".repeat(10_000);
        assertThrows(QRCodeException.class, () -> generator.generate(tooLong, 300));
    }
}
