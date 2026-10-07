package main.modele;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import main.exception.QRCodeException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class PdfExporter {
    private static final float MARGIN = 50f;

    private static final float IMAGE_SPACING = 15f;

    public void export(BufferedImage qrImage, String content, File file) throws QRCodeException {
        export(qrImage, content, file, new PdfStyle());
    }

    public void export(BufferedImage qrImage, String content, File file, PdfStyle style)
            throws QRCodeException {
        export(qrImage, content, file, style, Collections.<PdfImage>emptyList());
    }

    public void export(BufferedImage qrImage, String content, File file, PdfStyle style,
                       List<PdfImage> images) throws QRCodeException {

        if (qrImage == null) {
            throw new QRCodeException("Aucun QR code à exporter.");
        }
        if (file == null) {
            throw new QRCodeException("Aucun fichier de destination choisi.");
        }
        if (style == null) {
            throw new QRCodeException("Aucun style de PDF fourni.");
        }
        style.validate();

        Font titleFont = buildFont(style, style.getTitleSize(),
                style.isTitleBold(), style.isTitleItalic(), style.getTitleColor());
        Font textFont = buildFont(style, style.getTextSize(),
                style.isTextBold(), style.isTextItalic(), style.getTextColor());
        int alignment = toElementAlignment(style.getAlignment());
        Map<PdfImage.Position, List<Paragraph>> extraImages = loadImages(images);

        Document document = new Document(PageSize.A4, MARGIN, MARGIN, MARGIN, MARGIN);

        try (FileOutputStream out = new FileOutputStream(file)) {

            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new BackgroundEvent(toBaseColor(style.getBackgroundColor())));
            document.open();
            document.addTitle("QR Code");

            addImages(document, extraImages.get(PdfImage.Position.TOP));

            if (!style.getTitleText().trim().isEmpty()) {
                Paragraph title = new Paragraph(style.getTitleText(), titleFont);
                title.setAlignment(alignment);
                title.setSpacingAfter(30);
                document.add(title);
            }

            addImages(document, extraImages.get(PdfImage.Position.BEFORE_QR));

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "png", buffer);
            Image qr = Image.getInstance(buffer.toByteArray());
            qr.scaleToFit(300, 300);
            qr.setAlignment(alignment);
            document.add(qr);

            addImages(document, extraImages.get(PdfImage.Position.AFTER_QR));

            Paragraph text = new Paragraph("Contenu : " + content, textFont);
            text.setAlignment(alignment);
            text.setSpacingBefore(30);
            document.add(text);

            addImages(document, extraImages.get(PdfImage.Position.BOTTOM));

            document.close();

        } catch (DocumentException e) {
            throw new QRCodeException("Erreur lors de la création du document PDF.", e);
        } catch (IOException e) {
            throw new QRCodeException("Impossible d'écrire le fichier PDF : " + e.getMessage(), e);
        }
    }

    private Map<PdfImage.Position, List<Paragraph>> loadImages(List<PdfImage> images) throws QRCodeException {
        Map<PdfImage.Position, List<Paragraph>> result = new EnumMap<>(PdfImage.Position.class);
        for (PdfImage.Position position : PdfImage.Position.values()) {
            result.put(position, new ArrayList<Paragraph>());
        }
        if (images == null || images.isEmpty()) {
            return result;
        }
        if (images.size() > PdfImage.MAX_IMAGES) {
            throw new QRCodeException("Un PDF ne peut contenir que " + PdfImage.MAX_IMAGES + " images au maximum.");
        }

        float usableWidth = PageSize.A4.getWidth() - 2 * MARGIN;
        float usableHeight = PageSize.A4.getHeight() - 2 * MARGIN - 4 * IMAGE_SPACING;

        for (PdfImage pdfImage : images) {
            pdfImage.validate();
            try {
                Image image = Image.getInstance(Files.readAllBytes(pdfImage.getFile().toPath()));

                float maxWidth = usableWidth * pdfImage.getWidthPercent() / 100f;
                image.scaleToFit(maxWidth, usableHeight);

                Paragraph line = new Paragraph();
                line.add(new Chunk(image, 0, 0, true));
                line.setAlignment(toElementAlignment(pdfImage.getAlignment()));
                line.setSpacingBefore(IMAGE_SPACING);
                line.setSpacingAfter(IMAGE_SPACING);
                result.get(pdfImage.getPosition()).add(line);
            } catch (DocumentException | IOException e) {
                throw new QRCodeException("Impossible de lire l'image \"" + pdfImage.getFile().getName()
                        + "\" : fichier image invalide ou corrompu.", e);
            }
        }
        return result;
    }

    private void addImages(Document document, List<Paragraph> images) throws DocumentException {
        if (images == null) {
            return;
        }
        for (Paragraph image : images) {
            document.add(image);
        }
    }

    private Font buildFont(PdfStyle style, int size, boolean bold, boolean italic, Color color)
            throws QRCodeException {

        int flags = Font.NORMAL;
        if (bold && italic) {
            flags = Font.BOLDITALIC;
        } else if (bold) {
            flags = Font.BOLD;
        } else if (italic) {
            flags = Font.ITALIC;
        }
        BaseColor baseColor = toBaseColor(color);

        switch (style.getFontFamily()) {
            case CUSTOM:
                try {
                    BaseFont base = BaseFont.createFont(
                            style.getCustomFontFile().getAbsolutePath(),
                            BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                    return new Font(base, size, flags, baseColor);
                } catch (DocumentException | IOException e) {
                    throw new QRCodeException("Impossible de charger la police \""
                            + style.getCustomFontFile().getName() + "\" : fichier de police invalide.", e);
                }
            case TIMES:
                return FontFactory.getFont(FontFactory.TIMES_ROMAN, size, flags, baseColor);
            case COURIER:
                return FontFactory.getFont(FontFactory.COURIER, size, flags, baseColor);
            case HELVETICA:
            default:
                return FontFactory.getFont(FontFactory.HELVETICA, size, flags, baseColor);
        }
    }

    private static BaseColor toBaseColor(Color c) {
        return new BaseColor(c.getRed(), c.getGreen(), c.getBlue());
    }

    private static int toElementAlignment(PdfStyle.Alignment alignment) {
        switch (alignment) {
            case LEFT:
                return Element.ALIGN_LEFT;
            case RIGHT:
                return Element.ALIGN_RIGHT;
            case CENTER:
            default:
                return Element.ALIGN_CENTER;
        }
    }

    private static class BackgroundEvent extends PdfPageEventHelper {

        private final BaseColor color;

        BackgroundEvent(BaseColor color) {
            this.color = color;
        }

        @Override
        public void onStartPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContentUnder();
            canvas.saveState();
            canvas.setColorFill(color);
            canvas.rectangle(0, 0, document.getPageSize().getWidth(), document.getPageSize().getHeight());
            canvas.fill();
            canvas.restoreState();
        }
    }
}
