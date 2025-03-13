package edu.uob;

public class Column {
    private String name;
    private String type;  // 类型，例如：INT, STRING

    public Column(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}