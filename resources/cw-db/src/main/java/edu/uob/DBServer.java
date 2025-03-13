package edu.uob;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.*;
import java.io.FileWriter;
import java.io.FileReader;
import java.util.stream.Collectors;

/** This class implements the DB server. */
public class DBServer {

    private static final char END_OF_TRANSMISSION = 4;
    private String storageFolderPath;
    private String currentDatabase = null; // 存储当前选中的数据库


    public static void main(String args[]) throws IOException {
        DBServer server = new DBServer();
        server.blockingListenOn(8888);
    }

    /**
    * KEEP this signature otherwise we won't be able to mark your submission correctly.
    */
    public DBServer() {
        storageFolderPath = Paths.get("databases").toAbsolutePath().toString();
        try {
            // Create the database storage folder if it doesn't already exist !
            Files.createDirectories(Paths.get(storageFolderPath));
        } catch(IOException ioe) {
            System.out.println("Can't seem to create database storage folder " + storageFolderPath);
        }
    }

    /**
    * KEEP this signature (i.e. {@code edu.uob.DBServer.handleCommand(String)}) otherwise we won't be
    * able to mark your submission correctly.
    *
    * <p>This method handles all incoming DB commands and carries out the required actions.
    */
    public String handleCommand(String command) {
        // 去除前后空格并拆分命令
        String[] tokens = command.trim().split("\\s+");
        if (tokens.length < 2) {
            return "[ERROR] Invalid command";
        }

        String action = tokens[0].toUpperCase();

        try {
            // CREATE DATABASE <dbname>
            if ("CREATE".equals(action) && "DATABASE".equals(tokens[1].toUpperCase())) {
                if (tokens.length != 3) {
                    return "[ERROR] Invalid CREATE DATABASE syntax. Usage: CREATE DATABASE <dbname>";
                }
                String dbName = tokens[2];
                CreateDatabase createDatabase = new CreateDatabase(storageFolderPath);
                return createDatabase.createDatabase(dbName) ? "[OK] Database created" : "[ERROR] Database already exists";
            }
            // 假设已经在解析 SQL 语句的地方
            if ("DROP".equals(action) && "DATABASE".equals(tokens[1].toUpperCase())) {
                if (tokens.length != 3) {
                    return "[ERROR] Invalid DROP DATABASE syntax. Usage: DROP DATABASE <dbname>";
                }
                String dbName = tokens[2];
                DropDatabase dropDatabase = new DropDatabase(storageFolderPath);
                return dropDatabase.dropDatabase(dbName) ? "[OK] Database dropped" : "[ERROR] Database not found";
            }

            // USE <dbname>
            if ("USE".equals(action)) {
                if (tokens.length != 2) {
                    return "[ERROR] Invalid USE syntax. Usage: USE <dbname>";
                }
                currentDatabase = tokens[1];
                return "[OK] Database switched to " + currentDatabase;
            }
            // CREATE TABLE <table_name> (col1, col2, ...)
            if ("CREATE".equals(action) && "TABLE".equals(tokens[1].toUpperCase())) {
                if (tokens.length < 4) {
                    return "[ERROR] Invalid CREATE TABLE syntax. Usage: CREATE TABLE <table_name> (col1, col2, ...)";
                }
                String tableName = tokens[2];
                List<String> columns = Arrays.asList(tokens).subList(3, tokens.length);
                return createTable(tableName, columns) ? "[OK] Table created" : "[ERROR] Table already exists or invalid column names";
            }
            // 解析 DROP TABLE 语句
            if ("DROP".equals(action) && "TABLE".equals(tokens[1].toUpperCase())) {
                if (tokens.length != 3) {
                    return "[ERROR] Invalid DROP TABLE syntax. Usage: DROP TABLE <table_name>";
                }
                String tableName = tokens[2];
                return dropTable(tableName) ? "[OK] Table dropped" : "[ERROR] Table not found";
            }
            // ALTER TABLE <TableName> ADD <AttributeName>
            if ("ALTER".equals(action) && "TABLE".equals(tokens[1].toUpperCase())) {
                if (tokens.length != 5) {
                    return "[ERROR] Invalid ALTER TABLE syntax. Usage: ALTER TABLE <TableName> ADD/DROP <AttributeName>";
                }
                String tableName = tokens[2];
                String alterationType = tokens[3].toUpperCase(); // "ADD" 或 "DROP"
                String columnName = tokens[4];

                // 判断操作类型
                if ("ADD".equals(alterationType)) {
                    return alterTableAddColumn(tableName, columnName) ? "[OK] Column added" : "[ERROR] Column already exists or invalid table";
                } else if ("DROP".equals(alterationType)) {
                    return alterTableDropColumn(tableName, columnName) ? "[OK] Column dropped" : "[ERROR] Column does not exist or invalid table";
                } else {
                    return "[ERROR] Invalid alteration type. Usage: ADD|DROP";
                }
            }
            // INSERT INTO <table_name> VALUES (value1, value2, ...)
            if ("INSERT".equals(action)) {
                if (tokens.length < 4 || !"INTO".equals(tokens[1].toUpperCase())) {
                    return "[ERROR] Invalid INSERT syntax. Usage: INSERT INTO <table_name> VALUES (value1, value2, ...)";
                }
                String tableName = tokens[2];
                List<String> values = Arrays.asList(tokens).subList(4, tokens.length);
                return insertIntoTable(tableName, values) ? "[OK] Record inserted" : "[ERROR] Insert failed";
            }

            /*// DELETE FROM <table_name> WHERE id=<record_id>
            if ("DELETE".equals(action)) {
                if (tokens.length < 4 || !"FROM".equals(tokens[1].toUpperCase())) {
                    return "[ERROR] Invalid DELETE syntax. Usage: DELETE FROM <table_name> WHERE id=<record_id>";
                }
                String tableName = tokens[2];
                String recordID = tokens[3];
                return deleteRecord(tableName, recordID) ? "[OK] Record deleted" : "[ERROR] Delete failed or invalid record";
            }


             */
            // SELECT * FROM <table_name>
            if ("SELECT".equals(action)) {
                if (tokens.length >= 7 && "id".equalsIgnoreCase(tokens[1]) && "FROM".equalsIgnoreCase(tokens[2]) && "WHERE".equalsIgnoreCase(tokens[4])) {
                    // 解析 `SELECT id FROM <table> WHERE <column> == <value>`
                    String tableName = tokens[3];
                    String whereColumn = tokens[5];
                    String whereValue = tokens[7].replaceAll("'", ""); // 去掉引号

                    return selectIdFromTable(tableName, whereColumn, whereValue);
                }else if (tokens.length < 4 || !"FROM".equals(tokens[tokens.length - 2].toUpperCase())) {
                    return "[ERROR] Invalid SELECT syntax. Usage: SELECT <column1> <column2> FROM <table_name>";
                }

                String tableName = tokens[tokens.length - 1]; // 获取表名

                try {
                    // 使用 loadTable 方法加载整个表
                    List<List<String>> tableData = readTable(tableName);  // 使用 loadTable 方法

                    // 将查询结果转换为字符串
                    StringBuilder result = new StringBuilder();
                    for (List<String> row : tableData) {
                        result.append(String.join("\t", row));  // 使用制表符分隔每一列
                        result.append("\n");
                    }
                    return "[OK] " + result.toString();  // 返回所有行的内容
                } catch (IOException e) {
                    return "[ERROR] " + e.getMessage();  // 错误处理
                }
            }
//UPDATE
            if ("UPDATE".equals(action)) {

                String tableName = tokens[1];

                // 重新组合 SQL 语句
                String fullSQL = String.join(" ", tokens);

                // 提取 SET 和 WHERE 部分
                int setIndex = fullSQL.indexOf("SET") + 3;
                int whereIndex = fullSQL.indexOf("WHERE");

                if (setIndex == -1 || whereIndex == -1 || setIndex >= whereIndex) {
                    return "[ERROR] Invalid UPDATE syntax.";
                }

                String setPart = fullSQL.substring(setIndex, whereIndex).trim();
                String wherePart = fullSQL.substring(whereIndex + 5).trim();

                // 解析 SET 部分
                String[] setColumnsValues = setPart.split("\\s*,\\s*");  // 以逗号分隔多个 `column=value` 对
                Map<String, String> setValuesMap = new HashMap<>();

                for (String setColumnValue : setColumnsValues) {
                    String[] parts = setColumnValue.split("=", 2);  // 只拆分一次，确保值中带 `=` 不受影响
                    if (parts.length == 2) {
                        String column = parts[0].trim();
                        String value = parts[1].trim().replace("'", "");  // 去掉引号
                        setValuesMap.put(column, value);
                    } else {
                        return "[ERROR] Invalid SET syntax. Usage: SET <column1> = <value1>, <column2> = <value2> ...";
                    }
                }

                // 解析 WHERE 条件
                String[] whereParts = wherePart.split("==");
                if (whereParts.length != 2) {
                    return "[ERROR] Invalid WHERE syntax. Usage: WHERE <column> == <value>";
                }
                String whereColumn = whereParts[0].trim();
                String whereValue = whereParts[1].trim().replace("'", "");

                return updateTable(tableName, setValuesMap, whereColumn, whereValue);
            }

            //DELETE
            if ("DELETE".equals(action)) {

                String tableName = tokens[2];

                // 重新构造 SQL 语句，提取 WHERE 条件
                String fullSQL = String.join(" ", tokens);
                int whereIndex = fullSQL.indexOf("WHERE");

                if (whereIndex == -1) {
                    return "[ERROR] Missing WHERE clause.";
                }

                String wherePart = fullSQL.substring(whereIndex + 5).trim();  // 去掉 "WHERE "

                // 解析 WHERE 条件
                String[] whereParts = wherePart.split("==", 2);
                if (whereParts.length != 2) {
                    return "[ERROR] Invalid WHERE syntax. Usage: WHERE <column> == <value>";
                }

                String whereColumn = whereParts[0].trim();
                String whereValue = whereParts[1].trim().replace("'", "");  // 去掉引号

                return deleteFromTable(tableName, whereColumn, whereValue);
            }

            if ("JOIN".equalsIgnoreCase(action)) {

                // 提取表名和列名
                String table1 = tokens[1];
                String table2 = tokens[3];
                String[] column1Parts = tokens[5].split("\\.");
                String[] column2Parts = tokens[7].split("\\.");

                if (column1Parts.length != 2 || column2Parts.length != 2) {
                    return "[ERROR] Invalid column syntax. Use: <table1.column> AND <table2.column>";
                }

                // 确保列属于正确的表
                if (!column1Parts[0].equalsIgnoreCase(table1) || !column2Parts[0].equalsIgnoreCase(table2)) {
                    return "[ERROR] Columns must belong to the correct tables.";
                }

                String column1 = column1Parts[1];
                String column2 = column2Parts[1];

                return joinTables(table1, table2, column1, column2);
            }


            return "[ERROR] Unsupported command";
        } catch (IOException e) {
            return "[ERROR] " + e.getMessage();
        }
    }

