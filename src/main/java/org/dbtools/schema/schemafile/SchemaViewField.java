package org.dbtools.schema.schemafile;

import org.dbtools.schema.ForeignKeyType;
import org.simpleframework.xml.Root;

import java.util.List;

@Root
public class SchemaViewField extends SchemaField {
    public SchemaViewField() {
    }

    public SchemaViewField(String name, SchemaFieldType jdbcDataType) {
        setName(name);
        setJdbcDataType(jdbcDataType);
    }

    public void validate() {
        if (getForeignKeyType() == ForeignKeyType.ENUM) {
//            setNotNull(true);
        }
    }

    @Override
    public boolean isIncrement() {
        return false;
    }


    @Override
    public boolean isUnique() {
        return false;
    }

    @Override
    public List<String> getEnumValues() {
        return null;
    }

    @Override
    public String getForeignKeyCascadeType() {
        return null;
    }

    @Override
    public String getSequencerName() {
        return null;
    }
}
