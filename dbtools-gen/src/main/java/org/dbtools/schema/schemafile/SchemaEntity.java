package org.dbtools.schema.schemafile;

import java.util.List;

public abstract class SchemaEntity {
    public abstract SchemaEntityType getType();
    public abstract String getName();
    public abstract String getClassName();
    public abstract boolean isEnumerationTable();
    public abstract boolean isReadonly();
    public abstract List<? extends SchemaField> getFields();
}
