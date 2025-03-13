package edu.uob;

import java.io.File;

public class DropDatabase {

    private String storageFolderPath;

    public DropDatabase(String storageFolderPath) {
        this.storageFolderPath = storageFolderPath;
    }

    /**
     * 删除指定数据库
     * @param dbName 数据库名称
     * @return 是否删除成功
     */
    public boolean dropDatabase(String dbName) {
        String dbPath = storageFolderPath + File.separator + dbName.toLowerCase();
        File dbFolder = new File(dbPath);

        // 检查数据库是否存在
        if (!dbFolder.exists()) {
            return false; // 数据库不存在
        }

        // 删除数据库目录及其所有内容
        return deleteDirectory(dbFolder);
    }

    // 删除目录及其内容的辅助方法
    private boolean deleteDirectory(File directory) {
        // 获取目录中的所有文件和子目录
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file); // 递归删除子目录
                } else {
                    file.delete(); // 删除文件
                }
            }
        }
        // 删除目录本身
        return directory.delete();
    }
}