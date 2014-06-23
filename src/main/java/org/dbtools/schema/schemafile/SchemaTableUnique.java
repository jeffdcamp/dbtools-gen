package org.dbtools.schema.schemafile;

import org.dbtools.schema.OnConflict;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root
public class SchemaTableUnique {
    @ElementList(entry = "uniqueField", inline = true, required = false)
    private List<SchemaUniqueField> uniqueFields;

    @Attribute
    private OnConflict sqliteOnConflict = OnConflict.NONE;

    public List<SchemaUniqueField> getUniqueFields() {
        return uniqueFields;
    }

    public void setUniqueFields(List<SchemaUniqueField> uniqueFields) {
        this.uniqueFields = uniqueFields;
    }

    public OnConflict getSqliteOnConflict() {
        return sqliteOnConflict;
    }

    public void setSqliteOnConflict(OnConflict sqliteOnConflict) {
        this.sqliteOnConflict = sqliteOnConflict;
    }
}
