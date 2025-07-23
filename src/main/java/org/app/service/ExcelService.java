package org.app.service;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.app.model.InvoiceData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ExcelService {
    private static final DateTimeFormatter DATE_PREFIX = DateTimeFormatter.BASIC_ISO_DATE;
    private final String templateResource;
    private static final int HEADER_ROW_INDEX = 6; // zero-based index of header in template

    /**
     * @param templateResource filename of .xlsx template on classpath under /excel-template/
     */
    public ExcelService(String templateResource) {
        this.templateResource = templateResource;
    }

    /**
     * Opens the workbook. If createNew is true, loads the .xlsx template from the classpath into memory;
     * otherwise loads the existing .xlsx target file.
     */
    public Workbook openWorkbook(boolean createNew, File target) throws IOException {
        if (createNew) {
            try (InputStream is = getClass().getResourceAsStream("/excel-template/" + templateResource)) {
                if (is == null) {
                    throw new FileNotFoundException("Template not found: " + templateResource);
                }
                // XSSFWorkbook reads entire .xlsx (Zip) including pictures, styles, etc.
                return new XSSFWorkbook(is);
            }
        } else {
            // Only .xlsx allowed
            try (InputStream fis = new FileInputStream(target)) {
                return WorkbookFactory.create(fis);
            }
        }
    }

    private int findHeaderRow(Sheet sheet, String headerName) {
        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (Cell c : row) {
                if (headerName.equals(c.getStringCellValue().trim())) {
                    return r;
                }
            }
        }
        throw new IllegalStateException("Header '" + headerName + "' not found");
    }

    /**
     * Appends a list of InvoiceData rows to the first sheet of the workbook,
     * using header-based mapping. Header is expected at row index HEADER_ROW_INDEX.
     */
    public void appendRowsByHeader(Workbook wb, List<InvoiceData> rows) {
        Sheet sheet = wb.getSheetAt(0);

        // 1) Find header row by name
        int hdrIdx = findHeaderRow(sheet, "Nr. Document transport unic AWB");
        Row headerRow = sheet.getRow(hdrIdx);

        // 2) Build header→column map
        Map<String,Integer> colIdx = new HashMap<>();
        for (Cell c : headerRow) {
            String name = c.getStringCellValue().trim();
            if (!name.isEmpty()) colIdx.put(name, c.getColumnIndex());
        }

        // 3) Pick a “key” column to detect real data rows
        int keyCol = colIdx.get("Nr. Document transport unic AWB");

        // 4) Scan for the last non-empty row in that column
        int lastDataRow = hdrIdx;
        int max = sheet.getLastRowNum();
        for (int r = hdrIdx+1; r <= max; r++) {
            Row row = sheet.getRow(r);
            if (row != null) {
                Cell cell = row.getCell(keyCol);
                if (cell != null
                        && cell.getCellType() != CellType.BLANK
                        && !cell.toString().trim().isEmpty()) {
                    lastDataRow = r;
                }
            }
        }

        // 5) Now append after that
        int startRow = lastDataRow + 1;
        for (InvoiceData inv : rows) {
            Row row = sheet.createRow(startRow++);
            writeCell(row, colIdx, "Nr. Document transport unic AWB", inv.getAwb());
            writeCell(row, colIdx, "Data", inv.getDate());
            writeCell(row, colIdx, "Nume Expeditor/Exportator", inv.getExporterName());
            writeCell(row, colIdx, "Strada si numarul Expeditor/Exportator", inv.getExporterAddress());
            writeCell(row, colIdx, "Orasul Expeditor/Exportator", inv.getExporterCity());
            writeCell(row, colIdx, "Codul postal Expeditor/Exportator", inv.getExporterPostalCode());
            writeCell(row, colIdx, "Tara Expeditor/Exportator", inv.getExporterCountry());
            writeCell(row, colIdx, "Nume Destinatar/Importator", inv.getImporterName());
            writeCell(row, colIdx, "Strada si numarul Destinatar/Importator", inv.getImporterAddress());
            writeCell(row, colIdx, "Orasul Destinatar/Importator", inv.getImporterCity());
            writeCell(row, colIdx, "Codul postal Destinatar/Importator", inv.getImporterPostalCode());
            writeCell(row, colIdx, "Tara Destinatar/Importator", inv.getImporterCountry());
            writeCell(row, colIdx, "Descrierea marfurilor", inv.getGoodsDescription());
            writeCell(row, colIdx, "Valoare", inv.getValue());
            writeCell(row, colIdx, "Moneda", inv.getCurrency());
            writeCell(row, colIdx, "Numar de pachete", inv.getNumberOfPackages());
            writeCell(row, colIdx, "Greutate bruta (kilograme)", inv.getWeightGross());
            writeCell(row, colIdx, "Metoda de plata a cheltuielilor de transport", "");
            writeCell(row, colIdx, "Codul tarilor de pe itinerar", inv.getCountryItineraryCodes());
        }
    }

    private void writeCell(Row row, Map<String, Integer> colIdx, String headerName, String value) {
        Integer idx = colIdx.get(headerName);
        if (idx != null) {
            Cell cell = row.createCell(idx);
            cell.setCellValue(value != null ? value : "");
        }
    }

    /**
     * Saves the workbook to the given target file. Always writes .xlsx format.
     */
    public void saveWorkbook(Workbook wb, File target) throws IOException {
        try (OutputStream out = new FileOutputStream(target)) {
            wb.write(out);
        }
    }

    /**
     * Given an output directory, returns a File named with current-date prefix
     * and same .xlsx extension as the template.
     */
    public File resolveNewTarget(File outDir) {
        String ext = templateResource.substring(templateResource.lastIndexOf('.'));
        String name = LocalDate.now().format(DATE_PREFIX) + "_Borderou Centralizator - MST - EXPORT" + ext;
        return new File(outDir, name);
    }
}
