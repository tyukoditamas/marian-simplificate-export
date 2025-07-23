package org.app.ui;

import org.apache.poi.ss.usermodel.Workbook;
import org.app.model.InvoiceData;
import org.app.reader.InvoiceReaderCsv;
import org.app.service.ExcelService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.util.List;

public class MainFrame extends JFrame {
    private final JTextArea logArea = new JTextArea();
    private final ExcelService excelService = new ExcelService("Borderou Centralizator - MST - EXPORT.xlsx");

    // UI components
    private final JRadioButton rbNew = new JRadioButton("Creaza excel nou din template");
    private final JRadioButton rbAppend = new JRadioButton("Adauga date la excel existent");
    private final JButton btnSelectCsv = new JButton("Selecteaza CSV");
    private final JLabel lblCsvPath = new JLabel("<niciun fisier>");
    private final JButton btnSelectTarget = new JButton("Selecteaza folder destinatie");
    private final JLabel lblTargetPath = new JLabel("<niciun target>");
    private final JButton btnProcess = new JButton("Start Proces");

    private File csvFile;
    private File targetFile;

    public MainFrame() {
        super("Simplificate Export - Generare Borderou Centralizator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null);

        // Mode Selection
        rbNew.setActionCommand("NEW");
        rbAppend.setActionCommand("APPEND");
        ButtonGroup group = new ButtonGroup();
        group.add(rbNew);
        group.add(rbAppend);
        rbNew.setSelected(true);

        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        modePanel.add(rbNew);
        modePanel.add(rbAppend);

        // CSV Selection Panel
        JPanel csvPanel = new JPanel(new BorderLayout(5, 0));
        csvPanel.add(btnSelectCsv, BorderLayout.WEST);
        csvPanel.add(lblCsvPath, BorderLayout.CENTER);

        // Target Selection Panel
        JPanel targetPanel = new JPanel(new BorderLayout(5, 0));
        targetPanel.add(btnSelectTarget, BorderLayout.WEST);
        targetPanel.add(lblTargetPath, BorderLayout.CENTER);

        // Listeners
        btnSelectCsv.addActionListener(e -> chooseCsv());
        btnSelectTarget.addActionListener(e -> chooseTarget(rbNew.isSelected()));
        btnProcess.addActionListener(e -> onProcess());
        rbNew.addActionListener(e -> onModeChanged());
        rbAppend.addActionListener(e -> onModeChanged());

        // Layout
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        controlPanel.add(modePanel);
        controlPanel.add(Box.createVerticalStrut(10));
        controlPanel.add(csvPanel);
        controlPanel.add(Box.createVerticalStrut(10));
        controlPanel.add(targetPanel);
        controlPanel.add(Box.createVerticalStrut(20));
        controlPanel.add(btnProcess);

        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        getContentPane().setLayout(new BorderLayout(10, 10));
        getContentPane().add(controlPanel, BorderLayout.NORTH);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
    }

    private void onModeChanged() {
        csvFile = null;
        targetFile = null;
        lblCsvPath.setText("<niciun fisier>");
        lblTargetPath.setText("<niciun target>");
        updateTargetButtonLabel();
        validateProcessButton();
    }

    private void updateTargetButtonLabel() {
        btnSelectTarget.setText(rbNew.isSelected() ? "Selecteaza folder destinatie" : "Selecteaza fisier .xlsx");
    }

    private void chooseCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            csvFile = chooser.getSelectedFile();
            lblCsvPath.setText(csvFile.getAbsolutePath());
        }
        validateProcessButton();
    }

    private void chooseTarget(boolean createNew) {
        JFileChooser chooser = new JFileChooser();
        if (createNew) {
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        } else {
            chooser.setFileFilter(new FileNameExtensionFilter("Excel files (.xlsx)", "xlsx"));
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File sel = chooser.getSelectedFile();
            if (!createNew && !sel.getName().toLowerCase().endsWith(".xlsx")) {
                JOptionPane.showMessageDialog(this,
                        "Trebuie selectat un fișier .xlsx valid.",
                        "Eroare", JOptionPane.ERROR_MESSAGE);
                return;
            }
            targetFile = createNew ? excelService.resolveNewTarget(sel) : sel;
            lblTargetPath.setText(targetFile.getAbsolutePath());
        }
        validateProcessButton();
    }

    private void validateProcessButton() {
        btnProcess.setEnabled(csvFile != null && targetFile != null);
    }

    private void onProcess() {
        try {
            logArea.append("Procesare CSV: " + csvFile.getName() + " ");
            boolean createNew = rbNew.isSelected();
            Workbook wb = excelService.openWorkbook(createNew, targetFile);
            List<InvoiceData> rows = InvoiceReaderCsv.readCsv(csvFile);
            excelService.appendRowsByHeader(wb, rows);
            excelService.saveWorkbook(wb, targetFile);
            logArea.append("Salvat fișier: " + targetFile.getName() + " (" + rows.size() + " înregistrari) ");
            JOptionPane.showMessageDialog(this, "Gata! " + rows.size() + " înregistrari procesate. " + targetFile.getName(),
                    "Finalizat", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            logArea.append("Eroare: " + ex.getMessage() + " ");
            JOptionPane.showMessageDialog(this, "A aparut o eroare: " + ex.getMessage(), "Eroare", JOptionPane.ERROR_MESSAGE);
        }
    }
}
