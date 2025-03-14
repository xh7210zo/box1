package edu.uob;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class TableQuery {

    private String storageFolderPath;
    private String currentDatabase;

    public TableQuery(String storageFolderPath, String currentDatabase) {
        this.storageFolderPath = storageFolderPath;
        this.currentDatabase = currentDatabase;
    }

    public String selectIdFromTable(String tableName, String whereColumn, String whereValue) throws IOException {
        if (currentDatabase == null) {
            return "[ERROR] No database selected.";
        }
        String cleanwhereValue = whereValue.replace(";", "").trim();
        String tablePath = storageFolderPath + File.separator + currentDatabase.toLowerCase() + File.separator + tableName.toLowerCase() + ".tab";
        File tableFile = new File(tablePath);

        if (!tableFile.exists()) {
            return "[ERROR] Table does not exist: " + tableName;
        }
        TableReader tableReader = new TableReader(storageFolderPath, currentDatabase);
        List<List<String>> tableData = tableReader.readTable(tableName);
        if (tableData.isEmpty()) {
            return "[ERROR] Table is empty";
        }

        List<String> header = tableData.get(0);
        header = header.stream()
                .map(s -> s.replaceAll("[^a-zA-Z0-9_]", ""))
                .collect(Collectors.toList());
        int idIndex = header.indexOf("id");
        int whereIndex = header.indexOf(whereColumn);
        if (idIndex == -1) {
            return "[ERROR] Table does not contain 'id' column.";
        }
        if (whereIndex == -1) {
            return "[ERROR] Column not found: " + whereColumn;
        }

        for (int i = 1; i < tableData.size(); i++) {
            List<String> row = tableData.get(i);
            String cleanRowValue = row.get(whereIndex)
                    .replace("'", "")
                    .replace(",", "")
                    .replace(";", "")
                    .replace("(", "")
                    .replace(")", "")
                    .trim();

            if (cleanRowValue.equals(cleanwhereValue)) {
                return "[OK] " + row.get(idIndex);
            }
        }
        return "[ERROR] No matching record found.";
    }
}
