package org.dbtools.schema.schemafile;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

@Root
public class SchemaView {
    @Attribute
    private String name;

    @Attribute(required = false)
    private String className;

    @ElementList(entry = "field", inline = true)
    List<SchemaViewField> fields = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<SchemaViewField> getFields() {
        return fields;
    }

    public void setFields(List<SchemaViewField> fields) {
        this.fields = fields;
    }
}
