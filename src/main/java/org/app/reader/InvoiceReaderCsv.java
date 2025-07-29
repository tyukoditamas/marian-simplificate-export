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


                inv.setAwb(get(row, idx, "TRACKING"));
                inv.setDate(get(row, idx, "DATE"));

                inv.setExporterName(get(row, idx, "EXPORTER"));
                inv.setExporterAddress(get(row, idx, "DIR1S"));
                inv.setExporterCity(get(row, idx, "DIR2S"));

                String dir3s = get(row, idx, "DIR3S");
                String[] parts1 = dir3s.trim().split("\\s+");

                inv.setExporterCountry(parts1[1]);
                inv.setExporterPostalCode(parts1[0]);

                inv.setImporterName(get(row, idx, "IMPORTER"));
                inv.setImporterAddress(get(row, idx, "DIR1"));
                inv.setImporterCity(get(row, idx, "DIR2"));

                String dir3 = get(row, idx, "DIR3");
                String[] parts2 = dir3.trim().split("\\s+");
                String countryCode = parts2[parts2.length - 1];

                inv.setImporterCountry(countryCode);
                //inv.setImporterPostalCode(get(row, idx, "Cod poștal destinatar"));

                inv.setGoodsDescription(get(row, idx, "DESCRIPTION"));
                inv.setValue(get(row, idx, "VALUE"));
                inv.setCurrency(get(row, idx, "CURRENCY"));
                inv.setNumberOfPackages(get(row, idx, "PARTS"));
                inv.setWeightGross(get(row, idx, "WEIGHT"));
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
