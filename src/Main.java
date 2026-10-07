import main.vue.InterfaceUtil;
import main.controleur.QRCodeController;
import main.modele.QRCodeModel;

import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            QRCodeModel model = new QRCodeModel();
            InterfaceUtil view = new InterfaceUtil();
            new QRCodeController(model, view).start();
        });
    }
}