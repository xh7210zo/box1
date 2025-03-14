package edu.uob;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Table {
    private final String storageFolderPath;
    private String currentDatabase;

    public Table(String storageFolderPath) {
        this.storageFolderPath = storageFolderPath;
    }

    public void setCurrentDatabase(String databaseName) {
        this.currentDatabase = databaseName;
    }

    public boolean createTable(String tableName, List<String> columns) throws IOException {
        if (currentDatabase == null) {
            System.out.println("[ERROR] No database selected.");
            return false;
        }

        String tablePath = storageFolderPath + File.separator + currentDatabase.toLowerCase() + File.separator + tableName.toLowerCase() + ".tab";
        File tableFile = new File(tablePath);

        if (tableFile.exists()) {
            System.out.println("[ERROR] Table already exists: " + tableName);
            return false;
        }

        List<String> cleanedColumns = new ArrayList<>();
        Set<String> uniqueColumns = new HashSet<>();

        for (String col : columns) {
            String cleanCol = col.replace("(", "")
                    .replace(")", "")
                    .replace(";", "")
                    .replace(",", "")
                    .trim();

            if (cleanCol.equalsIgnoreCase("id")) {
                System.out.println("[ERROR] Column name 'id' is not allowed.");
                return false;
            }

            if (!uniqueColumns.add(cleanCol)) {
                System.out.println("[ERROR] Duplicate column name found: " + cleanCol);
                return false;
            }
            cleanedColumns.add(cleanCol);
        }

        File databaseFolder = new File(storageFolderPath + File.separator + currentDatabase.toLowerCase());
        if (!databaseFolder.exists()) {
            databaseFolder.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile, StandardCharsets.UTF_8))) {
            writer.write("id");
            for (String col : cleanedColumns) {
                writer.write("\t" + col);
            }
            writer.newLine();
            System.out.println("[OK] Table created: " + tableName);
            return true;
        } catch (IOException e) {
            System.out.println("[ERROR] Failed to create table: " + e.getMessage());
            return false;
        }
    }
    public boolean dropTable(String tableName) {
        if (currentDatabase == null) {
            return false;
        }

        String cleanTableName = tableName.replace(";", "").trim();
        String tablePath = storageFolderPath + File.separator + currentDatabase.toLowerCase() + File.separator + cleanTableName.toLowerCase() + ".tab";
        File tableFile = new File(tablePath);

        return tableFile.exists() && tableFile.delete();
    }
}
