package org.dbtools.schema.schemafile;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root
public class SchemaIndexField {
    @Attribute
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
