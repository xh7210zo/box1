package edu.uob;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TableQuery {

    private String storageFolderPath;
    private String currentDatabase;

    public TableQuery(String storageFolderPath, String currentDatabase) {
        this.storageFolderPath = storageFolderPath;
        this.currentDatabase = currentDatabase;
    }

    public String selectFromTable(String tableName, String wildAttribList, String whereClause) throws IOException {
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

        // 获取表头
        List<String> header = tableData.get(0);

        // 解析查询中的通配符 (*) 和指定列
        List<String> selectedColumns;
        if ("*".equals(wildAttribList)) {
            selectedColumns = header; // 选择所有列
        } else {
            selectedColumns = Arrays.asList(wildAttribList.split(","));
        }

        // 检查所请求的列是否在表头中
        for (String column : selectedColumns) {
            if (!header.contains(column)) {
                return "[ERROR] Attribute does not exist: " + column;
            }
        }

        // 解析 WHERE 子句并处理条件
        List<List<String>> resultData = new ArrayList<>();
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            String[] conditions = whereClause.split("AND|OR");
            List<String> andConditions = new ArrayList<>();
            List<String> orConditions = new ArrayList<>();

            // 将条件按 AND 和 OR 分割并分类
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

            // 处理每一行数据，检查是否满足所有条件
            for (List<String> row : tableData.subList(1, tableData.size())) {
                boolean conditionMatched = evaluateConditions(andConditions, orConditions, row, header);

                if (conditionMatched) {
                    resultData.add(row);  // 如果条件匹配，则加入结果
                }
            }
        } else {
            resultData = tableData.subList(1, tableData.size()); // 如果没有 WHERE 子句，返回所有数据
        }

        // 输出查询结果
        StringBuilder result = new StringBuilder();
        result.append(String.join("\t", selectedColumns)).append("\n"); // 打印头部

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

        return "[OK]\n" + result.toString();
    }

    private boolean evaluateConditions(List<String> andConditions, List<String> orConditions, List<String> row, List<String> header) {
        boolean conditionMatched = true; // 默认情况是条件匹配，只有条件不匹配才会更新为 false

        // 处理 "AND" 条件
        for (String condition : andConditions) {
            boolean result = evaluateCondition(condition, row, header);
            System.out.println("[DEBUG] Evaluating AND condition: " + condition + " result: " + result);
            if (!result) {
                conditionMatched = false; // 如果有任何 AND 条件不匹配，返回 false
                break;
            }
        }

        // 处理 "OR" 条件
        for (String condition : orConditions) {
            boolean result = evaluateCondition(condition, row, header);
            System.out.println("[DEBUG] Evaluating OR condition: " + condition + " result: " + result);
            if (result) {
                return true; // 如果有任何 OR 条件匹配，直接返回 true
            }
        }

        return conditionMatched; // 如果没有匹配的 OR 条件，返回 AND 条件的匹配结果
    }

    private boolean evaluateCondition(String condition, List<String> row, List<String> header) {
        // 解析单个条件
        String[] parts = condition.split("==|!=|>|<|LIKE");
        if (parts.length != 2) {
            System.out.println("[ERROR] Invalid condition: " + condition);
            return false;
        }

        String column = parts[0].trim().replace("(", "");
        String value = parts[1].trim().replace("'", "").replace(")", "");

        int colIndex = header.indexOf(column);
        if (colIndex == -1) {
            System.out.println("[ERROR] Column not found: " + column);
            return false; // Column not found
        }

        String rowValue = row.get(colIndex).trim().replace("'", "");
        return evaluateCondition(column, condition, value, rowValue);
    }

    private boolean evaluateCondition(String column, String condition, String value, String rowValue) {
        // 打印调试信息，查看比较的具体内容
        System.out.println("[DEBUG] Evaluating condition: " + column + " " + condition + " with rowValue: '" + rowValue + "' and value: '" + value + "'");

        // 去除前后空格并转化为小写进行比较
        value = value.trim().toLowerCase();
        rowValue = rowValue.trim().toLowerCase();

        // 处理 "LIKE" 条件
        if (condition.contains("LIKE")) {
            System.out.println("[DEBUG] Performing LIKE check: '" + rowValue + "' LIKE '" + value + "'");
            // 使用 contains 方法检查是否包含指定的子字符串
            return rowValue.contains(value);
        }

        // 如果是等于比较
        if (condition.contains("==")) {
            return rowValue.equals(value);
        }
        // 如果是不同于比较
        else if (condition.contains("!=")) {
            return !rowValue.equals(value);
        }
        // 如果是大于比较
        else if (condition.contains(">")) {
            try {
                int rowIntValue = Integer.parseInt(rowValue);
                int compareValue = Integer.parseInt(value);
                return rowIntValue > compareValue;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        // 如果是小于比较
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
