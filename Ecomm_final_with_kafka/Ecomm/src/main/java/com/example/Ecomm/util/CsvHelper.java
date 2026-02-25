package com.example.Ecomm.util;

import com.example.Ecomm.dto.ProductDTO;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CsvHelper {

    // ✅ static final + renamed + non-public
    private static final String[] headers = {
            "name", "description", "price", "categoryId", "stockQuantity", "images"
    };

    // ✅ private constructor (utility class)
    private CsvHelper() {
    }

    public static List<ProductDTO> csvToProductDTOs(InputStream is) {

        try (BufferedReader fileReader =
                     new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
             CSVParser csvParser =
                     CSVFormat.DEFAULT.builder()
                             .setHeader(headers)
                             .setSkipHeaderRecord(true)
                             .setIgnoreHeaderCase(true)
                             .setTrim(true)
                             .build()
                             .parse(fileReader)) {

            List<ProductDTO> products = new ArrayList<>();

            for (CSVRecord csvRecord : csvParser.getRecords()) {
                products.add(parseCsvRecord(csvRecord));
            }

            return products;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to parse CSV file due to an IO error.", e);
        }
    }

    // ✅ Extracted nested try block
    private static ProductDTO parseCsvRecord(CSVRecord csvRecord) {

        try {
            ProductDTO productDTO = new ProductDTO();
            productDTO.setName(csvRecord.get("name"));
            productDTO.setDescription(csvRecord.get("description"));
            productDTO.setPrice(new BigDecimal(csvRecord.get("price")));
            productDTO.setCategoryId(Long.parseLong(csvRecord.get("categoryId")));
            productDTO.setStockQuantity(
                    Long.parseLong(csvRecord.get("stockQuantity")));

            String imagesString = csvRecord.get("images");
            if (imagesString != null && !imagesString.trim().isEmpty()) {
                productDTO.setImages(
                        Arrays.stream(imagesString.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .toList() // ✅ unmodifiable list
                );
            } else {
                productDTO.setImages(new ArrayList<>());
            }

            return productDTO;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "CSV parsing error: Invalid number format at row "
                            + csvRecord.getRecordNumber(), e);

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "CSV parsing error: Missing or invalid header at row "
                            + csvRecord.getRecordNumber(), e);
        }
    }

    public static boolean hasCsvFormat(MultipartFile file) {

        if (file == null) {
            return false;
        }

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();

        return "text/csv".equals(contentType)
                || (filename != null && filename.toLowerCase().endsWith(".csv"));
    }
}
