package org.dbtools.schema.schemafile;

import org.dbtools.schema.ForeignKeyType;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

@Root
public class SchemaTableField extends SchemaField {
    public static final int DEFAULT_INITIAL_INCREMENT = 1;

    @Attribute(required = false)
    private boolean primaryKey = false;
    @Attribute(required = false)
    private boolean increment = false;
    @Attribute(required = false)
    private boolean unique = false;

    @Attribute(required = false)
    private boolean index = false;
    @Attribute(required = false)
    private int incrementInitialValue = 0;
    @Attribute(required = false)
    private String sequencerName = "";
    @Attribute(required = false)
    private int sequencerStartValue = 1;

    @Attribute(required = false)
    private String foreignKeyOrderByColumn = "";
    @Attribute(required = false)
    private String foreignKeyCascadeType = "ALL";

    @Attribute(required = false)
    private String enumerations = "";

    @Attribute(required = false)
    private boolean lastModifiedField = false;

    public SchemaTableField() {
    }

    public SchemaTableField(String name, SchemaFieldType jdbcDataType) {
        setName(name);
        setJdbcDataType(jdbcDataType);
    }

    public void validate() {
        if (getForeignKeyType() == ForeignKeyType.ENUM) {
            setNotNull(true);
        }

        if (enumerations != null && enumerations.length() > 0) {
            if (!getJdbcDataType().isNumberDataType() && getJdbcDataType() != SchemaFieldType.VARCHAR) {
                throw new IllegalStateException("Enumerations can ONLY be used with INTEGER or VARCHAR datatypes for field [" + getName() + "]");
            }
        }
    }

    public boolean isEnumeration() {
        return (enumerations.length() > 0 || isForeignKeyIsEnumeration());
    }


    public List<String> getEnumValues() {
        if (enumerations == null || enumerations.isEmpty()) {
            return null;
        }

        List<String> enumValues = new ArrayList<>();

        StringBuilder cleanEnumArrayStr = new StringBuilder();
        for (char c : enumerations.toCharArray()) {
            if (c != ' ') {
                cleanEnumArrayStr.append(c);
            }
        }

        String[] array = cleanEnumArrayStr.toString().split(",");
        for (String enumItem : array) {
            enumValues.add(enumItem.trim());
        }

        return enumValues;
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

    public int getSequencerStartValue() {
        return sequencerStartValue;
    }

    public void setSequencerStartValue(int sequencerStartValue) {
        this.sequencerStartValue = sequencerStartValue;
    }

    public String getForeignKeyOrderByColumn() {
        return foreignKeyOrderByColumn;
    }

    public void setForeignKeyOrderByColumn(String foreignKeyOrderByColumn) {
        this.foreignKeyOrderByColumn = foreignKeyOrderByColumn;
    }

    public String getForeignKeyCascadeType() {
        return foreignKeyCascadeType;
    }

    public void setForeignKeyCascadeType(String foreignKeyCascadeType) {
        this.foreignKeyCascadeType = foreignKeyCascadeType;
    }

    public String getEnumerations() {
        return enumerations;
    }

    public void setEnumerations(String enumerations) {
        this.enumerations = enumerations;
    }

    public boolean isLastModifiedField() {
        return lastModifiedField;
    }

    public void setLastModifiedField(boolean lastModifiedField) {
        this.lastModifiedField = lastModifiedField;
    }
}