    /**
     * 创建数据库（文件夹）
     */



    /**
     * 创建表（文件）
     */
    public boolean createTable(String tableName, List<String> columns) throws IOException {
        if (currentDatabase == null) {
            return false; // 没有选择数据库
        }

        String tablePath = storageFolderPath + File.separator + currentDatabase.toLowerCase() + File.separator + tableName.toLowerCase() + ".tab";
        File tableFile = new File(tablePath);

        if (tableFile.exists()) {
            return false;
        }

        List<String> cleanedColumns = new ArrayList<>();
        Set<String> uniqueColumns = new HashSet<>();

        for (String col : columns) {
            String cleanCol = col.replace("(", "").replace(")", "").replace(";", "").replace(",", "").trim(); // 清理括号和分号
            if (cleanCol.equalsIgnoreCase("id")) {
                return false; // 不能手动包含 id 列
            }
            if (!uniqueColumns.add(cleanCol)) {
                return false; // 列名重复
            }
            cleanedColumns.add(cleanCol);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile, StandardCharsets.UTF_8))) {
            writer.write("id"); // 第一列是 id
            for (String col : cleanedColumns) {
                writer.write("\t" + col); // 使用制表符分隔列名
            }
            writer.newLine();
        }

        return true;
    }

    public boolean dropTable(String tableName) {
        if (currentDatabase == null) {
            return false; // 没有选择数据库
        }

        String tablePath = storageFolderPath + File.separator + currentDatabase.toLowerCase() + File.separator + tableName.toLowerCase() + ".tab";
        File tableFile = new File(tablePath);

        // 检查表格是否存在
        if (!tableFile.exists()) {
            return false; // 表格不存在
        }

        // 删除表格文件
        return tableFile.delete(); // 删除文件，返回删除是否成功
    }
    /**
     * 读取表数据
     */
    public List<List<String>> readTable(String tableName) throws IOException {
        if (currentDatabase == null) {
            throw new IOException("No database selected.");
        }
        String cleanTableName = tableName.replace(";", "").trim();
        String tablePath = storageFolderPath + File.separator + currentDatabase.toLowerCase() + File.separator + cleanTableName.toLowerCase() + ".tab";
        File tableFile = new File(tablePath);
        System.out.println("readpath: " + tablePath);

        if (!tableFile.exists()) {
            throw new IOException("Table does not exist: " + tableName);
        }

        List<List<String>> tableData = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(tableFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                tableData.add(Arrays.asList(line.split("\t")));
            }
        }

        return tableData;
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

        List<List<String>> tableData = readTable(tableName);
        if (tableData.isEmpty()) {
            return "[ERROR] Table is empty";
        }
        System.out.println("column: " + whereColumn);
        System.out.println("value: " + cleanwhereValue);
        // 解析表头
        List<String> header = tableData.get(0);
        header = header.stream()
                .map(s -> s.replaceAll("[^a-zA-Z0-9_]", "")) // 删除任何非字母数字字符
                .collect(Collectors.toList());
        int idIndex = header.indexOf("id"); // `id` 列索引
        int whereIndex = header.indexOf(whereColumn); // `whereColumn` 的索引
        System.out.println("header: " + header);
        System.out.println("idindex: " + idIndex);
        System.out.println("nameindex: " + whereIndex);
        if (idIndex == -1) {
            return "[ERROR] Table does not contain 'id' column.";
        }
        if (whereIndex == -1) {
            return "[ERROR] Column not found: " + whereColumn;
        }

        // 遍历表数据，查找匹配 WHERE 条件的行
        for (int i = 1; i < tableData.size(); i++) {
            List<String> row = tableData.get(i);

            // 清理数据中的引号
            String cleanRowValue = row.get(whereIndex)
                    .replace("'", "")
                    .replace(",", "")
                    .replace(";", "")
                    .replace("(", "")
                    .replace(")", "")
                    .trim();
            System.out.println("cleanrowvalue: " + cleanRowValue);
            // 比较时用干净的值
            if (cleanRowValue.equals(cleanwhereValue)) {
                return "[OK] " + row.get(idIndex); // 返回 id
            }
        }
        return "[ERROR] No matching record found.";
    }
    public boolean alterTableAddColumn(String tableName, String columnName) {
        if (currentDatabase == null) {
            return false; // 没有选择数据库
        }

        String tablePath = storageFolderPath + File.separator + currentDatabase.toLowerCase() + File.separator + tableName.toLowerCase() + ".tab";
        File tableFile = new File(tablePath);

        if (!tableFile.exists()) {
            return false; // 表不存在
        }

        // 读取当前表格内容
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

        // 检查列名是否已经存在
        String firstLine = lines.get(0); // 第一行是列名
        String[] existingColumns = firstLine.split("\t");

        for (String existingColumn : existingColumns) {
            if (existingColumn.trim().equalsIgnoreCase(columnName)) {
                return false; // 列已经存在
            }
        }

        // 将新的列添加到表格的第一行和数据行中
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile, StandardCharsets.UTF_8))) {
            // 添加新的列名
            writer.write(firstLine + "\t" + columnName);
            writer.newLine();

            // 添加数据行
            for (int i = 1; i < lines.size(); i++) {
                writer.write(lines.get(i) + "\t"); // 在每一行的数据末尾添加一个新的空值
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
            return false; // 没有选择数据库
        }

        String tablePath = storageFolderPath + File.separator + currentDatabase.toLowerCase() + File.separator + tableName.toLowerCase() + ".tab";
        File tableFile = new File(tablePath);

        if (!tableFile.exists()) {
            return false; // 表不存在
        }

        // 读取当前表格内容
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

        // 检查列名是否存在
        String firstLine = lines.get(0); // 第一行是列名
        String[] existingColumns = firstLine.split("\t");

        int columnIndex = -1;
        for (int i = 0; i < existingColumns.length; i++) {
            if (existingColumns[i].trim().equalsIgnoreCase(columnName)) {
                columnIndex = i;
                break;
            }
        }

        if (columnIndex == -1) {
            return false; // 列不存在
        }

        // 移除该列
        List<String> updatedLines = new ArrayList<>();
        for (String line : lines) {
            String[] columns = line.split("\t");
            List<String> updatedColumns = new ArrayList<>(Arrays.asList(columns));
            updatedColumns.remove(columnIndex); // 删除指定的列
            updatedLines.add(String.join("\t", updatedColumns));
        }

        // 将更新后的内容写回文件
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
            return false; // 没有选择数据库
        }

        String tablePath = storageFolderPath + File.separator + currentDatabase.toLowerCase() + File.separator + tableName.toLowerCase() + ".tab";
        File tableFile = new File(tablePath);
        System.out.println("insertpath: " + tablePath);
        if (!tableFile.exists()) {
            return false; // 表格文件不存在
        }

        // 获取当前表格的内容
        List<String> existingData = Files.readAllLines(tableFile.toPath(), StandardCharsets.UTF_8);
        // 这里可以检查列是否匹配
        // 你需要确保 "id" 列是唯一的并且数据插入正确

        // 获取表头列数
        String[] headerColumns = existingData.get(0).split("\t");
        int columnCount = headerColumns.length - 1; // 除去 `id` 列

        // 处理 `values`，去掉括号、单引号、多余符号
        List<String> cleanedValues = new ArrayList<>();
        for (String value : values) {
            String cleanValue = value.replace("(", "") // 去掉左括号
                    .replace(")", "") // 去掉右括号
                    .replace(";", "") // 去掉分号
                    .replace("'", "")
                    .replace(",", "") // 去掉单引号
                    .trim();
            cleanedValues.add(cleanValue);
        }

        // 检查插入值的个数是否匹配表头
        if (cleanedValues.size() != columnCount) {
            return false; // 数据列数不匹配
        }

        // 计算新的 `id`（保证唯一）
        int nextId = existingData.size(); // id = 当前行数（第一行是表头）

        // 插入新数据
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

        List<List<String>> tableData = readTable(tableName);
        if (tableData.isEmpty()) {
            return "[ERROR] Table is empty.";
        }

        // 获取表头
        List<String> header = tableData.get(0);
        int whereIndex = header.indexOf(whereColumn);
        if (whereIndex == -1) {
            return "[ERROR] Column not found: " + whereColumn;
        }

        for (String col : setValues.keySet()) {
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

        // 重新写入文件
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

        List<List<String>> tableData = readTable(tableName);
        if (tableData.isEmpty()) {
            return "[ERROR] Table is empty.";
        }

        // 获取表头
        List<String> header = tableData.get(0);
        int whereIndex = header.indexOf(whereColumn);

        if (whereIndex == -1) {
            return "[ERROR] Column not found: " + whereColumn;
        }

        // 过滤出需要保留的数据
        List<List<String>> updatedTableData = new ArrayList<>();
        updatedTableData.add(header);  // 保留表头

        boolean rowDeleted = false;

        for (int i = 1; i < tableData.size(); i++) {
            List<String> row = tableData.get(i);
            String rowValue = row.get(whereIndex).replace("'", "").trim();

            if (!rowValue.equals(whereValue)) {
                updatedTableData.add(row);  // 仅保留未匹配的行
            } else {
                rowDeleted = true;
            }
        }

        if (!rowDeleted) {
            return "[ERROR] No matching record found.";
        }

        // 覆盖写回文件
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

        // 读取两个表的数据
        List<List<String>> table1Data = readTable(table1);
        List<List<String>> table2Data = readTable(table2);

        if (table1Data.isEmpty() || table2Data.isEmpty()) {
            return "[ERROR] One or both tables are empty.";
        }

        // 解析表头
        List<String> header1 = table1Data.get(0);
        List<String> header2 = table2Data.get(0);

        System.out.println("[DEBUG] Table 1 Header: " + header1);
        System.out.println("[DEBUG] Table 2 Header: " + header2);

        // 获取 JOIN 列索引
        int index1 = header1.indexOf(column1);
        int index2 = header2.indexOf(column2);

        if (index1 == -1) return "[ERROR] Column not found in " + table1 + ": " + column1;
        if (index2 == -1) return "[ERROR] Column not found in " + table2 + ": " + column2;

        System.out.println("[DEBUG] JOIN ON " + table1 + "." + column1 + " (Index: " + index1 + ") AND " + table2 + "." + column2 + " (Index: " + index2 + ")");

        // 生成 JOIN 结果表头
        List<String> joinedHeader = new ArrayList<>(header1);
        joinedHeader.addAll(header2);

        // 进行 INNER JOIN
        List<List<String>> joinResult = new ArrayList<>();
        joinResult.add(joinedHeader); // 先添加表头

        for (int i = 1; i < table1Data.size(); i++) {
            List<String> row1 = table1Data.get(i);
            String key1 = row1.get(index1).trim();

            for (int j = 1; j < table2Data.size(); j++) {
                List<String> row2 = table2Data.get(j);
                String key2 = row2.get(index2).trim();

                if (key1.equals(key2)) { // INNER JOIN 匹配
                    List<String> joinedRow = new ArrayList<>(row1);
                    joinedRow.addAll(row2);
                    joinResult.add(joinedRow);
                    System.out.println("[DEBUG] MATCH FOUND: " + row1 + " <-> " + row2);
                }
            }
        }

        // 如果没有匹配数据
        if (joinResult.size() == 1) {
            return "[ERROR] No matching records found.";
        }

        // 格式化 JOIN 结果输出
        StringBuilder result = new StringBuilder();
        for (List<String> row : joinResult) {
            result.append(String.join("\t", row)).append("\n");
        }

        return "[OK]\n" + result.toString();
    }




    //  === Methods below handle networking aspects of the project - you will not need to change these ! ===

    public void blockingListenOn(int portNumber) throws IOException {
        try (ServerSocket s = new ServerSocket(portNumber)) {
            System.out.println("Server listening on port " + portNumber);
            while (!Thread.interrupted()) {
                try {
                    blockingHandleConnection(s);
                } catch (IOException e) {
                    System.err.println("Server encountered a non-fatal IO error:");
                    e.printStackTrace();
                    System.err.println("Continuing...");
                }
            }
        }
    }

    private void blockingHandleConnection(ServerSocket serverSocket) throws IOException {
        try (Socket s = serverSocket.accept();
        BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {

            System.out.println("Connection established: " + serverSocket.getInetAddress());
            while (!Thread.interrupted()) {
                String incomingCommand = reader.readLine();
                System.out.println("Received message: " + incomingCommand);
                String result = handleCommand(incomingCommand);
                writer.write(result);
                writer.write("\n" + END_OF_TRANSMISSION + "\n");
                writer.flush();
            }
        }
    }
}