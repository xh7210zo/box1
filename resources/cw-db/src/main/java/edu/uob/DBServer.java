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
                if (tokens.length != 3) {
                    return "[ERROR] Invalid CREATE DATABASE syntax. Usage: CREATE DATABASE <dbname>";
                }
                String dbName = tokens[2];
                CreateDatabase createDatabase = new CreateDatabase(storageFolderPath);
                return createDatabase.createDatabase(dbName) ? "[OK] Database created" : "[ERROR] Database already exists";
            }
            // DROP DATABASE
            if ("DROP".equals(action) && "DATABASE".equals(tokens[1].toUpperCase())) {
                if (tokens.length != 3) {
                    return "[ERROR] Invalid DROP DATABASE syntax. Usage: DROP DATABASE <dbname>";
                }
                String dbName = tokens[2];
                DropDatabase dropDatabase = new DropDatabase(storageFolderPath);
                return dropDatabase.dropDatabase(dbName) ? "[OK] Database dropped" : "[ERROR] Database not found";
            }

            // USE
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
            // DROP TABLE
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
                String alterationType = tokens[3].toUpperCase();
                String columnName = tokens[4];

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
            // SELECT * FROM <table_name>
            if ("SELECT".equals(action)) {
                if (tokens.length >= 7 && "id".equalsIgnoreCase(tokens[1]) && "FROM".equalsIgnoreCase(tokens[2]) && "WHERE".equalsIgnoreCase(tokens[4])) {
                    String tableName = tokens[3];
                    String whereColumn = tokens[5];
                    String whereValue = tokens[7].replaceAll("'", "");

                    return selectIdFromTable(tableName, whereColumn, whereValue);
                }else if (tokens.length < 4 || !"FROM".equals(tokens[tokens.length - 2].toUpperCase())) {
                    return "[ERROR] Invalid SELECT syntax. Usage: SELECT <column1> <column2> FROM <table_name>";
                }

                String tableName = tokens[tokens.length - 1];

                try {
                    List<List<String>> tableData = readTable(tableName);

                    StringBuilder result = new StringBuilder();
                    for (List<String> row : tableData) {
                        result.append(String.join("\t", row));
                        result.append("\n");
                    }
                    return "[OK] " + result.toString();
                } catch (IOException e) {
                    return "[ERROR] " + e.getMessage();
                }
            }
            //UPDATE
            if ("UPDATE".equals(action)) {

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
                String whereValue = whereParts[1].trim().replace("'", "");

                return updateTable(tableName, setValuesMap, whereColumn, whereValue);
            }
            //DELETE
            if ("DELETE".equals(action)) {

                String tableName = tokens[2];

                String fullSQL = String.join(" ", tokens);
                int whereIndex = fullSQL.indexOf("WHERE");

                if (whereIndex == -1) {
                    return "[ERROR] Missing WHERE clause.";
                }

                String wherePart = fullSQL.substring(whereIndex + 5).trim();

                String[] whereParts = wherePart.split("==", 2);
                if (whereParts.length != 2) {
                    return "[ERROR] Invalid WHERE syntax. Usage: WHERE <column> == <value>";
                }

                String whereColumn = whereParts[0].trim();
                String whereValue = whereParts[1].trim().replace("'", "");

                return deleteFromTable(tableName, whereColumn, whereValue);
            }
            //JOIN
            if ("JOIN".equalsIgnoreCase(action)) {

                String table1 = tokens[1];
                String table2 = tokens[3];
                String[] column1Parts = tokens[5].split("\\.");
                String[] column2Parts = tokens[7].split("\\.");

                if (column1Parts.length != 2 || column2Parts.length != 2) {
                    return "[ERROR] Invalid column syntax. Use: <table1.column> AND <table2.column>";
                }

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

    public boolean createTable(String tableName, List<String> columns) throws IOException {
        if (currentDatabase == null) {
            return false;
        }

        String tablePath = storageFolderPath + File.separator + currentDatabase.toLowerCase() + File.separator + tableName.toLowerCase() + ".tab";
        File tableFile = new File(tablePath);

        if (tableFile.exists()) {
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
                return false;
            }
            if (!uniqueColumns.add(cleanCol)) {
                return false;
            }
            cleanedColumns.add(cleanCol);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile, StandardCharsets.UTF_8))) {
            writer.write("id");
            for (String col : cleanedColumns) {
                writer.write("\t" + col);
            }
            writer.newLine();
        }

        return true;
    }

    public boolean dropTable(String tableName) {
        if (currentDatabase == null) {
            return false;
        }

        String tablePath = storageFolderPath + File.separator + currentDatabase.toLowerCase() + File.separator + tableName.toLowerCase() + ".tab";
        File tableFile = new File(tablePath);

        if (!tableFile.exists()) {
            return false;
        }

        return tableFile.delete();
    }

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

        List<String> header = tableData.get(0);
        header = header.stream()
                .map(s -> s.replaceAll("[^a-zA-Z0-9_]", ""))
                .collect(Collectors.toList());
        int idIndex = header.indexOf("id");
        int whereIndex = header.indexOf(whereColumn);
        System.out.println("header: " + header);
        System.out.println("idindex: " + idIndex);
        System.out.println("nameindex: " + whereIndex);
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
            System.out.println("cleanrowvalue: " + cleanRowValue);

            if (cleanRowValue.equals(cleanwhereValue)) {
                return "[OK] " + row.get(idIndex);
            }
        }
        return "[ERROR] No matching record found.";
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
        System.out.println("insertpath: " + tablePath);
        if (!tableFile.exists()) {
            return false;
        }

        List<String> existingData = Files.readAllLines(tableFile.toPath(), StandardCharsets.UTF_8);

        String[] headerColumns = existingData.get(0).split("\t");
        int columnCount = headerColumns.length - 1; // 除去 `id` 列

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

        List<List<String>> tableData = readTable(tableName);
        if (tableData.isEmpty()) {
            return "[ERROR] Table is empty.";
        }

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

        List<List<String>> table1Data = readTable(table1);
        List<List<String>> table2Data = readTable(table2);

        if (table1Data.isEmpty() || table2Data.isEmpty()) {
            return "[ERROR] One or both tables are empty.";
        }

        List<String> header1 = table1Data.get(0);
        List<String> header2 = table2Data.get(0);

        System.out.println("[DEBUG] Table 1 Header: " + header1);
        System.out.println("[DEBUG] Table 2 Header: " + header2);

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
                    System.out.println("[DEBUG] MATCH FOUND: " + row1 + " <-> " + row2);
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