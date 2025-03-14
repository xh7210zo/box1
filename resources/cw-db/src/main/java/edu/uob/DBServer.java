package edu.uob;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.*;

/** This class implements the DB server. */
public class DBServer {

    private static final char END_OF_TRANSMISSION = 4;
    private String storageFolderPath;
    private String currentDatabase = null;


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
        String[] tokens = command.trim().split("\\s+");
        if (tokens.length < 2) {
            return "[ERROR] Invalid command";
        }

        String action = tokens[0].toUpperCase();

        try {
            // CREATE DATABASE
            if ("CREATE".equals(action) && "DATABASE".equals(tokens[1].toUpperCase())) {
                if (!tokens[tokens.length - 1].endsWith(";")) {
                    return "[ERROR] Semi-colon missing at end of line. Each statement must end with a semi-colon.";
                }
                if (tokens.length != 3) {
                    return "[ERROR] Invalid CREATE DATABASE syntax. Usage: CREATE DATABASE <dbname>";
                }
                String dbName = tokens[2].replace(";", "");;
                Database createDatabase = new Database(storageFolderPath);
                return createDatabase.createDatabase(dbName) ? "[OK] Database created" : "[ERROR] Database already exists";
            }

            // DROP DATABASE
            if ("DROP".equals(action) && "DATABASE".equals(tokens[1].toUpperCase())) {
                if (!tokens[tokens.length - 1].endsWith(";")) {
                    return "[ERROR] Semi-colon missing at end of line. Each statement must end with a semi-colon.";
                }
                if (tokens.length != 3) {
                    return "[ERROR] Invalid DROP DATABASE syntax. Usage: DROP DATABASE <dbname>";
                }
                String dbName = tokens[2].replace(";", "");
                Database dropDatabase = new Database(storageFolderPath);
                return dropDatabase.dropDatabase(dbName) ? "[OK] Database dropped" : "[ERROR] Database not found";
            }

            // USE
            if ("USE".equals(action)) {
                if (!tokens[tokens.length - 1].endsWith(";")) {
                    return "[ERROR] Semi-colon missing at end of line. Each statement must end with a semi-colon.";
                }
                if (tokens.length != 2) {
                    return "[ERROR] Invalid USE syntax. Usage: USE <dbname>";
                }
                currentDatabase = tokens[1].replace(";", "");
                return "[OK] Database switched to " + currentDatabase;
            }

            // CREATE TABLE <table_name> (col1, col2, ...)
            if ("CREATE".equals(action) && "TABLE".equals(tokens[1].toUpperCase())) {
                if (!tokens[tokens.length - 1].endsWith(";")) {
                    return "[ERROR] Semi-colon missing at end of line. Each statement must end with a semi-colon.";
                }
                if (tokens.length < 4) {
                    return "[ERROR] Invalid CREATE TABLE syntax. Usage: CREATE TABLE <table_name> (col1, col2, ...)";
                }
                String tableName = tokens[2];
                List<String> columns = Arrays.asList(tokens).subList(3, tokens.length);
                Table createTableHandler = new Table(storageFolderPath);
                createTableHandler.setCurrentDatabase(currentDatabase);
                return createTableHandler.createTable(tableName, columns) ? "[OK] Table created" : "[ERROR] Table already exists or invalid column names";
            }

            // DROP TABLE
            if ("DROP".equals(action) && "TABLE".equals(tokens[1].toUpperCase())) {
                if (!tokens[tokens.length - 1].endsWith(";")) {
                    return "[ERROR] Semi-colon missing at end of line. Each statement must end with a semi-colon.";
                }
                if (tokens.length != 3) {
                    return "[ERROR] Invalid DROP TABLE syntax. Usage: DROP TABLE <table_name>";
                }
                String tableName = tokens[2];
                Table dropTableHandler = new Table(storageFolderPath);
                dropTableHandler.setCurrentDatabase(currentDatabase);
                return dropTableHandler.dropTable(tableName) ? "[OK] Table dropped" : "[ERROR] Table not found";
            }

            // ALTER TABLE <TableName> ADD <AttributeName>
            if ("ALTER".equals(action) && "TABLE".equals(tokens[1].toUpperCase())) {
                if (!tokens[tokens.length - 1].endsWith(";")) {
                    return "[ERROR] Semi-colon missing at end of line. Each statement must end with a semi-colon.";
                }
                if (tokens.length != 5) {
                    return "[ERROR] Invalid ALTER TABLE syntax. Usage: ALTER TABLE <TableName> ADD/DROP <AttributeName>";
                }
                String tableName = tokens[2];
                String alterationType = tokens[3].toUpperCase();
                String columnName = tokens[4].replace(";","");

                TableChanger alterTableHandler = new TableChanger(storageFolderPath, currentDatabase);
                if ("ADD".equals(alterationType)) {
                    return alterTableHandler.alterTableAddColumn(tableName, columnName) ? "[OK] Column added" : "[ERROR] Column already exists or invalid table";
                } else if ("DROP".equals(alterationType)) {
                    return alterTableHandler.alterTableDropColumn(tableName, columnName) ? "[OK] Column dropped" : "[ERROR] Column does not exist or invalid table";
                } else {
                    return "[ERROR] Invalid alteration type. Usage: ADD|DROP";
                }
            }

            // INSERT INTO <table_name> VALUES (value1, value2, ...)
            if ("INSERT".equals(action)) {
                if (!tokens[tokens.length - 1].endsWith(";")) {
                    return "[ERROR] Semi-colon missing at end of line. Each statement must end with a semi-colon.";
                }
                if (tokens.length < 4 || !"INTO".equals(tokens[1].toUpperCase())) {
                    return "[ERROR] Invalid INSERT syntax. Usage: INSERT INTO <table_name> VALUES (value1, value2, ...)";
                }
                String tableName = tokens[2];
                TableChanger insertTableHandler = new TableChanger(storageFolderPath, currentDatabase);

                List<String> values = Arrays.asList(tokens).subList(4, tokens.length);

                return insertTableHandler.insertIntoTable(tableName, values) ? "[OK] Record inserted" : "[ERROR] Invalid number of values for the table";
            }

            // 查询语句处理
            if ("SELECT".equals(action)) {
                System.out.println("Action: " + action); // 打印查询类型，检查是否正确
                System.out.println("Tokens Length: " + tokens.length); // 打印 tokens 数量
                if (!tokens[tokens.length - 1].endsWith(";")) {
                    return "[ERROR] Semi-colon missing at end of line. Each statement must end with a semi-colon.";
                }
                if (tokens.length == 4 && "*".equals(tokens[1]) && "FROM".equalsIgnoreCase(tokens[2])) {
                    String tableName = tokens[3];
                    System.out.println("Simple SELECT query detected, tableName: " + tableName); // 打印表名

                    try {
                        TableReader tableReader = new TableReader(storageFolderPath, currentDatabase);
                        List<List<String>> tableData = tableReader.readTable(tableName);

                        if (tableData.isEmpty()) {
                            return "[ERROR] Table is empty";
                        }

                        StringBuilder result = new StringBuilder();
                        for (List<String> row : tableData) {
                            result.append(String.join("\t", row));
                            result.append("\n");
                        }
                        return "[OK]\n" + result.toString();
                    } catch (IOException e) {
                        return "[ERROR] " + e.getMessage();
                    }
                }
                // 确保 'FROM' 在倒数第二的位置
                if ( "FROM".equalsIgnoreCase(tokens[2])) {
                    if (!tokens[tokens.length - 1].endsWith(";")) {
                        return "[ERROR] Semi-colon missing at end of line. Each statement must end with a semi-colon.";
                    }
                    System.out.println("Valid SELECT query detected"); // 打印有效查询信息

                    String wildAttribList = tokens[1]; // WildAttribList 可以是 "*" 或列名的逗号分隔列表
                    System.out.println("WildAttribList: " + wildAttribList); // 打印选择的列

                    String tableName = tokens[3]; // 表名
                    System.out.println("TableName: " + tableName); // 打印表名

                    String wherePart = null;
                    if ( "WHERE".equalsIgnoreCase(tokens[4])) {
                        // 从第5个元素到最后一个元素合并为 WHERE 子句
                        wherePart = String.join(" ", Arrays.copyOfRange(tokens, 5, tokens.length));
                        System.out.println("wherePart before cleanup: " + wherePart); // 打印 WHERE 子句原始内容

                        // 清理掉可能的分号
                        wherePart = wherePart.replace(";", "");
                        wherePart = wherePart.replaceAll("(?<=\\w)([><=!]+)(?=\\d)", " $1 ");  // 对数字条件添加空格
                        wherePart = wherePart.replaceAll("(?<=\\d)([><=!]+)(?=\\w)", " $1 ");  // 对数字条件添加空格
                        wherePart = wherePart.replaceAll("([><=!]+)(?=\\w)", " $1 "); // 对标识符前添加空格
                        wherePart = wherePart.replaceAll("(?<=\\w)([><=!]+)", " $1 "); // 对标识符后添加空格
                        wherePart = wherePart.trim();
                        System.out.println("wherePart after cleanup: " + wherePart); // 打印清理后的 WHERE 子句
                    }

                    TableQuery tableQuery = new TableQuery(storageFolderPath, currentDatabase);
                    return tableQuery.selectFromTable(tableName, wildAttribList, wherePart);
                } else {
                    System.out.println("Invalid SELECT syntax: " + Arrays.toString(tokens)); // 打印错误信息和 tokens 数组
                    return "[ERROR] Invalid SELECT syntax. Usage: SELECT <column1> <column2> FROM <table_name>";
                }
            }


            // UPDATE
            if ("UPDATE".equals(action)) {
                if (!tokens[tokens.length - 1].endsWith(";")) {
                    return "[ERROR] Semi-colon missing at end of line. Each statement must end with a semi-colon.";
                }
                String tableName = tokens[1];

                String fullSQL = String.join(" ", tokens);

                int setIndex = fullSQL.indexOf("SET") + 3;
                int whereIndex = fullSQL.indexOf("WHERE");

                if (setIndex == -1 || whereIndex == -1 || setIndex >= whereIndex) {
                    return "[ERROR] Invalid UPDATE syntax.";
                }

                String setPart = fullSQL.substring(setIndex, whereIndex).trim();
                String wherePart = fullSQL.substring(whereIndex + 5).trim();

                String[] setColumnsValues = setPart.split("\\s*,\\s*");
                Map<String, String> setValuesMap = new HashMap<>();

                for (String setColumnValue : setColumnsValues) {
                    String[] parts = setColumnValue.split("=", 2);
                    if (parts.length == 2) {
                        String column = parts[0].trim();
                        String value = parts[1].trim().replace("'", "");
                        if ("id".equalsIgnoreCase(column)) {
                            return "[ERROR] Cannot update the ID column.";
                        }
                        setValuesMap.put(column, value);
                    } else {
                        return "[ERROR] Invalid SET syntax. Usage: SET <column1> = <value1>, <column2> = <value2> ...";
                    }
                }

                String[] whereParts = wherePart.split("==");
                if (whereParts.length != 2) {
                    return "[ERROR] Invalid WHERE syntax. Usage: WHERE <column> == <value>";
                }
                String whereColumn = whereParts[0].trim();
                String whereValue = whereParts[1].trim().replace("'", "").replace(";", "");
                whereValue = whereValue.replaceAll(";$", "").trim(); // 移除末尾的分号
                TableChanger updateTableHandler = new TableChanger(storageFolderPath, currentDatabase);
                return updateTableHandler.updateTable(tableName, setValuesMap, whereColumn, whereValue);
            }

            // DELETE 解析逻辑
            if ("DELETE".equals(action)) {
                if (!tokens[tokens.length - 1].endsWith(";")) {
                    return "[ERROR] Semi-colon missing at end of line. Each statement must end with a semi-colon.";
                }
                String tableName = tokens[2];

                String fullSQL = String.join(" ", tokens);
                int whereIndex = fullSQL.indexOf("WHERE");

                if (whereIndex == -1) {
                    return "[ERROR] Missing WHERE clause.";
                }

                String wherePart = fullSQL.substring(whereIndex + 5).trim();
                wherePart = wherePart.replace(";", "");
                wherePart = wherePart.replaceAll("(?<=\\w)([><=!]+)(?=\\d)", " $1 ");  // 对数字条件添加空格
                wherePart = wherePart.replaceAll("(?<=\\d)([><=!]+)(?=\\w)", " $1 ");  // 对数字条件添加空格
                wherePart = wherePart.replaceAll("([><=!]+)(?=\\w)", " $1 "); // 对标识符前添加空格
                wherePart = wherePart.replaceAll("(?<=\\w)([><=!]+)", " $1 "); // 对标识符后添加空格
                wherePart = wherePart.trim();

                if (wherePart.toLowerCase().contains("id")) {
                    return "[ERROR] Cannot delete from the ID column.";
                }

                // 解析 WHERE 子句中的运算符
                String[] operators = {"==", "!=", ">=", "<=", ">", "<"};
                String whereColumn = null, whereValue = null, operator = null;

                for (String op : operators) {
                    if (wherePart.contains(op)) {
                        String[] whereParts = wherePart.split(op, 2);
                        if (whereParts.length == 2) {
                            whereColumn = whereParts[0].trim();
                            whereValue = whereParts[1].trim().replace("'", "").replace(";", "");
                            operator = op;
                            break;
                        }
                    }
                }

                if (whereColumn == null || whereValue == null || operator == null) {
                    return "[ERROR] Invalid WHERE syntax. Usage: WHERE <column> <operator> <value>";
                }

                System.out.println("DELETE WHERE: " + whereColumn + " " + operator + " " + whereValue);

                TableChanger deleteTableHandler = new TableChanger(storageFolderPath, currentDatabase);
                return deleteTableHandler.deleteFromTable(tableName, whereColumn, whereValue, operator);
            }

            // JOIN
            if ("JOIN".equalsIgnoreCase(action)) {
                if (!tokens[tokens.length - 1].endsWith(";")) {
                    return "[ERROR] Semi-colon missing at end of line. Each statement must end with a semi-colon.";
                }
                String table1 = tokens[1];  // 第一个表
                String table2 = tokens[3];  // 第二个表
                String column1 = tokens[5]; // 第一个表的列（没有"table.column"格式）
                String column2 = tokens[7].replace(";", ""); // 第二个表的列（去掉分号）

                // 调试信息：打印表名和列名
                System.out.println("Action: JOIN");
                System.out.println("Table 1: " + table1);
                System.out.println("Table 2: " + table2);
                System.out.println("Column 1: " + column1);
                System.out.println("Column 2: " + column2);

                // 获取表的数据
                TableReader tableReader = new TableReader(storageFolderPath, currentDatabase);
                List<List<String>> table1Data = tableReader.readTable(table1);
                List<List<String>> table2Data = tableReader.readTable(table2);

                if (table1Data.isEmpty() || table2Data.isEmpty()) {
                    return "[ERROR] One or both tables are empty.";
                }

                List<String> header1 = table1Data.get(0); // 表1的表头
                List<String> header2 = table2Data.get(0); // 表2的表头

                // 检查列名是否在表头中存在
                if (!header1.contains(column1)) {
                    return "[ERROR] Column '" + column1 + "' not found in " + table1;
                }
                if (!header2.contains(column2)) {
                    return "[ERROR] Column '" + column2 + "' not found in " + table2;
                }

                // 调用 joinTables 方法处理 JOIN 逻辑
                TableChanger joinTableHandler = new TableChanger(storageFolderPath, currentDatabase);
                return joinTableHandler.joinTables(table1, table2, column1, column2);
            }

            return "[ERROR] Unsupported command";
        } catch (IOException e) {
            return "[ERROR] " + e.getMessage();
        }
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