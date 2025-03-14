package edu.uob;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TableQuery {

    private String storageFolderPath;
    private String currentDatabase;

    public TableQuery(String storageFolderPath, String currentDatabase) {
        this.storageFolderPath = storageFolderPath;
        this.currentDatabase = currentDatabase;
    }

    public String selectTable(String tableName, String wildAttribList, String whereClause) throws IOException {
        if (currentDatabase == null) {
            return "[ERROR] No database selected.";
        }

        String tablePath = storageFolderPath + File.separator + currentDatabase.toLowerCase() + File.separator + tableName.toLowerCase() + ".tab";
        File tableFile = new File(tablePath);

        if (!tableFile.exists()) {
            return "[ERROR] Table does not exist: " + tableName;
        }

        //read the content of the table
        TableReader tableReader = new TableReader(storageFolderPath, currentDatabase);
        List<List<String>> tableData = tableReader.readTable(tableName);
        if (tableData.isEmpty()) {
            return "[ERROR] Table is empty.";
        }

        //decide which column to choose
        List<String> header = tableData.get(0);
        List<String> selectedColumns;
        if ("*".equals(wildAttribList)) {
            selectedColumns = header;
        } else {
            selectedColumns = Arrays.asList(wildAttribList.split(","));
        }

        //check if the column exists
        for (String column : selectedColumns) {
            if (!header.contains(column)) {
                return "[ERROR] Attribute does not exist: " + column;
            }
        }

        //deal with the WHERE conditions
        List<List<String>> resultData = new ArrayList<>();
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            String[] conditions = whereClause.split("AND|OR");
            List<String> andConditions = new ArrayList<>();
            List<String> orConditions = new ArrayList<>();

            for (String condition : conditions) {
                condition = condition.trim();
                if (condition.contains("AND")) {
                    andConditions.add(condition);
                } else if (condition.contains("OR")) {
                    orConditions.add(condition);
                } else {
                    andConditions.add(condition);
                }
            }

            // matching against the criteria
            for (List<String> row : tableData.subList(1, tableData.size())) {
                boolean conditionMatched = evaluateConditions(andConditions, orConditions, row, header);

                if (conditionMatched) {
                    // If the condition is met, add the result
                    resultData.add(row);
                }
            }
        } else {
            // if there is no WHERE clause, return all data
            resultData = tableData.subList(1, tableData.size());
        }

        StringBuilder result = new StringBuilder();
        result.append(String.join("\t", selectedColumns)).append("\n");

        //output each row of data according to the selected column
        for (List<String> row : resultData) {
            List<String> selectedRow = new ArrayList<>();
            for (String column : selectedColumns) {
                int colIndex = header.indexOf(column);
                if (colIndex != -1) {
                    selectedRow.add(row.get(colIndex));
                }
            }
            result.append(String.join("\t", selectedRow)).append("\n");
        }

        return "[OK]\n" + result;
    }

    private boolean evaluateConditions(List<String> andConditions, List<String> orConditions, List<String> row, List<String> header) {
        boolean conditionMatched = true;

        //handle all the AND
        for (String condition : andConditions) {
            boolean result = evaluateCondition(condition, row, header);
            if (!result) {
                conditionMatched = false;
                break;
            }
        }

        //handle all the OR
        for (String condition : orConditions) {
            boolean result = evaluateCondition(condition, row, header);
            if (result) {
                return true;
            }
        }

        return conditionMatched;
    }

    private boolean evaluateCondition(String condition, List<String> row, List<String> header) {

        // Decomposition condition (e.g. "column == value")
        String[] parts = condition.split("==|!=|>|<|LIKE");
        if (parts.length != 2) {
            System.out.println("[ERROR] Invalid condition: " + condition);
            return false;
        }

        String column = parts[0].trim().replace("(", "");
        String value = parts[1].trim().replace("'", "").replace(")", "");

        //find the index of column
        int colIndex = header.indexOf(column);
        if (colIndex == -1) {
            System.out.println("[ERROR] Column not found: " + column);
            return false;
        }

        //get the value
        String rowValue = row.get(colIndex).trim().replace("'", "");
        return evaluateCondition(column, condition, value, rowValue);
    }

    private boolean evaluateCondition(String column, String condition, String value, String rowValue) {

        value = value.trim().toLowerCase();
        rowValue = rowValue.trim().toLowerCase();

        //add method to match 'LIKE'
        if (condition.contains("LIKE")) {
            return rowValue.contains(value);
        }

        if (condition.contains("==")) {
            return rowValue.equals(value);
        }
        else if (condition.contains("!=")) {
            return !rowValue.equals(value);
        }
        else if (condition.contains(">")) {
            try {
                int rowIntValue = Integer.parseInt(rowValue);
                int compareValue = Integer.parseInt(value);
                return rowIntValue > compareValue;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        else if (condition.contains("<")) {
            try {
                int rowIntValue = Integer.parseInt(rowValue);
                int compareValue = Integer.parseInt(value);
                return rowIntValue < compareValue;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
}
