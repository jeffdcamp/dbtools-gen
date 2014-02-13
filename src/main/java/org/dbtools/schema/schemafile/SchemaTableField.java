package org.dbtools.schema.schemafile;

import org.dbtools.schema.ForeignKeyType;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

@Root
public class SchemaTableField {
    public static final int DEFAULT_INITIAL_INCREMENT = 1;

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
    private String defaultValue = "";

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
    private String sequencerName = "";
    @Attribute(required = false)
    private int sequencerStartValue = 1;

    @Attribute(required = false)
    private String foreignKeyTable = "";
    @Attribute(required = false)
    private String foreignKeyField = "";
    @Attribute(required = false)
    private String foreignKeyOrderByColumn = "";
    @Attribute(required = false)
    private ForeignKeyType foreignKeyType = ForeignKeyType.IGNORE;
    @Attribute(required = false)
    private ForeignKeyFetchType foreignKeyFetchType = ForeignKeyFetchType.LAZY;
    @Attribute(required = false)
    private String foreignKeyCascadeType = "ALL";

    @Attribute(required = false)
    private String enumerations = "";
    @Attribute(required = false)
    private String enumerationDefault = "";

    @Attribute(required = false)
    private boolean lastModifiedField = false;

    public SchemaTableField() {
    }

    public SchemaTableField(String name, SchemaFieldType jdbcDataType) {
        this.name = name;
        this.jdbcDataType = jdbcDataType;
    }

    public void validate() {
        if (getForeignKeyType() == ForeignKeyType.ENUM) {
            setNotNull(true);
        }

        if (enumerations != null && enumerations.length() > 0) {
            if (!jdbcDataType.isNumberDataType() && jdbcDataType != SchemaFieldType.VARCHAR) {
                throw new IllegalStateException("Enumerations can ONLY be used with INTEGER or VARCHAR datatypes for field [" + getName() + "]");
            }
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

    private String javaFieldNameStyleName = "";
    public String getName(boolean javaFieldNameStyle) {
        if (javaFieldNameStyle) {
            // check to see if the name of this variable is being overridden
            String customVarName = getVarName();
            if (customVarName != null && customVarName.length() > 0) {
                return customVarName;
            }

            if (javaFieldNameStyleName == null || javaFieldNameStyleName.equals("")) {
                // check to see if all letters are uppercase
                boolean isAllUppercase = false;
                for (char currentChar : name.toCharArray()) {
                    if (Character.isUpperCase(currentChar) && Character.isLetter(currentChar)) {
                        isAllUppercase = true;
                    } else if (Character.isLetter(currentChar)) {
                        isAllUppercase = false;
                        break;
                    }
                }

                String nameToConvert;
                // if all uppercase force lowercase on all letter
                if (isAllUppercase) {
                    nameToConvert = name.toLowerCase();
                } else {
                    nameToConvert = name;
                }

                for (int i = 0; i < nameToConvert.length(); i++) {
                    char currentChar = nameToConvert.charAt(i);

                    // REMOVE _ and replace next letter with an uppercase letter
                    switch (currentChar) {
                        case '_':
                            // move to the next letter
                            i++;
                            currentChar = nameToConvert.charAt(i);

                            if (!javaFieldNameStyleName.isEmpty()) {
                                javaFieldNameStyleName += Character.toString(currentChar).toUpperCase();
                            } else {
                                javaFieldNameStyleName += Character.toString(currentChar);
                            }
                            break;
                        default:
                            javaFieldNameStyleName += currentChar;
                    }
                }
            }

            return javaFieldNameStyleName;
        } else {
            return name;
        }
    }

    public boolean isEnumeration() {
        return (enumerations.length() > 0 || isForeignKeyIsEnumeration());
    }

    public boolean isForeignKeyIsEnumeration() {
        return foreignKeyType == ForeignKeyType.ENUM;
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

    public int getSequencerStartValue() {
        return sequencerStartValue;
    }

    public void setSequencerStartValue(int sequencerStartValue) {
        this.sequencerStartValue = sequencerStartValue;
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

    public ForeignKeyFetchType getForeignKeyFetchType() {
        return foreignKeyFetchType;
    }

    public void setForeignKeyFetchType(ForeignKeyFetchType foreignKeyFetchType) {
        this.foreignKeyFetchType = foreignKeyFetchType;
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
