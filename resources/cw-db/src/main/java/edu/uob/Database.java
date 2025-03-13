package edu.uob;

import java.util.HashMap;
import java.util.Map;

public class Database {
    private String name;
    private Map<String, Table> tables;

    public Database(String name) {
        this.name = name;
        this.tables = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public void addTable(Table table) {
        tables.put(table.getName(), table);
    }


    public Map<String, Table> getTables() {
        return tables;
    }
}
