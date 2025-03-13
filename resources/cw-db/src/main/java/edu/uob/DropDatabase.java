package edu.uob;

import java.io.File;

public class DropDatabase {

    private String storageFolderPath;

    public DropDatabase(String storageFolderPath) {
        this.storageFolderPath = storageFolderPath;
    }

    public boolean dropDatabase(String dbName) {
        String dbPath = storageFolderPath + File.separator + dbName.toLowerCase();
        File dbFolder = new File(dbPath);

        if (!dbFolder.exists()) {
            return false;
        }

        return deleteDirectory(dbFolder);
    }

    private boolean deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        return directory.delete();
    }
}
