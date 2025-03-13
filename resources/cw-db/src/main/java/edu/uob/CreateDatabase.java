package edu.uob;

import java.io.File;
import java.io.IOException;

public class CreateDatabase {

    private String storageFolderPath;

    public CreateDatabase(String storageFolderPath) {
        this.storageFolderPath = storageFolderPath;
    }

    /**
     * 创建数据库
     * @param dbName 数据库名称
     * @return 是否创建成功
     * @throws IOException
     */
    public boolean createDatabase(String dbName) throws IOException {
        String dbPath = storageFolderPath + File.separator + dbName.toLowerCase();
        File dbFolder = new File(dbPath);

        // 如果数据库文件夹已存在，返回 false
        if (dbFolder.exists()) {
            return false;
        }

        // 尝试创建文件夹
        return dbFolder.mkdirs();
    }
}