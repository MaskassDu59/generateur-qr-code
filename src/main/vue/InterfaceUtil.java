package main.vue;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import main.modele.PdfImage;
import main.modele.PdfStyle;

public class InterfaceUtil extends JFrame {

    private static final long serialVersionUID = 1L;

    private final JPanel contentPane;

    private final JTextArea inputArea = new JTextArea(4, 30);
    private final JButton generateButton = new JButton("Generate QR code");
    private final JButton exportButton = new JButton("Export PDF");
    private final JLabel qrLabel = new JLabel("Le QR code apparaîtra ici", SwingConstants.CENTER);

    private final JTextField titleField = new JTextField(14);
    private final JComboBox<PdfStyle.FontFamily> fontCombo = new JComboBox<>(PdfStyle.FontFamily.values());
    private final JButton fontFileButton = new JButton("Choisir un .ttf / .otf…");
    private final JLabel fontFileLabel = new JLabel("Aucun fichier");
    private final JSpinner titleSizeSpinner = new JSpinner(
            new SpinnerNumberModel(20, PdfStyle.MIN_TITLE_SIZE, PdfStyle.MAX_TITLE_SIZE, 1));
    private final JSpinner textSizeSpinner = new JSpinner(
            new SpinnerNumberModel(12, PdfStyle.MIN_TEXT_SIZE, PdfStyle.MAX_TEXT_SIZE, 1));
    private final JCheckBox titleBoldBox = new JCheckBox("Gras");
    private final JCheckBox titleItalicBox = new JCheckBox("Italique");
    private final JCheckBox textBoldBox = new JCheckBox("Gras");
    private final JCheckBox textItalicBox = new JCheckBox("Italique");
    private final JComboBox<PdfStyle.Alignment> alignCombo = new JComboBox<>(PdfStyle.Alignment.values());
    private final ColorSelector titleColorSelector = new ColorSelector("Couleur du titre", Color.BLACK);
    private final ColorSelector textColorSelector = new ColorSelector("Couleur du texte", Color.BLACK);
    private final ColorSelector backgroundColorSelector = new ColorSelector("Couleur du fond", Color.WHITE);
    private final ColorSelector qrColorSelector = new ColorSelector("Couleur du QR code", Color.BLACK);
    private final JButton resetStyleButton = new JButton("Réinitialiser le style");

    private final DefaultListModel<PdfImage> imageListModel = new DefaultListModel<>();
    private final JList<PdfImage> imageList = new JList<>(imageListModel);
    private final JButton addImageButton = new JButton("Ajouter des images…");
    private final JButton removeImageButton = new JButton("Supprimer");
    private final JComboBox<PdfImage.Position> imagePositionCombo = new JComboBox<>(PdfImage.Position.values());
    private final JComboBox<PdfStyle.Alignment> imageAlignCombo = new JComboBox<>(PdfStyle.Alignment.values());
    private final JSpinner imageWidthSpinner = new JSpinner(new SpinnerNumberModel(
            30, PdfImage.MIN_WIDTH_PERCENT, PdfImage.MAX_WIDTH_PERCENT, 5));

    private boolean updatingImageControls = false;

    private File customFontFile;

