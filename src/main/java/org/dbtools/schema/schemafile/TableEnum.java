package org.dbtools.schema.schemafile;

public class TableEnum {
    private String name;
    private String value;

    public TableEnum(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
