package main.modele;

import main.exception.QRCodeException;

import java.awt.Color;
import java.io.File;

public class PdfStyle {

    public static final double MIN_CONTRAST = 3.0;

    public static final int MIN_TITLE_SIZE = 8;
    public static final int MAX_TITLE_SIZE = 72;
    public static final int MIN_TEXT_SIZE = 6;
    public static final int MAX_TEXT_SIZE = 48;

    public enum FontFamily {
        HELVETICA("Helvetica"),
        TIMES("Times"),
        COURIER("Courier"),
        CUSTOM("Police personnalisée");

        private final String label;

        FontFamily(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum Alignment {
        LEFT("Gauche"),
        CENTER("Centré"),
        RIGHT("Droite");

        private final String label;

        Alignment(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private String titleText = "QR Code généré";
    private FontFamily fontFamily = FontFamily.HELVETICA;
    private File customFontFile;
    private int titleSize = 20;
    private int textSize = 12;
    private boolean titleBold = true;
    private boolean titleItalic = false;
    private boolean textBold = false;
    private boolean textItalic = false;
    private Alignment alignment = Alignment.CENTER;
    private Color titleColor = Color.BLACK;
    private Color textColor = Color.BLACK;
    private Color backgroundColor = Color.WHITE;
    private Color qrColor = Color.BLACK;

    public void validate() throws QRCodeException {
        if (titleText == null) {
            throw new QRCodeException("Le titre du PDF est invalide.");
        }
        if (titleSize < MIN_TITLE_SIZE || titleSize > MAX_TITLE_SIZE) {
            throw new QRCodeException("La taille du titre doit être comprise entre "
                    + MIN_TITLE_SIZE + " et " + MAX_TITLE_SIZE + ".");
        }
        if (textSize < MIN_TEXT_SIZE || textSize > MAX_TEXT_SIZE) {
            throw new QRCodeException("La taille du texte doit être comprise entre "
                    + MIN_TEXT_SIZE + " et " + MAX_TEXT_SIZE + ".");
        }
        if (fontFamily == null || alignment == null) {
            throw new QRCodeException("La police ou l'alignement n'est pas défini.");
        }
        if (titleColor == null || textColor == null || backgroundColor == null || qrColor == null) {
            throw new QRCodeException("Toutes les couleurs doivent être définies.");
        }

        if (fontFamily == FontFamily.CUSTOM) {
            if (customFontFile == null) {
                throw new QRCodeException("Choisissez un fichier de police (.ttf ou .otf).");
            }
            String name = customFontFile.getName().toLowerCase();
            if (!customFontFile.isFile()) {
                throw new QRCodeException("Le fichier de police est introuvable : " + customFontFile.getName());
            }
            if (!name.endsWith(".ttf") && !name.endsWith(".otf")) {
                throw new QRCodeException("La police doit être un fichier .ttf ou .otf.");
            }
        }

        if (!titleText.trim().isEmpty() && contrastRatio(titleColor, backgroundColor) < MIN_CONTRAST) {
            throw new QRCodeException("La couleur du titre n'est pas assez contrastée avec le fond.");
        }
        if (contrastRatio(textColor, backgroundColor) < MIN_CONTRAST) {
            throw new QRCodeException("La couleur du texte n'est pas assez contrastée avec le fond.");
        }
    }

    public static double luminance(Color c) {
        return 0.2126 * linear(c.getRed() / 255.0)
                + 0.7152 * linear(c.getGreen() / 255.0)
                + 0.0722 * linear(c.getBlue() / 255.0);
    }

    private static double linear(double v) {
        return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    public static double contrastRatio(Color a, Color b) {
        double la = luminance(a);
        double lb = luminance(b);
        double lighter = Math.max(la, lb);
        double darker = Math.min(la, lb);
        return (lighter + 0.05) / (darker + 0.05);
    }

    public String getTitleText() { return titleText; }
    public void setTitleText(String titleText) { this.titleText = titleText; }

    public FontFamily getFontFamily() { return fontFamily; }
    public void setFontFamily(FontFamily fontFamily) { this.fontFamily = fontFamily; }

    public File getCustomFontFile() { return customFontFile; }
    public void setCustomFontFile(File customFontFile) { this.customFontFile = customFontFile; }

    public int getTitleSize() { return titleSize; }
    public void setTitleSize(int titleSize) { this.titleSize = titleSize; }

    public int getTextSize() { return textSize; }
    public void setTextSize(int textSize) { this.textSize = textSize; }

    public boolean isTitleBold() { return titleBold; }
    public void setTitleBold(boolean titleBold) { this.titleBold = titleBold; }

    public boolean isTitleItalic() { return titleItalic; }
    public void setTitleItalic(boolean titleItalic) { this.titleItalic = titleItalic; }

    public boolean isTextBold() { return textBold; }
    public void setTextBold(boolean textBold) { this.textBold = textBold; }

    public boolean isTextItalic() { return textItalic; }
    public void setTextItalic(boolean textItalic) { this.textItalic = textItalic; }

    public Alignment getAlignment() { return alignment; }
    public void setAlignment(Alignment alignment) { this.alignment = alignment; }

    public Color getTitleColor() { return titleColor; }
    public void setTitleColor(Color titleColor) { this.titleColor = titleColor; }

    public Color getTextColor() { return textColor; }
    public void setTextColor(Color textColor) { this.textColor = textColor; }

    public Color getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(Color backgroundColor) { this.backgroundColor = backgroundColor; }

    public Color getQrColor() { return qrColor; }
    public void setQrColor(Color qrColor) { this.qrColor = qrColor; }
}