    public InterfaceUtil() {
        super("Générateur de QR Code");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(new EmptyBorder(15, 15, 15, 15));
        setContentPane(contentPane);

        hautFenetre();
        milieuFenetre();
        droiteFenetre();
        basFenetre();

        exportButton.setEnabled(false);
        setStyle(new PdfStyle());

        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    private void hautFenetre() {
        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.add(new JLabel("Texte ou lien à encoder :"), BorderLayout.NORTH);

        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        top.add(new JScrollPane(inputArea), BorderLayout.CENTER);

        contentPane.add(top, BorderLayout.NORTH);
    }

    private void milieuFenetre() {
        qrLabel.setPreferredSize(new Dimension(320, 320));
        qrLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        contentPane.add(qrLabel, BorderLayout.CENTER);
    }

    private void droiteFenetre() {
        JPanel stylePanel = new JPanel(new GridBagLayout());
        stylePanel.setBorder(BorderFactory.createTitledBorder("Style du PDF"));

        JPanel fontFilePanel = new JPanel(new BorderLayout(5, 0));
        fontFilePanel.add(fontFileButton, BorderLayout.NORTH);
        fontFilePanel.add(fontFileLabel, BorderLayout.CENTER);

        JPanel titleStylePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        titleStylePanel.add(titleBoldBox);
        titleStylePanel.add(titleItalicBox);

        JPanel textStylePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        textStylePanel.add(textBoldBox);
        textStylePanel.add(textItalicBox);

        int row = 0;
        addRow(stylePanel, row++, "Titre :", titleField);
        addRow(stylePanel, row++, "Police :", fontCombo);
        addRow(stylePanel, row++, "", fontFilePanel);
        addRow(stylePanel, row++, "Taille du titre :", titleSizeSpinner);
        addRow(stylePanel, row++, "Style du titre :", titleStylePanel);
        addRow(stylePanel, row++, "Taille du texte :", textSizeSpinner);
        addRow(stylePanel, row++, "Style du texte :", textStylePanel);
        addRow(stylePanel, row++, "Alignement :", alignCombo);
        addRow(stylePanel, row++, "Couleur titre :", titleColorSelector);
        addRow(stylePanel, row++, "Couleur texte :", textColorSelector);
        addRow(stylePanel, row++, "Couleur fond :", backgroundColorSelector);
        addRow(stylePanel, row++, "Couleur QR code :", qrColorSelector);
        addRow(stylePanel, row++, "", resetStyleButton);

        fontCombo.addActionListener(e -> updateFontFileControls());
        fontFileButton.addActionListener(e -> chooseFontFile());

        resetStyleButton.addActionListener(e -> {
            setStyle(new PdfStyle());
            qrColorSelector.fireChange();
        });

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Style", stylePanel);
        tabs.addTab("Images", buildImagesPanel());
        contentPane.add(tabs, BorderLayout.EAST);
    }

    private void basFenetre() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttons.add(generateButton);
        buttons.add(exportButton);
        contentPane.add(buttons, BorderLayout.SOUTH);
    }

    private void addRow(JPanel panel, int row, String label, JComponent component) {
        GridBagConstraints l = new GridBagConstraints();
        l.gridx = 0;
        l.gridy = row;
        l.anchor = GridBagConstraints.WEST;
        l.insets = new Insets(3, 5, 3, 8);
        panel.add(new JLabel(label), l);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = row;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(3, 0, 3, 5);
        panel.add(component, c);
    }

    private void updateFontFileControls() {
        boolean custom = fontCombo.getSelectedItem() == PdfStyle.FontFamily.CUSTOM;
        fontFileButton.setEnabled(custom);
        fontFileLabel.setEnabled(custom);
    }

    private void chooseFontFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choisir une police");
        chooser.setFileFilter(new FileNameExtensionFilter("Polices (*.ttf, *.otf)", "ttf", "otf"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            customFontFile = chooser.getSelectedFile();
            fontFileLabel.setText(customFontFile.getName());
        }
    }

    public PdfStyle getStyle() {
        PdfStyle style = new PdfStyle();
        style.setTitleText(titleField.getText());
        style.setFontFamily((PdfStyle.FontFamily) fontCombo.getSelectedItem());
        style.setCustomFontFile(customFontFile);
        style.setTitleSize(((Number) titleSizeSpinner.getValue()).intValue());
        style.setTextSize(((Number) textSizeSpinner.getValue()).intValue());
        style.setTitleBold(titleBoldBox.isSelected());
        style.setTitleItalic(titleItalicBox.isSelected());
        style.setTextBold(textBoldBox.isSelected());
        style.setTextItalic(textItalicBox.isSelected());
        style.setAlignment((PdfStyle.Alignment) alignCombo.getSelectedItem());
        style.setTitleColor(titleColorSelector.getColor());
        style.setTextColor(textColorSelector.getColor());
        style.setBackgroundColor(backgroundColorSelector.getColor());
        style.setQrColor(qrColorSelector.getColor());
        return style;
    }

