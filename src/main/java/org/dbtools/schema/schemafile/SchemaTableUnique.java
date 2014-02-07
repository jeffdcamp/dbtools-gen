package org.dbtools.schema.schemafile;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root
public class SchemaTableUnique {
    @ElementList(entry = "uniqueField", inline = true, required = false)
    private List<SchemaUniqueField> uniqueFields;

    public List<SchemaUniqueField> getUniqueFields() {
        return uniqueFields;
    }

    public void setUniqueFields(List<SchemaUniqueField> uniqueFields) {
        this.uniqueFields = uniqueFields;
    }
}
