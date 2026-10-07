package main.modele;

import main.exception.QRCodeException;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class QRCodeModel {

    public static final int DEFAULT_SIZE = 300;

    private final QRCodeGenerator generator;
    private final PdfExporter exporter;

    private String content;
    private BufferedImage image;
    private PdfStyle style = new PdfStyle();
    private List<PdfImage> pdfImages = new ArrayList<>(); // images à insérer dans le PDF

    public QRCodeModel() {
        this(new QRCodeGenerator(), new PdfExporter());
    }

    public QRCodeModel(QRCodeGenerator generator, PdfExporter exporter) {
        this.generator = generator;
        this.exporter = exporter;
    }

    public void generate(String newContent) throws QRCodeException {
        BufferedImage newImage = generator.generate(newContent, DEFAULT_SIZE, style.getQrColor());
        this.content = newContent;
        this.image = newImage;
    }

    public void refreshQRCode() throws QRCodeException {
        if (content == null) {
            throw new QRCodeException("Générez d'abord un QR code.");
        }
        this.image = generator.generate(content, DEFAULT_SIZE, style.getQrColor());
    }

    public void exportToPdf(File file) throws QRCodeException {
        if (image == null) {
            throw new QRCodeException("Générez d'abord un QR code avant de l'exporter.");
        }
        refreshQRCode();
        exporter.export(image, content, file, style, pdfImages);
    }

    public void setStyle(PdfStyle style) {
        if (style == null) {
            throw new IllegalArgumentException("Le style ne peut pas être nul.");
        }
        this.style = style;
    }

    public PdfStyle getStyle() {
        return style;
    }

    public void setImages(List<PdfImage> images) {
        this.pdfImages = (images == null) ? new ArrayList<PdfImage>() : new ArrayList<>(images);
    }

    public List<PdfImage> getImages() {
        return new ArrayList<>(pdfImages);
    }

    public boolean hasQRCode() {
        return image != null;
    }

    public String getContent() {
        return content;
    }

    public BufferedImage getImage() {
        return image;
    }
}