    public void setStyle(PdfStyle style) {
        titleField.setText(style.getTitleText());
        fontCombo.setSelectedItem(style.getFontFamily());
        customFontFile = style.getCustomFontFile();
        fontFileLabel.setText(customFontFile == null ? "Aucun fichier" : customFontFile.getName());
        titleSizeSpinner.setValue(style.getTitleSize());
        textSizeSpinner.setValue(style.getTextSize());
        titleBoldBox.setSelected(style.isTitleBold());
        titleItalicBox.setSelected(style.isTitleItalic());
        textBoldBox.setSelected(style.isTextBold());
        textItalicBox.setSelected(style.isTextItalic());
        alignCombo.setSelectedItem(style.getAlignment());
        titleColorSelector.setColor(style.getTitleColor());
        textColorSelector.setColor(style.getTextColor());
        backgroundColorSelector.setColor(style.getBackgroundColor());
        qrColorSelector.setColor(style.getQrColor());
        updateFontFileControls();
    }

    private JPanel buildImagesPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        imageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        imageList.setCellRenderer(new ImageCellRenderer());
        JScrollPane listScroll = new JScrollPane(imageList);
        listScroll.setPreferredSize(new Dimension(300, 150));
        panel.add(listScroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(5, 8));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttons.add(addImageButton);
        buttons.add(removeImageButton);
        bottom.add(buttons, BorderLayout.NORTH);

        JPanel settings = new JPanel(new GridBagLayout());
        settings.setBorder(BorderFactory.createTitledBorder("Image sélectionnée"));
        addRow(settings, 0, "Emplacement :", imagePositionCombo);
        addRow(settings, 1, "Alignement :", imageAlignCombo);
        addRow(settings, 2, "Largeur (% de la page) :", imageWidthSpinner);
        bottom.add(settings, BorderLayout.CENTER);

        panel.add(bottom, BorderLayout.SOUTH);

