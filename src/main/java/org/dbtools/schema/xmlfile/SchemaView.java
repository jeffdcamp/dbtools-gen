package org.dbtools.schema.xmlfile;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root
public class SchemaView {
    @Attribute(required = false)
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
