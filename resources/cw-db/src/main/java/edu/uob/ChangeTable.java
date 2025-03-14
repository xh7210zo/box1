package edu.uob;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class ChangeTable {

    private String storageFolderPath;
    private String currentDatabase;

    public ChangeTable(String storageFolderPath, String currentDatabase) {
        this.storageFolderPath = storageFolderPath;
        this.currentDatabase = currentDatabase;
    }

    public boolean alterTableAddColumn(String tableName, String columnName) {
        if (currentDatabase == null) {
            return false;
        }

        String tablePath = storageFolderPath + File.separator + currentDatabase.toLowerCase() + File.separator + tableName.toLowerCase() + ".tab";
        File tableFile = new File(tablePath);

        if (!tableFile.exists()) {
            return false;
        }

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(tableFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        String firstLine = lines.get(0);
        String[] existingColumns = firstLine.split("\t");

        for (String existingColumn : existingColumns) {
            if (existingColumn.trim().equalsIgnoreCase(columnName)) {
                return false;
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile, StandardCharsets.UTF_8))) {
            writer.write(firstLine + "\t" + columnName);
            writer.newLine();

            for (int i = 1; i < lines.size(); i++) {
                writer.write(lines.get(i) + "\t");
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean alterTableDropColumn(String tableName, String columnName) {
        if (currentDatabase == null) {
            return false;
        }

        String tablePath = storageFolderPath + File.separator + currentDatabase.toLowerCase() + File.separator + tableName.toLowerCase() + ".tab";
        File tableFile = new File(tablePath);

        if (!tableFile.exists()) {
            return false;
        }

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(tableFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        String firstLine = lines.get(0);
        String[] existingColumns = firstLine.split("\t");

        int columnIndex = -1;
        for (int i = 0; i < existingColumns.length; i++) {
            if (existingColumns[i].trim().equalsIgnoreCase(columnName)) {
                columnIndex = i;
                break;
            }
        }

        if (columnIndex == -1) {
            return false;
        }

        List<String> updatedLines = new ArrayList<>();
        for (String line : lines) {
            String[] columns = line.split("\t");
            List<String> updatedColumns = new ArrayList<>(Arrays.asList(columns));
            updatedColumns.remove(columnIndex);
            updatedLines.add(String.join("\t", updatedColumns));
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile, StandardCharsets.UTF_8))) {
            for (String updatedLine : updatedLines) {
                writer.write(updatedLine);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
    public boolean insertIntoTable(String tableName, List<String> values) throws IOException {
        if (currentDatabase == null) {
            return false;
        }

        String tablePath = storageFolderPath + File.separator + currentDatabase.toLowerCase() + File.separator + tableName.toLowerCase() + ".tab";
        File tableFile = new File(tablePath);

        if (!tableFile.exists()) {
            return false;
        }

        List<String> existingData = Files.readAllLines(tableFile.toPath(), StandardCharsets.UTF_8);

        String[] headerColumns = existingData.get(0).split("\t");
        int columnCount = headerColumns.length - 1;

        List<String> cleanedValues = new ArrayList<>();
        for (String value : values) {
            String cleanValue = value.replace("(", "")
                    .replace(")", "")
                    .replace(";", "")
                    .replace("'", "")
                    .replace(",", "")
                    .trim();
            cleanedValues.add(cleanValue);
        }

        if (cleanedValues.size() != columnCount) {
            return false;
        }

        int nextId = existingData.size();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile, true))) {
            writer.write(nextId + "\t" + String.join("\t", cleanedValues));
            writer.newLine();
        }

        return true;
    }

    public String updateTable(String tableName, Map<String, String> setValues, String whereColumn, String whereValue) throws IOException {
        if (currentDatabase == null) {
            return "[ERROR] No database selected.";
        }

        String tablePath = storageFolderPath + File.separator + currentDatabase.toLowerCase() + File.separator + tableName.toLowerCase() + ".tab";
        File tableFile = new File(tablePath);
        if (!tableFile.exists()) {
            return "[ERROR] Table does not exist: " + tableName;
        }

        TableReader tableReader = new TableReader(storageFolderPath, currentDatabase);
        List<List<String>> tableData = tableReader.readTable(tableName);
        if (tableData.isEmpty()) {
            return "[ERROR] Table is empty.";
        }

        List<String> header = tableData.get(0);
        int whereIndex = header.indexOf(whereColumn);
        if (whereIndex == -1) {
            return "[ERROR] Column not found: " + whereColumn;
        }

        for (String col : setValues.keySet()) {
            if ("id".equalsIgnoreCase(col)) {
                return "[ERROR] Cannot update the ID column.";
            }
            if (!header.contains(col)) {
                return "[ERROR] Column not found in table: " + col;
            }
        }

        boolean updated = false;
        for (int i = 1; i < tableData.size(); i++) {
            List<String> row = tableData.get(i);
            if (row.get(whereIndex).replace("'", "").equals(whereValue)) {
                for (Map.Entry<String, String> entry : setValues.entrySet()) {
                    int colIndex = header.indexOf(entry.getKey());
                    row.set(colIndex, entry.getValue());
                }
                updated = true;
            }
        }

        if (!updated) {
            return "[ERROR] No matching record found.";
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile, StandardCharsets.UTF_8))) {
            for (List<String> row : tableData) {
                writer.write(String.join("\t", row));
                writer.newLine();
            }
        }

        return "[OK] Update successful.";
    }


    public String deleteFromTable(String tableName, String whereColumn, String whereValue) throws IOException {
        if (currentDatabase == null) {
            return "[ERROR] No database selected.";
        }

        String tablePath = storageFolderPath + File.separator + currentDatabase.toLowerCase() + File.separator + tableName.toLowerCase() + ".tab";
        File tableFile = new File(tablePath);

        if (!tableFile.exists()) {
            return "[ERROR] Table does not exist: " + tableName;
        }

        if ("id".equalsIgnoreCase(whereColumn)) {
            return "[ERROR] Cannot delete from the ID column.";
        }

        TableReader tableReader = new TableReader(storageFolderPath, currentDatabase);
        List<List<String>> tableData = tableReader.readTable(tableName);
        if (tableData.isEmpty()) {
            return "[ERROR] Table is empty.";
        }

        List<String> header = tableData.get(0);
        int whereIndex = header.indexOf(whereColumn);

        if (whereIndex == -1) {
            return "[ERROR] Column not found: " + whereColumn;
        }

        List<List<String>> updatedTableData = new ArrayList<>();
        updatedTableData.add(header);

        boolean rowDeleted = false;

        for (int i = 1; i < tableData.size(); i++) {
            List<String> row = tableData.get(i);
            String rowValue = row.get(whereIndex).replace("'", "").trim();

            if (!rowValue.equals(whereValue)) {
                updatedTableData.add(row);
            } else {
                rowDeleted = true;
            }
        }

        if (!rowDeleted) {
            return "[ERROR] No matching record found.";
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile, StandardCharsets.UTF_8))) {
            for (List<String> row : updatedTableData) {
                writer.write(String.join("\t", row));
                writer.newLine();
            }
        }

        return "[OK] Record deleted.";
    }

    public String joinTables(String table1, String table2, String column1, String column2) throws IOException {
        if (currentDatabase == null) {
            return "[ERROR] No database selected.";
        }
        TableReader tableReader = new TableReader(storageFolderPath, currentDatabase);
        List<List<String>> table1Data = tableReader.readTable(table1);
        List<List<String>> table2Data = tableReader.readTable(table2);

        if (table1Data.isEmpty() || table2Data.isEmpty()) {
            return "[ERROR] One or both tables are empty.";
        }

        List<String> header1 = table1Data.get(0);
        List<String> header2 = table2Data.get(0);

        int index1 = header1.indexOf(column1);
        int index2 = header2.indexOf(column2);

        if (index1 == -1) return "[ERROR] Column not found in " + table1 + ": " + column1;
        if (index2 == -1) return "[ERROR] Column not found in " + table2 + ": " + column2;

        List<String> joinedHeader = new ArrayList<>(header1);
        joinedHeader.addAll(header2);

        List<List<String>> joinResult = new ArrayList<>();
        joinResult.add(joinedHeader);

        for (int i = 1; i < table1Data.size(); i++) {
            List<String> row1 = table1Data.get(i);
            String key1 = row1.get(index1).trim();

            for (int j = 1; j < table2Data.size(); j++) {
                List<String> row2 = table2Data.get(j);
                String key2 = row2.get(index2).trim();

                if (key1.equals(key2)) {
                    List<String> joinedRow = new ArrayList<>(row1);
                    joinedRow.addAll(row2);
                    joinResult.add(joinedRow);
                }
            }
        }

        if (joinResult.size() == 1) {
            return "[ERROR] No matching records found.";
        }

        StringBuilder result = new StringBuilder();
        for (List<String> row : joinResult) {
            result.append(String.join("\t", row)).append("\n");
        }

        return "[OK]\n" + result.toString();
    }
}