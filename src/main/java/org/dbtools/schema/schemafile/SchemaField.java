package org.dbtools.schema.schemafile;

import org.dbtools.schema.ForeignKeyType;
import org.dbtools.util.JavaUtil;
import org.simpleframework.xml.Attribute;

import java.util.List;

public abstract class SchemaField {
    @Attribute
    private String name;

    @Attribute
    private SchemaFieldType jdbcDataType;
    @Attribute(required = false)
    private String varName = "";

    @Attribute(required = false)
    private int size = 0;
    @Attribute(required = false)
    private int decimals = 0;
    @Attribute(required = false)
    private Boolean notNull = null;

    @Attribute(required = false)
    private String defaultValue = "";

    @Attribute(required = false)
    private String foreignKeyTable = "";
    @Attribute(required = false)
    private String foreignKeyField = "";
    @Attribute(required = false)
    private ForeignKeyType foreignKeyType = ForeignKeyType.IGNORE;
    @Attribute(required = false)
    private ForeignKeyFetchType foreignKeyFetchType = ForeignKeyFetchType.LAZY;
    @Attribute(required = false)
    private String enumerationClass = "";
    @Attribute(required = false)
    private String enumerationDefault = "";
    @Attribute(required = false)
    private String sqliteCollate = null;


    public abstract boolean isPrimaryKey();
    public abstract boolean isIncrement();
    public abstract String getForeignKeyCascadeType();
    public abstract String getSequencerName();
    public abstract boolean isUnique();
    public abstract List<String> getEnumValues();

    private String javaFieldNameStyleName = "";
    public String getName(boolean javaFieldNameStyle) {
        if (javaFieldNameStyle) {
            // check to see if the name of this variable is being overridden
            String customVarName = getVarName();
            if (customVarName != null && customVarName.length() > 0) {
                return customVarName;
            }

            if (javaFieldNameStyleName == null || javaFieldNameStyleName.equals("")) {
                javaFieldNameStyleName = JavaUtil.sqlNameToJavaVariableName(getName());
            }

            return javaFieldNameStyleName;
        } else {
            return getName();
        }
    }

    public Class<?> getJavaClassType() {
        return getJdbcDataType().getJavaClassType(!isNotNull());
    }

    public String getJavaTypeText() {
        return getJdbcDataType().getJavaTypeText(!isNotNull());
    }

    public String getFormattedClassDefaultValue() {
        return formatValueForField(defaultValue);
    }

    public String formatValueForField(String inValue) {
        if (inValue == null) {
            return inValue;
        }

        String retValue = inValue;
        Class c = jdbcDataType.getJavaClassType(isNotNull());
        if ((c == Float.class || c == float.class) && !inValue.isEmpty()) {
            if (!inValue.endsWith("f")) {
                retValue = inValue + 'f';
            }
        } else if ((c == Long.class || c == long.class) && !inValue.isEmpty()) {
            if (!inValue.endsWith("l")) {
                retValue = inValue + 'l';
            }
        } else if ((c == Double.class || c == double.class) && !inValue.isEmpty()) {
            if (!inValue.endsWith("d")) {
                retValue = inValue + 'd';
            }
        }

        return retValue;
    }

    public boolean isForeignKeyIsEnumeration() {
        return foreignKeyType == ForeignKeyType.ENUM;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SchemaFieldType getJdbcDataType() {
        return jdbcDataType;
    }

    public void setJdbcDataType(SchemaFieldType jdbcDataType) {
        this.jdbcDataType = jdbcDataType;
    }

    public String getVarName() {
        return varName;
    }

    public void setVarName(String varName) {
        this.varName = varName;
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

    public String getJavaFieldNameStyleName() {
        return javaFieldNameStyleName;
    }

    public void setJavaFieldNameStyleName(String javaFieldNameStyleName) {
        this.javaFieldNameStyleName = javaFieldNameStyleName;
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

    public ForeignKeyType getForeignKeyType() {
        return foreignKeyType;
    }

    public void setForeignKeyType(ForeignKeyType foreignKeyType) {
        this.foreignKeyType = foreignKeyType;
    }

    public ForeignKeyFetchType getForeignKeyFetchType() {
        return foreignKeyFetchType;
    }

    public void setForeignKeyFetchType(ForeignKeyFetchType foreignKeyFetchType) {
        this.foreignKeyFetchType = foreignKeyFetchType;
    }

    public String getEnumerationClass() {
        return enumerationClass;
    }

    public void setEnumerationClass(String enumerationClass) {
        this.enumerationClass = enumerationClass;
    }

    public void setEnumerationDefault(String enumerationDefault) {
        this.enumerationDefault = enumerationDefault;
    }

    public String getEnumerationDefault() {
        return enumerationDefault;
    }

    public boolean isEnumeration() {
        return isForeignKeyIsEnumeration() || !getEnumerationClass().isEmpty();
    }

    public Boolean isNotNull() {
        return notNull != null ? notNull : false;
    }

    public void setNotNull(Boolean notNull) {
        this.notNull = notNull;
    }

    public String getSqliteCollate() {
        return sqliteCollate;
    }

    public void setSqliteCollate(String sqliteCollate) {
        this.sqliteCollate = sqliteCollate;
    }

    public void setNotNullDefaultValue(Boolean notNullDefaultValue) {
        if (notNull == null) {
            setNotNull(notNullDefaultValue);
        }
    }
}
