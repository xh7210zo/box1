package edu.uob;

import java.io.File;
import java.io.IOException;

public class Database {

    private String storageFolderPath;

    public Database(String storageFolderPath) {
        this.storageFolderPath = storageFolderPath;
    }

    public boolean createDatabase(String dbName) throws IOException {

        //change to lowercase
        String dbPath = storageFolderPath + File.separator + dbName.toLowerCase();
        File dbFolder = new File(dbPath);

        if (dbFolder.exists()) {
            return false;
        }

        //return if successfully made
        return dbFolder.mkdirs();
    }
    public boolean dropDatabase(String dbName) {
        String dbPath = storageFolderPath + File.separator + dbName.toLowerCase();
        File dbFolder = new File(dbPath);

        if (!dbFolder.exists()) {
            return false;
        }

        //delete directory and its content
        return deleteDirectory(dbFolder);
    }

    private boolean deleteDirectory(File directory) {

        //get all the files and directories
        File[] files = directory.listFiles();

        //delete files recursively
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    //delete directory
                    deleteDirectory(file);
                } else {
                    //delete file
                    file.delete();
                }
            }
        }
        return directory.delete();
    }
}
