package edu.uob;

import java.util.HashMap;
import java.util.Map;

public class Row {
    private int id;
    private Map<String, String> data;

    public Row(int id) {
        this.id = id;
        this.data = new HashMap<>();
    }

    public int getId() {
        return id;
    }

    public void addData(String columnName, String value) {
        data.put(columnName, value);
    }

    public String getData(String columnName) {
        return data.get(columnName);
    }

    public Map<String, String> getValues() {
        return data;
    }
}
