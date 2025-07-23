package org.app.reader;

import com.opencsv.CSVReader;
import org.app.model.InvoiceData;

import java.io.File;
import java.io.FileReader;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InvoiceReaderCsv {

    private static final DateTimeFormatter IN_FMT  = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter OUT_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static List<InvoiceData> readCsv(File csvFile) throws Exception {
        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            // 1) Read header row
            String[] headers = reader.readNext();
            if (headers == null) {
                throw new IllegalArgumentException("CSV has no header row");
            }

            // 2) Build header→index map
            Map<String,Integer> idx = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                idx.put(headers[i].trim(), i);
            }

            // 3) Read data rows
            List<InvoiceData> invoiceDataList = new ArrayList<>();
            String[] row;
            while ((row = reader.readNext()) != null) {
                if (isAllEmpty(row)) continue;
                InvoiceData inv = new InvoiceData();


                inv.setAwb(get(row, idx, "Număr de urmărire expediere"));
                inv.setDate(get(row, idx, "Stat expeditor"));

                inv.setExporterName(get(row, idx, "Companie sau nume expeditor"));
                inv.setExporterAddress(get(row, idx, "Linie adresă 1 pentru expeditor"));
                inv.setExporterCity(get(row, idx, "Oraș expeditor"));
                inv.setExporterCountry(get(row, idx, "Țară expeditor"));
                inv.setExporterPostalCode(get(row, idx, "Cod poștal expeditor"));

                inv.setImporterName(get(row, idx, "Destinatar – Companie sau nume"));
                inv.setImporterAddress(get(row, idx, "Destinație expediere – Linie adresă 1"));
                inv.setImporterCity(get(row, idx, "Destinatar – Localitate"));
                inv.setImporterCountry(get(row, idx, "Țară sau teritoriu destinatar"));
                inv.setImporterPostalCode(get(row, idx, "Cod poștal destinatar"));

                inv.setGoodsDescription(get(row, idx, "Descriere bunuri"));
                inv.setValue(get(row, idx, "Totaluri linie factură"));
                inv.setCurrency(get(row, idx, "Cod monedă"));
                inv.setNumberOfPackages(get(row, idx, "Nr. colete din expediere"));
                inv.setWeightGross(get(row, idx, "Greutate reală expediere"));
                inv.setCountryItineraryCodes(inv.getExporterCountry() + ";" + inv.getImporterCountry());







                invoiceDataList.add(inv);
            }
            return invoiceDataList;
        }
    }

    // Safe getter: returns "" if header not found or cell missing
    private static String get(String[] row, Map<String,Integer> idx, String header) {
        Integer i = idx.get(header);
        if (i == null || i < 0 || i >= row.length) return "";
        return row[i].trim();
    }

    private static boolean isAllEmpty(String[] row) {
        for (String s : row) {
            if (s != null && !s.trim().isEmpty()) return false;
        }
        return true;
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return (s.length() <= maxLen) ? s : s.substring(0, maxLen);
    }
}
