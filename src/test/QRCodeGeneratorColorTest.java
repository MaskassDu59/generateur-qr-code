package test;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import main.exception.QRCodeException;
import main.modele.QRCodeGenerator;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class QRCodeGeneratorColorTest {

    private final QRCodeGenerator generator = new QRCodeGenerator();

    private boolean containsColor(BufferedImage image, Color color) {
        int expected = color.getRGB() & 0xFFFFFF;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                if ((image.getRGB(x, y) & 0xFFFFFF) == expected) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void coloredQRCodeUsesRequestedColor() throws QRCodeException {
        Color navy = new Color(0, 0, 128);
        BufferedImage image = generator.generate("https://www.example.com", 200, navy);
        assertTrue(containsColor(image, navy));
        assertTrue(containsColor(image, Color.WHITE));
    }

    @Test
    void coloredQRCodeCanStillBeDecoded() throws Exception {
        String text = "Test couleur";
        BufferedImage image = generator.generate(text, 300, new Color(0, 90, 0));
        BinaryBitmap bitmap = new BinaryBitmap(
                new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        assertEquals(text, new MultiFormatReader().decode(bitmap).getText());
    }

    @Test
    void tooLightColorThrows() {
        assertThrows(QRCodeException.class, () -> generator.generate("test", 200, Color.YELLOW));
        assertThrows(QRCodeException.class, () -> generator.generate("test", 200, Color.WHITE));
    }

    @Test
    void nullColorThrows() {
        assertThrows(QRCodeException.class, () -> generator.generate("test", 200, null));
    }

    @Test
    void twoArgumentVersionStillWorks() throws QRCodeException {
        assertNotNull(generator.generate("test", 200));
    }
}
