package main.modele;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import main.exception.QRCodeException;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

public class QRCodeGenerator {

    public static final int MIN_SIZE = 50;

    public static final int MAX_SIZE = 2000;

    public BufferedImage generate(String content, int size) throws QRCodeException {
        return generate(content, size, Color.BLACK);
    }

    public BufferedImage generate(String content, int size, Color color) throws QRCodeException {

        if (content == null || content.trim().isEmpty()) {
            throw new QRCodeException("Le contenu du QR code ne peut pas être vide.");
        }
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw new QRCodeException("La taille doit être comprise entre "
                    + MIN_SIZE + " et " + MAX_SIZE + " pixels.");
        }
        if (color == null) {
            throw new QRCodeException("La couleur du QR code n'est pas définie.");
        }
        if (PdfStyle.contrastRatio(color, Color.WHITE) < PdfStyle.MIN_CONTRAST) {
            throw new QRCodeException(
                    "La couleur du QR code est trop claire : il ne serait pas lisible. Choisissez une couleur plus foncée.");
        }

        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);

        try {
            BitMatrix matrix = new QRCodeWriter()
                    .encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            MatrixToImageConfig config = new MatrixToImageConfig(color.getRGB(), Color.WHITE.getRGB());
            return MatrixToImageWriter.toBufferedImage(matrix, config);
        } catch (WriterException e) {
            throw new QRCodeException(
                    "Impossible de générer le QR code : le contenu est probablement trop long.", e);
        }
    }
}
