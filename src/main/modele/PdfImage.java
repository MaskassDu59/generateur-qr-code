package main.modele;

import main.exception.QRCodeException;

import java.io.File;

public class PdfImage {
    public static final int MAX_IMAGES = 10;

    public static final int MIN_WIDTH_PERCENT = 5;
    public static final int MAX_WIDTH_PERCENT = 100;

    public static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    private static final String[] EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif", ".bmp"};

    public enum Position {
        TOP("Avant le titre"),
        BEFORE_QR("Avant le QR code"),
        AFTER_QR("Après le QR code"),
        BOTTOM("Après le texte");

        private final String label;

        Position(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private File file;
    private Position position = Position.TOP;
    private PdfStyle.Alignment alignment = PdfStyle.Alignment.CENTER;
    private int widthPercent = 30;

    public PdfImage(File file) {
        this.file = file;
    }

    public PdfImage copy() {
        PdfImage c = new PdfImage(file);
        c.position = position;
        c.alignment = alignment;
        c.widthPercent = widthPercent;
        return c;
    }

    public void validate() throws QRCodeException {
        if (file == null) {
            throw new QRCodeException("Aucun fichier image sélectionné.");
        }
        if (!file.isFile()) {
            throw new QRCodeException("Image introuvable : " + file.getName());
        }
        String name = file.getName().toLowerCase();
        boolean supported = false;
        for (String extension : EXTENSIONS) {
            if (name.endsWith(extension)) {
                supported = true;
                break;
            }
        }
        if (!supported) {
            throw new QRCodeException("Format d'image non pris en charge : " + file.getName()
                    + " (formats acceptés : PNG, JPG, GIF, BMP).");
        }
        if (file.length() > MAX_FILE_BYTES) {
            throw new QRCodeException("L'image \"" + file.getName() + "\" est trop volumineuse (maximum 10 Mo).");
        }
        if (position == null || alignment == null) {
            throw new QRCodeException("L'emplacement ou l'alignement de l'image n'est pas défini.");
        }
        if (widthPercent < MIN_WIDTH_PERCENT || widthPercent > MAX_WIDTH_PERCENT) {
            throw new QRCodeException("La largeur de l'image doit être comprise entre "
                    + MIN_WIDTH_PERCENT + " % et " + MAX_WIDTH_PERCENT + " %.");
        }
    }

    public File getFile() { return file; }
    public void setFile(File file) { this.file = file; }

    public Position getPosition() { return position; }
    public void setPosition(Position position) { this.position = position; }

    public PdfStyle.Alignment getAlignment() { return alignment; }
    public void setAlignment(PdfStyle.Alignment alignment) { this.alignment = alignment; }

    public int getWidthPercent() { return widthPercent; }
    public void setWidthPercent(int widthPercent) { this.widthPercent = widthPercent; }
}
