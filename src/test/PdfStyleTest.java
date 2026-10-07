package test;

import main.exception.QRCodeException;
import main.modele.PdfStyle;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class PdfStyleTest {

    @Test
    void defaultStyleIsValid() {
        assertDoesNotThrow(() -> new PdfStyle().validate());
    }

    @Test
    void contrastOfBlackOnWhiteIsMaximal() {
        assertEquals(21.0, PdfStyle.contrastRatio(Color.BLACK, Color.WHITE), 0.01);
    }

    @Test
    void contrastOfIdenticalColorsIsMinimal() {
        assertEquals(1.0, PdfStyle.contrastRatio(Color.RED, Color.RED), 0.001);
    }

    @Test
    void titleSizeOutOfRangeThrows() {
        PdfStyle style = new PdfStyle();
        style.setTitleSize(5);
        assertThrows(QRCodeException.class, style::validate);
        style.setTitleSize(100);
        assertThrows(QRCodeException.class, style::validate);
    }

    @Test
    void textSizeOutOfRangeThrows() {
        PdfStyle style = new PdfStyle();
        style.setTextSize(2);
        assertThrows(QRCodeException.class, style::validate);
        style.setTextSize(60);
        assertThrows(QRCodeException.class, style::validate);
    }

    @Test
    void whiteTextOnWhiteBackgroundThrows() {
        PdfStyle style = new PdfStyle();
        style.setTextColor(Color.WHITE);
        assertThrows(QRCodeException.class, style::validate);
    }

    @Test
    void whiteTitleOnWhiteBackgroundThrows() {
        PdfStyle style = new PdfStyle();
        style.setTitleColor(Color.WHITE);
        assertThrows(QRCodeException.class, style::validate);
    }

    @Test
    void lowContrastTitleIsIgnoredWhenTitleIsEmpty() {
        PdfStyle style = new PdfStyle();
        style.setTitleText("");
        style.setTitleColor(Color.WHITE);
        assertDoesNotThrow(style::validate);
    }

    @Test
    void lightTextOnDarkBackgroundIsValid() {
        PdfStyle style = new PdfStyle();
        style.setBackgroundColor(new Color(0, 0, 80));
        style.setTitleColor(Color.WHITE);
        style.setTextColor(Color.YELLOW);
        assertDoesNotThrow(style::validate);
    }

    @Test
    void customFontWithoutFileThrows() {
        PdfStyle style = new PdfStyle();
        style.setFontFamily(PdfStyle.FontFamily.CUSTOM);
        assertThrows(QRCodeException.class, style::validate);
    }

    @Test
    void customFontWithMissingFileThrows() {
        PdfStyle style = new PdfStyle();
        style.setFontFamily(PdfStyle.FontFamily.CUSTOM);
        style.setCustomFontFile(new File("police-inexistante.ttf"));
        assertThrows(QRCodeException.class, style::validate);
    }

    @Test
    void nullColorThrows() {
        PdfStyle style = new PdfStyle();
        style.setQrColor(null);
        assertThrows(QRCodeException.class, style::validate);
    }
}
