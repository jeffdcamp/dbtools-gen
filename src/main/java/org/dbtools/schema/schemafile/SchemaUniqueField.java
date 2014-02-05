package org.dbtools.schema.schemafile;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * User: jcampbell
 * Date: 1/25/14
 */
@Root
public class SchemaUniqueField {
    @Element
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