        addImageButton.addActionListener(e -> addImagesFromDialog());
        removeImageButton.addActionListener(e -> removeSelectedImage());
        imageList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedImageIntoControls();
            }
        });
        imagePositionCombo.addActionListener(e -> applyImageControls());
        imageAlignCombo.addActionListener(e -> applyImageControls());
        imageWidthSpinner.addChangeListener(e -> applyImageControls());

        loadSelectedImageIntoControls();
        return panel;
    }

    private void addImagesFromDialog() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choisir des images");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Images (*.png, *.jpg, *.jpeg, *.gif, *.bmp)", "png", "jpg", "jpeg", "gif", "bmp"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        for (File file : chooser.getSelectedFiles()) {
            if (imageListModel.size() >= PdfImage.MAX_IMAGES) {
                showError("Vous ne pouvez pas ajouter plus de " + PdfImage.MAX_IMAGES + " images.");
                break;
            }
            imageListModel.addElement(new PdfImage(file));
        }
        if (!imageListModel.isEmpty()) {
            imageList.setSelectedIndex(imageListModel.size() - 1);
        }
    }

    private void removeSelectedImage() {
        int index = imageList.getSelectedIndex();
        if (index < 0) {
            return;
        }
        imageListModel.remove(index);
        if (!imageListModel.isEmpty()) {
            imageList.setSelectedIndex(Math.min(index, imageListModel.size() - 1));
        } else {
            loadSelectedImageIntoControls();
        }
    }

    private void loadSelectedImageIntoControls() {
        PdfImage selected = imageList.getSelectedValue();
        boolean hasSelection = selected != null;

        removeImageButton.setEnabled(hasSelection);
        imagePositionCombo.setEnabled(hasSelection);
        imageAlignCombo.setEnabled(hasSelection);
        imageWidthSpinner.setEnabled(hasSelection);

        if (hasSelection) {
            updatingImageControls = true;
            imagePositionCombo.setSelectedItem(selected.getPosition());
            imageAlignCombo.setSelectedItem(selected.getAlignment());
            imageWidthSpinner.setValue(selected.getWidthPercent());
            updatingImageControls = false;
        }
    }

    private void applyImageControls() {
        PdfImage selected = imageList.getSelectedValue();
        if (updatingImageControls || selected == null) {
            return;
        }
        selected.setPosition((PdfImage.Position) imagePositionCombo.getSelectedItem());
        selected.setAlignment((PdfStyle.Alignment) imageAlignCombo.getSelectedItem());
        selected.setWidthPercent(((Number) imageWidthSpinner.getValue()).intValue());
        imageList.repaint();
    }

    public List<PdfImage> getImages() {
        List<PdfImage> images = new ArrayList<>();
        for (int i = 0; i < imageListModel.size(); i++) {
            images.add(imageListModel.get(i).copy());
        }
        return images;
    }

    public void setImages(List<PdfImage> images) {
        imageListModel.clear();
        if (images != null) {
            for (PdfImage image : images) {
                imageListModel.addElement(image.copy());
            }
        }
        loadSelectedImageIntoControls();
    }

    public String getContent() {
        return inputArea.getText();
    }

    public void addGenerateListener(ActionListener listener) {
        generateButton.addActionListener(listener);
    }

    public void addExportListener(ActionListener listener) {
        exportButton.addActionListener(listener);
    }

    public void addQrColorListener(ActionListener listener) {
        qrColorSelector.addChangeListener(listener);
    }

    public void displayQRCode(BufferedImage image) {
        qrLabel.setText(null);
        qrLabel.setIcon(new ImageIcon(image));
    }

    public void setExportEnabled(boolean enabled) {
        exportButton.setEnabled(enabled);
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    public void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public File chooseSaveFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Enregistrer le PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("Fichiers PDF (*.pdf)", "pdf"));
        chooser.setSelectedFile(new File("qrcode.pdf"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File file = chooser.getSelectedFile();

        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            file = new File(file.getParentFile(), file.getName() + ".pdf");
        }

        if (file.exists()) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "Le fichier existe déjà. Voulez-vous le remplacer ?",
                    "Confirmation", JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) {
                return null;
            }
        }
        return file;
    }

    private static class ImageCellRenderer extends DefaultListCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof PdfImage) {
                PdfImage image = (PdfImage) value;
                setText(image.getFile().getName() + " — " + image.getPosition() + ", "
                        + image.getAlignment() + ", " + image.getWidthPercent() + " %");
            }
            return this;
        }
    }

    private static class ColorSelector extends JPanel {

        private static final long serialVersionUID = 1L;

        private final String dialogTitle;
        private final JLabel swatch = new JLabel();
        private final transient List<ActionListener> listeners = new ArrayList<>();
        private Color color;

        ColorSelector(String dialogTitle, Color initialColor) {
            super(new FlowLayout(FlowLayout.LEFT, 5, 0));
            this.dialogTitle = dialogTitle;

            swatch.setOpaque(true);
            swatch.setPreferredSize(new Dimension(40, 20));
            swatch.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            setColor(initialColor);

            JButton chooseButton = new JButton("Choisir…");
            chooseButton.addActionListener(e -> {
                Color chosen = JColorChooser.showDialog(this, this.dialogTitle, color);
                if (chosen != null) {
                	setColor(chosen);
                    fireChange();
                }
            });

            add(swatch);
            add(chooseButton);
        }

        Color getColor() {
            return color;
        }

        void setColor(Color newColor) {
            this.color = newColor;
            swatch.setBackground(newColor);
        }

        void addChangeListener(ActionListener listener) {
            listeners.add(listener);
        }

        void fireChange() {
            ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "color");
            for (ActionListener listener : listeners) {
                listener.actionPerformed(event);
            }
        }
    }
}
