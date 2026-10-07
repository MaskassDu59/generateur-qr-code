package main.controleur;

import main.exception.QRCodeException;
import main.modele.QRCodeModel;
import main.vue.InterfaceUtil;

import java.io.File;

public class QRCodeController {

    private final QRCodeModel modele;
    private final InterfaceUtil vue;

    public QRCodeController(QRCodeModel modele, InterfaceUtil vue) {
        this.modele = modele;
        this.vue = vue;
        this.vue.addGenerateListener(e -> onGenerate());
        this.vue.addExportListener(e -> onExport());
        this.vue.addQrColorListener(e -> onQrColorChanged());
    }

    private void onGenerate() {
        try {
            modele.setStyle(vue.getStyle());
            modele.generate(vue.getContent());
            vue.displayQRCode(modele.getImage());
            vue.setExportEnabled(true);
        } catch (QRCodeException e) {
            vue.showError(e.getMessage());
        }
    }

    private void onExport() {
        File file = vue.chooseSaveFile();
        if (file == null) {
            return;
        }
        try {
            modele.setStyle(vue.getStyle());
            modele.setImages(vue.getImages());
            modele.exportToPdf(file);
            vue.showInfo("PDF enregistré :\n" + file.getAbsolutePath());
        } catch (QRCodeException e) {
            vue.showError(e.getMessage());
        }
    }

    private void onQrColorChanged() {
        if (!modele.hasQRCode()) {
            return;
        }
        try {
            modele.setStyle(vue.getStyle());
            modele.refreshQRCode();
            vue.displayQRCode(modele.getImage());
        } catch (QRCodeException e) {
            vue.showError(e.getMessage());
        }
    }

    public void start() {
        vue.setVisible(true);
    }
}
