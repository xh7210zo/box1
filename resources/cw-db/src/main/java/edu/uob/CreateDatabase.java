package edu.uob;

import java.io.File;
import java.io.IOException;

public class CreateDatabase {

    private String storageFolderPath;

    public CreateDatabase(String storageFolderPath) {
        this.storageFolderPath = storageFolderPath;
    }

    public boolean createDatabase(String dbName) throws IOException {
        String dbPath = storageFolderPath + File.separator + dbName.toLowerCase();
        File dbFolder = new File(dbPath);

        if (dbFolder.exists()) {
            return false;
        }

        return dbFolder.mkdirs();
    }
}
