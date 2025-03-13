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
                return createDatabase(dbName) ? "[OK] Database created" : "[ERROR] Database already exists";
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





            return "[ERROR] Unsupported command";
        } catch (IOException e) {
            return "[ERROR] " + e.getMessage();
        }
    }

    /**
     * 创建数据库（文件夹）
     */
    public boolean createDatabase(String dbName) {
        String dbPath = storageFolderPath + File.separator + dbName.toLowerCase();
        File dbFolder = new File(dbPath);
        return !dbFolder.exists() && dbFolder.mkdirs();
    }

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

        Set<String> uniqueColumns = new HashSet<>(columns);
        if (uniqueColumns.size() != columns.size()) {
            return false; // 列名重复
        }

        if (columns.contains("id")) {
            return false; // 不能包含 id 列
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile))) {
            writer.write("id"); // 第一列是 id
            for (String col : columns) {
                writer.write("\t" + col);
            }
            writer.newLine();
        }

        return true;
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



    /**
     * 插入数据
     */
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

        // 插入新数据
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile, true))) {
            // 生成 id 并写入
            int nextId = existingData.size(); // 这里可以根据你的需求生成 id
            writer.write(nextId + "\t" + String.join("\t", values));
            writer.newLine();
        }

        return true;
    }



/*
    // 删除记录时检查是否有外键引用
    public boolean deleteRecord(String tableName, String recordID) throws IOException {
        if (currentDatabase == null) {
            throw new IOException("No database selected.");
        }

        // 使用当前选中的数据库来构建文件路径
        String tablePath = storageFolderPath + File.separator + currentDatabase.toLowerCase() + File.separator + tableName.toLowerCase() + ".tab";
        File tableFile = new File(tablePath);

        if (!tableFile.exists()) {
            throw new IOException("Table does not exist: " + tableName);
        }

        // Check if the record can be deleted (check for foreign key constraints)
        if (!canDeleteRecord(tableName, recordID)) {
            return false;
        }

        // Delete the record from the table
        List<List<String>> tableData = loadTable( tableName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile))) {
            writer.write(tableData.get(0).get(0)); // Write the table header
            writer.newLine();
            for (int i = 1; i < tableData.size(); i++) {
                // Write all rows except the one to delete
                if (!tableData.get(i).get(0).equals(recordID)) {
                    writer.write(String.join("\t", tableData.get(i)));
                    writer.newLine();
                }
            }
        }

        return true;
    }

    private boolean canDeleteRecord(String tableName, String recordID) throws IOException {
        // 遍历所有表，检查是否有外键引用了该 recordID
        File folder = new File(storageFolderPath);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".tab"));

        if (files == null) return true;

        for (File file : files) {
            List<List<String>> data = loadTable("shop");
            for (List<String> row : data) {
                if (row.contains(recordID)) {
                    return false;
                }
            }
        }
        return true;
    }


*/
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
