package org.dbtools.schema.schemafile;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root
public class SchemaTableParameter {
    @Element
    private String db;

    @Element
    private String name;

    @Element
    private String value;

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
