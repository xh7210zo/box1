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
                if (tokens.length != 3) {
                    return "[ERROR] Invalid CREATE DATABASE syntax. Usage: CREATE DATABASE <dbname>";
                }
                String dbName = tokens[2];
                Database createDatabase = new Database(storageFolderPath);
                return createDatabase.createDatabase(dbName) ? "[OK] Database created" : "[ERROR] Database already exists";
            }

            // DROP DATABASE
            if ("DROP".equals(action) && "DATABASE".equals(tokens[1].toUpperCase())) {
                if (tokens.length != 3) {
                    return "[ERROR] Invalid DROP DATABASE syntax. Usage: DROP DATABASE <dbname>";
                }
                String dbName = tokens[2];
                Database dropDatabase = new Database(storageFolderPath);
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
                Table createTableHandler = new Table(storageFolderPath);
                createTableHandler.setCurrentDatabase(currentDatabase);
                return createTableHandler.createTable(tableName, columns) ? "[OK] Table created" : "[ERROR] Table already exists or invalid column names";
            }

            // DROP TABLE
            if ("DROP".equals(action) && "TABLE".equals(tokens[1].toUpperCase())) {
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
                if (tokens.length != 5) {
                    return "[ERROR] Invalid ALTER TABLE syntax. Usage: ALTER TABLE <TableName> ADD/DROP <AttributeName>";
                }
                String tableName = tokens[2];
                String alterationType = tokens[3].toUpperCase();
                String columnName = tokens[4];

                ChangeTable alterTableHandler = new ChangeTable(storageFolderPath, currentDatabase);
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
                if (tokens.length < 4 || !"INTO".equals(tokens[1].toUpperCase())) {
                    return "[ERROR] Invalid INSERT syntax. Usage: INSERT INTO <table_name> VALUES (value1, value2, ...)";
                }
                String tableName = tokens[2];
                ChangeTable insertTableHandler = new ChangeTable(storageFolderPath, currentDatabase);

                List<String> values = Arrays.asList(tokens).subList(4, tokens.length);

                return insertTableHandler.insertIntoTable(tableName, values) ? "[OK] Record inserted" : "[ERROR] Invalid number of values for the table";
            }

            // SELECT * FROM <table_name>
            if ("SELECT".equals(action)) {
                if (tokens.length >= 7 && "id".equalsIgnoreCase(tokens[1]) && "FROM".equalsIgnoreCase(tokens[2]) && "WHERE".equalsIgnoreCase(tokens[4])) {
                    String tableName = tokens[3];
                    String whereColumn = tokens[5];
                    String whereValue = tokens[7].replaceAll("'", "");
                    TableQuery tableQuery = new TableQuery(storageFolderPath, currentDatabase);
                    return tableQuery.selectIdFromTable(tableName, whereColumn, whereValue);
                } else if (tokens.length < 4 || !"FROM".equals(tokens[tokens.length - 2].toUpperCase())) {
                    return "[ERROR] Invalid SELECT syntax. Usage: SELECT <column1> <column2> FROM <table_name>";
                }

                String tableName = tokens[tokens.length - 1];

                try {
                    TableReader tableReader = new TableReader(storageFolderPath, currentDatabase);
                    List<List<String>> tableData = tableReader.readTable(tableName);

                    if (tableData.isEmpty()) {
                        String columnNames = String.join("\t", tableData.get(0)) + "\n";
                        return "[OK] " + columnNames;
                    }

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

            // UPDATE
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
                String whereValue = whereParts[1].trim().replace("'", "");
                ChangeTable updateTableHandler = new ChangeTable(storageFolderPath, currentDatabase);
                return updateTableHandler.updateTable(tableName, setValuesMap, whereColumn, whereValue);
            }

            // DELETE
            if ("DELETE".equals(action)) {
                String tableName = tokens[2];

                String fullSQL = String.join(" ", tokens);
                int whereIndex = fullSQL.indexOf("WHERE");

                if (whereIndex == -1) {
                    return "[ERROR] Missing WHERE clause.";
                }

                String wherePart = fullSQL.substring(whereIndex + 5).trim();

                if (wherePart.contains("id") || wherePart.contains("ID")) {
                    return "[ERROR] Cannot delete from the ID column.";
                }

                String[] whereParts = wherePart.split("==", 2);
                if (whereParts.length != 2) {
                    return "[ERROR] Invalid WHERE syntax. Usage: WHERE <column> == <value>";
                }

                String whereColumn = whereParts[0].trim();
                String whereValue = whereParts[1].trim().replace("'", "");

                ChangeTable deleteTableHandler = new ChangeTable(storageFolderPath, currentDatabase);
                return deleteTableHandler.deleteFromTable(tableName, whereColumn, whereValue);
            }

            // JOIN
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
                ChangeTable joinTableHandler = new ChangeTable(storageFolderPath, currentDatabase);
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