package org.dbtools.schema.xmlfile;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.util.List;

@Root
public class SchemaTableUnique {
    @Element(required = false, name = "uniqueField")
    private List<SchemaUniqueField> uniqueFields;

    public List<SchemaUniqueField> getUniqueFields() {
        return uniqueFields;
    }

    public void setUniqueFields(List<SchemaUniqueField> uniqueFields) {
        this.uniqueFields = uniqueFields;
    }
}
