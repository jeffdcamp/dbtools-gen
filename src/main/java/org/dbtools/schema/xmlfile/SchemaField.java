package org.dbtools.schema.xmlfile;

import org.dbtools.schema.ForeignKeyType;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root
public class SchemaField {
    @Attribute
    private String name;
    @Attribute
    private SchemaFieldType jdbcDataType;
    @Attribute(required = false)
    private String varName;

    @Attribute(required = false)
    private int size = 0;
    @Attribute(required = false)
    private int decimals = 0;

    @Attribute(required = false)
    private String defaultValue;

    @Attribute(required = false)
    private boolean primaryKey = false;
    @Attribute(required = false)
    private boolean increment = false;
    @Attribute(required = false)
    private boolean notNull = false;
    @Attribute(required = false)
    private boolean unique = false;

    @Attribute(required = false)
    private boolean index = false;
    @Attribute(required = false)
    private int incrementInitialValue = 0;
    @Attribute(required = false)
    private String sequencerName;

    @Attribute(required = false)
    private String foreignKeyTable;
    @Attribute(required = false)
    private String foreignKeyField;
    @Attribute(required = false)
    private String foreignKeyOrderByColumn;
    @Attribute(required = false)
    private ForeignKeyType foreignKeyType;

    @Attribute(required = false)
    private String enumerations;
    @Attribute(required = false)
    private String enumerationDefault;

    @Attribute(required = false)
    private boolean lastModifiedField = false;

    public SchemaField() {
    }

    public SchemaField(String name, SchemaFieldType jdbcDataType) {
        this.name = name;
        this.jdbcDataType = jdbcDataType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVarName() {
        return varName;
    }

    public void setVarName(String varName) {
        this.varName = varName;
    }

    public SchemaFieldType getJdbcDataType() {
        return jdbcDataType;
    }

    public void setJdbcDataType(SchemaFieldType jdbcDataType) {
        this.jdbcDataType = jdbcDataType;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getDecimals() {
        return decimals;
    }

    public void setDecimals(int decimals) {
        this.decimals = decimals;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public boolean isIncrement() {
        return increment;
    }

    public void setIncrement(boolean increment) {
        this.increment = increment;
    }

    public boolean isNotNull() {
        return notNull;
    }

    public void setNotNull(boolean notNull) {
        this.notNull = notNull;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public boolean isIndex() {
        return index;
    }

    public void setIndex(boolean index) {
        this.index = index;
    }

    public int getIncrementInitialValue() {
        return incrementInitialValue;
    }

    public void setIncrementInitialValue(int incrementInitialValue) {
        this.incrementInitialValue = incrementInitialValue;
    }

    public String getSequencerName() {
        return sequencerName;
    }

    public void setSequencerName(String sequencerName) {
        this.sequencerName = sequencerName;
    }

    public String getForeignKeyTable() {
        return foreignKeyTable;
    }

    public void setForeignKeyTable(String foreignKeyTable) {
        this.foreignKeyTable = foreignKeyTable;
    }

    public String getForeignKeyField() {
        return foreignKeyField;
    }

    public void setForeignKeyField(String foreignKeyField) {
        this.foreignKeyField = foreignKeyField;
    }

    public String getForeignKeyOrderByColumn() {
        return foreignKeyOrderByColumn;
    }

    public void setForeignKeyOrderByColumn(String foreignKeyOrderByColumn) {
        this.foreignKeyOrderByColumn = foreignKeyOrderByColumn;
    }

    public ForeignKeyType getForeignKeyType() {
        return foreignKeyType;
    }

    public void setForeignKeyType(ForeignKeyType foreignKeyType) {
        this.foreignKeyType = foreignKeyType;
    }

    public String getEnumerations() {
        return enumerations;
    }

    public void setEnumerations(String enumerations) {
        this.enumerations = enumerations;
    }

    public String getEnumerationDefault() {
        return enumerationDefault;
    }

    public void setEnumerationDefault(String enumerationDefault) {
        this.enumerationDefault = enumerationDefault;
    }

    public boolean isLastModifiedField() {
        return lastModifiedField;
    }

    public void setLastModifiedField(boolean lastModifiedField) {
        this.lastModifiedField = lastModifiedField;
    }
}
