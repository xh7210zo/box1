package edu.uob;

import java.io.*;
import java.util.*;

public class TableReader {

    private String storageFolderPath;
    private String currentDatabase;

    public TableReader(String storageFolderPath, String currentDatabase) {
        this.storageFolderPath = storageFolderPath;
        this.currentDatabase = currentDatabase;
    }

    public List<List<String>> readTable(String tableName) throws IOException {
        if (currentDatabase == null) {
            throw new IOException("No database selected.");
        }

        //clean all the ';' and space at the end
        String cleanTableName = tableName.replace(";", "").trim();
        String tablePath = storageFolderPath + File.separator + currentDatabase.toLowerCase() + File.separator + cleanTableName.toLowerCase() + ".tab";
        File tableFile = new File(tablePath);

        if (!tableFile.exists()) {
            throw new IOException("Table does not exist: " + tableName);
        }

        //store the data of the table
        List<List<String>> tableData = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(tableFile))) {
            String line;

            //read data
            while ((line = reader.readLine()) != null) {
                //store data in the List
                tableData.add(Arrays.asList(line.split("\t")));
            }
        }

        return tableData;
    }
}
