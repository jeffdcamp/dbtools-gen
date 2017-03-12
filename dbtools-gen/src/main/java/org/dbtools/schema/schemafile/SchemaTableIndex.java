package org.dbtools.schema.schemafile;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root
public class SchemaTableIndex {
    @ElementList(entry = "indexField", inline = true, required = false)
    private List<SchemaIndexField> indexFields;

    public List<SchemaIndexField> getIndexFields() {
        return indexFields;
    }

    public void setIndexFields(List<SchemaIndexField> indexFields) {
        this.indexFields = indexFields;
    }
}
