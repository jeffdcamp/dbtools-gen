/*
 * Field.java
 *
 * Created on February 23, 2002
 *
 * Copyright 2006 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.schema;

import org.dbtools.util.XMLUtil;
import org.dom4j.Element;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.util.*;

/**
 * @author Jeff Campbell
 */
public class SchemaField {
    private String name;
    private String jdbcType;
    private int size;
    private int decimals;
    private String defaultValue;
    private boolean primaryKey;
    private boolean index;
    private boolean unique;
    private boolean createdTimeStampField;
    private boolean lastModTimeStampField;
    private boolean increment;
    public static final int DEFAULT_INITIAL_INCREMENT = 1;
    private int incrementInitialValue = DEFAULT_INITIAL_INCREMENT;
    private String sequencerName;
    private int sequencerStartValue;
    private boolean notNull;
    private ForeignKeyType foreignKeyType = ForeignKeyType.IGNORE;
    public static final String FK_FETCH_LAZY = "LAZY";
    public static final String FK_FETCH_EAGER = "EAGER";
    private String foreignKeyFetchType = FK_FETCH_LAZY;
    public static final String FK_CASCADE_ALL = "ALL";
    private String foreignKeyCascadeType = FK_CASCADE_ALL;
    private String foreignKeyField;
    private String foreignKeyTable;
    private String foreignKeyOrderByColumn;
    private String varName;
    private List<String> enumerations = new ArrayList<String>();
    private String enumerationDefault = "";
    private static Map<String, JavaType> javaTypes = new HashMap<String, JavaType>();
    public static final String TYPE_BIT = "BIT";
    public static final String TYPE_TINYINT = "TINYINT";
    public static final String TYPE_SMALLINT = "SMALLINT";
    public static final String TYPE_INTEGER = "INTEGER";
    public static final String TYPE_BIGINT = "BIGINT";
    public static final String TYPE_REAL = "REAL";
    public static final String TYPE_DOUBLE = "DOUBLE";
    public static final String TYPE_CHAR = "CHAR";
    public static final String TYPE_VARCHAR = "VARCHAR";
    public static final String TYPE_DATE = "DATE";
    public static final String TYPE_TIME = "TIME";
    public static final String TYPE_TIMESTAMP = "TIMESTAMP";
    public static final String TYPE_JAVA_OBJECT = "JAVA_OBJECT";
//    public static final String TYPE_MONEY = "MONEY"; // Custom type
    public static final String TYPE_DECIMAL = "DECIMAL";
    public static final String TYPE_NUMERIC = "NUMERIC";
//    public static final String TYPE_FRACTION = "FRACTION"; // Custom type
    public static final String TYPE_BLOB = "BLOB";
    public static final String TYPE_CLOB = "CLOB";

    static {
        // BIT | TINYINT | SMALLINT | INTEGER | NUMERIC | BIGINT | REAL | FLOAT | DOUBLE | CHAR | VARCHAR | DATE | TIME | TIMESTAMP | JAVA_OBJECT
        javaTypes.put("BOOLEAN", new JavaType("boolean", true, true, boolean.class, Boolean.class, "Boolean"));
        javaTypes.put("BIT", new JavaType("boolean", true, true, boolean.class, Boolean.class, "Boolean"));
        javaTypes.put("TINYINT", new JavaType("boolean", true, true, boolean.class, Boolean.class, "Boolean"));
        javaTypes.put("SMALLINT", new JavaType("int", true, true, int.class, Integer.class, "Integer"));
        javaTypes.put("INTEGER", new JavaType("int", true, true, int.class, Integer.class, "Integer"));
        javaTypes.put("NUMERIC", new JavaType("int", true, true, int.class, Integer.class, "Integer"));
        javaTypes.put("BIGINT", new JavaType("long", true, true, long.class, Long.class, "Long"));
        javaTypes.put("REAL", new JavaType("float", true, true, float.class, Float.class, "Float"));
        javaTypes.put("FLOAT", new JavaType("float", true, true, float.class, Float.class, "Float"));
        javaTypes.put("DOUBLE", new JavaType("double", true, true, double.class, Double.class, "Double"));
        javaTypes.put("CHAR", new JavaType("char", true, true, char.class, Character.class, "Character"));
        javaTypes.put("VARCHAR", new JavaType("String", false, true, String.class, String.class, "String"));
        javaTypes.put("LONGVARCHAR", new JavaType("String", false, true, String.class, String.class, "String"));
        javaTypes.put("DATE", new JavaType("java.util.Date", false, false, Date.class, Date.class, "java.util.Date"));
        javaTypes.put("TIME", new JavaType("Time", false, false, Time.class, Time.class, "Time"));
        javaTypes.put("TIMESTAMP", new JavaType("java.util.Date", false, false, Date.class, Date.class, "java.util.Date"));
        javaTypes.put("JAVA_OBJECT", new JavaType("Object", false, false, Object.class, Object.class, "Object"));
        javaTypes.put("DECIMAL", new JavaType("float", true, true, float.class, Float.class, "Float"));
        javaTypes.put("NUMERIC", new JavaType("float", true, true, float.class, Float.class, "Float"));
//        javaTypes.put("MONEY", new JavaType("com.jdc.datatypes.Money", false, true, Money.class, Money.class, "com.jdc.datatypes.Money"));
//        javaTypes.put("FRACTION", new JavaType("com.jdc.datatypes.Fraction", false, true, Fraction.class, Fraction.class, "com.jdc.datatypes.Fraction"));
        javaTypes.put("BIGDECIMAL", new JavaType("java.math.BigDecimal", false, true, BigDecimal.class, BigDecimal.class, "java.math.BigDecimal"));
        javaTypes.put("BIGINTEGER", new JavaType("java.math.BigInteger", false, true, BigInteger.class, BigInteger.class, "java.math.BigInteger"));
        javaTypes.put("BLOB", new JavaType("byte[]", true, true, byte[].class, Byte[].class, "Byte[]"));
        javaTypes.put("CLOB", new JavaType("String", false, true, String.class, String.class, "String"));
    }

    public SchemaField(String name, String jdbcType) {
        this.name = name;
        this.jdbcType = jdbcType;
    }

    public SchemaField(String name, int jdbcTypeID) {
        this.name = name;
        this.jdbcType = getJavaTypeFromJDBCTypeID(jdbcTypeID);
    }

    /**
     * Creates a new instance of SchemaField
     *
     * @param fieldElement
     */
    public SchemaField(Element fieldElement) {
        try {

            name = XMLUtil.getAttribute(fieldElement, "name", true);
            jdbcType = XMLUtil.getAttribute(fieldElement, "jdbcDataType", true);
            size = XMLUtil.getAttributeInt(fieldElement, "size", false, 0);
            decimals = XMLUtil.getAttributeInt(fieldElement, "decimals", false, 0);
            defaultValue = XMLUtil.getAttribute(fieldElement, "defaultValue", false);
            primaryKey = XMLUtil.getAttributeBoolean(fieldElement, "primaryKey", false, false);
            index = XMLUtil.getAttributeBoolean(fieldElement, "index", false, false);
            unique = XMLUtil.getAttributeBoolean(fieldElement, "unique", false, false);

            // since this is a new field... support if there is an old dtd
            setCreatedTimeStampField(XMLUtil.getAttributeBoolean(fieldElement, "createdTimeStampField", false, false));
            lastModTimeStampField = XMLUtil.getAttributeBoolean(fieldElement, "lastModifiedTimeStampField", false, false);

            increment = XMLUtil.getAttributeBoolean(fieldElement, "increment", false, false);
            incrementInitialValue = XMLUtil.getAttributeInt(fieldElement, "incrementInitialValue", false, DEFAULT_INITIAL_INCREMENT);
            setSequencerName(XMLUtil.getAttribute(fieldElement, "sequencerName", false));
            sequencerStartValue = XMLUtil.getAttributeInt(fieldElement, "sequencerStartValue", false, 1);
            notNull = XMLUtil.getAttributeBoolean(fieldElement, "notNull", false, false);
            foreignKeyField = XMLUtil.getAttribute(fieldElement, "foreignKeyField", false, "");
            foreignKeyTable = XMLUtil.getAttribute(fieldElement, "foreignKeyTable", false, "");
            setForeignKeyOrderByColumn(XMLUtil.getAttribute(fieldElement, "foreignKeyOrderByColumn", false, ""));
            setCustomVarName(XMLUtil.getAttribute(fieldElement, "varName", false, ""));

            String fkTypeString = XMLUtil.getAttribute(fieldElement, "foreignKeyType", false, "IGNORE");
            if (fkTypeString.equalsIgnoreCase("IGNORE")) {
                foreignKeyType = ForeignKeyType.IGNORE;
            } else if (fkTypeString.equalsIgnoreCase("ONETOONE")) {
                foreignKeyType = ForeignKeyType.ONETOONE;
            } else if (fkTypeString.equalsIgnoreCase("MANYTOONE")) {
                foreignKeyType = ForeignKeyType.MANYTOONE;
            } else if (fkTypeString.equalsIgnoreCase("ONETOMANY")) {
                foreignKeyType = ForeignKeyType.ONETOMANY;
            } else if (fkTypeString.equalsIgnoreCase("ENUM")) {
                foreignKeyType = ForeignKeyType.ENUM;

                // field is an enum field, then it should never be null
                setNotNull(true);
            } else {
                throw new IllegalArgumentException("Unknown foreignKeyType [" + fkTypeString + "]");
            }

            setForeignKeyFetchType(XMLUtil.getAttribute(fieldElement, "foreignKeyFetchType", false, FK_FETCH_LAZY));
            setForeignKeyCascadeType(XMLUtil.getAttribute(fieldElement, "foreignKeyCascadeType", false, FK_CASCADE_ALL));

            // enumeration
            String enumArrayStr = XMLUtil.getAttribute(fieldElement, "enumerations", false, null);
            if (enumArrayStr != null && enumArrayStr.length() > 0) {
                if (!isNumberDataType() && !jdbcType.equals(TYPE_VARCHAR)) {
                    throw new IllegalStateException("Enumerations can ONLY be used with INTEGER or VARCHAR datatypes for field [" + getName() + "]");
                }

                StringBuilder cleanEnumArrayStr = new StringBuilder();
                for (char c : enumArrayStr.toCharArray()) {
                    if (c != ' ') {
                        cleanEnumArrayStr.append(c);
                    }
                }

                String[] array = cleanEnumArrayStr.toString().split(",");
                for (String enumItem : array) {
                    getEnumerations().add(enumItem.trim());
                }
            }
            enumerationDefault = XMLUtil.getAttribute(fieldElement, "enumerationDefault", false, null);
            if ((enumerationDefault == null || enumerationDefault.isEmpty()) && !enumerations.isEmpty()) {
                enumerationDefault = enumerations.get(0);
            }
        } catch (Exception e) {
            System.out.println("Error converting field attribute." + e.getMessage());
        }
    }

    public Element toXML(Element parent) {
        Element element = parent.addElement("field");
        element.addAttribute("name", name);
        element.addAttribute("jdbcDataType", jdbcType);

        // size
        if (size > 0) {
            if (jdbcType.equals(TYPE_VARCHAR) || jdbcType.equals(TYPE_DECIMAL) || jdbcType.equals(TYPE_NUMERIC) || jdbcType.equals(TYPE_DOUBLE)) { // || jdbcType.equals(TYPE_MONEY)) {
                element.addAttribute("size", Integer.toString(size));
            }
        }

        if (decimals > 0) {
            if (jdbcType.equals(TYPE_DECIMAL) || jdbcType.equals(TYPE_NUMERIC) || jdbcType.equals(TYPE_DOUBLE)) { // || jdbcType.equals(TYPE_MONEY)) {
                element.addAttribute("decimals", Integer.toString(decimals));
            }
        }

        if (defaultValue != null && !defaultValue.equals("")) {
            element.addAttribute("defaultValue", defaultValue);
        }

        if (primaryKey) {
            element.addAttribute("primaryKey", "true");
        }
        if (index) {
            element.addAttribute("index", "true");
        }
        if (unique) {
            element.addAttribute("unique", "true");
        }
        if (increment) {
            element.addAttribute("increment", "true");
        }
        if (incrementInitialValue > 0) {
            if (jdbcType.equals(TYPE_DECIMAL) || jdbcType.equals(TYPE_NUMERIC) || jdbcType.equals(TYPE_DOUBLE)) { // || jdbcType.equals(TYPE_MONEY)) {
                element.addAttribute("incrementInitialValue", Integer.toString(incrementInitialValue));
            }
        }

        if (getSequencerName() != null && !getSequencerName().equals("")) {
            element.addAttribute("sequencerName", getSequencerName());
        }

        if (sequencerStartValue > 0) {
            if (jdbcType.equals(TYPE_VARCHAR) || jdbcType.equals(TYPE_DECIMAL) || jdbcType.equals(TYPE_NUMERIC) || jdbcType.equals(TYPE_DOUBLE)) { // || jdbcType.equals(TYPE_MONEY)) {
                element.addAttribute("sequencerStartValue", Integer.toString(sequencerStartValue));
            }
        }

        if (notNull) {
            element.addAttribute("notNull", "true");
        }
        if (lastModTimeStampField) {
            element.addAttribute("lastModifiedTimeStampField", "true");
        }
        if (isCreatedTimeStampField()) {
            element.addAttribute("createdTimeStampField", "true");

        }
        if (foreignKeyTable != null && foreignKeyTable.length() > 0) {
            element.addAttribute("foreignKeyTable", foreignKeyTable);
        }
        if (foreignKeyField != null && foreignKeyField.length() > 0) {
            element.addAttribute("foreignKeyField", foreignKeyField);
        }
        if (getForeignKeyOrderByColumn() != null && getForeignKeyOrderByColumn().length() > 0) {
            element.addAttribute("foreignKeyOrderByColumn", getForeignKeyOrderByColumn());
        }
        if (getCustomVarName() != null && getCustomVarName().length() > 0) {
            element.addAttribute("varName", getCustomVarName());
        }

//        if (!foreignKeyTable.equals(""))
//            element.addAttribute("foreignKeyTable", foreignKeyTable);
//        }

        return element;
    }

    public JavaType getJavaType() {
        return javaTypes.get(jdbcType);
    }

    public static Class<?> getJavaClassType(String type, boolean isNullable) {
        if (!isNullable) {
            return javaTypes.get(type).getMainClass();
        } else {
            // field is nullable.... so we CANNOT use a primative
            return javaTypes.get(type).getMatchingNonPrimativeClass();
        }
    }

    // NOTE!!! BE SURE TO CHANGE getJavaTypeText() to match changes to this method
    public Class<?> getJavaClassType() {
        Class<?> fieldClass = null;

        // check to see if we need to change from a primative to an Object
        if (isNotNull()) {
            fieldClass = javaTypes.get(jdbcType).getMainClass();
        } else {
            // field is nullable.... so we CANNOT use a primative
            fieldClass = javaTypes.get(jdbcType).getMatchingNonPrimativeClass();
        }

        return fieldClass;
    }

    // NOTE!!! BE SURE TO CHANGE getJavaClassType() to match changes to this method
    public String getJavaTypeText() {
        String fieldClass = null;

        // check to see if we need to change from a primative to an Object
        if (isNotNull()) {
            fieldClass = javaTypes.get(jdbcType).getJavaTypeText();
        } else {
            // field is nullable.... so we CANNOT use a primative
            fieldClass = javaTypes.get(jdbcType).getMatchingNonPrimativeClassText();
        }

        if (fieldClass == null) {
            throw new IllegalStateException("Unknown jdbcType [" + jdbcType + "]");
        }

        return fieldClass;
    }

    public boolean isJavaTypePrimative() {
        if (isNotNull()) {
            return javaTypes.get(jdbcType).isPrimative();
        } else {
            return false;
        }
    }

    public boolean isJavaTypeImmutable() {
        return javaTypes.get(jdbcType).isImmutable();
    }

    private String getJavaTypeFromJDBCTypeID(int jdbcTypeID) {
        String type = "";

        switch (jdbcTypeID) {
            case java.sql.Types.BIT:
                type = TYPE_BIT;
                break;
            case java.sql.Types.TINYINT:
                type = TYPE_TINYINT;
                break;
            case java.sql.Types.SMALLINT:
                type = TYPE_SMALLINT;
                break;
            case java.sql.Types.INTEGER:
                type = TYPE_INTEGER;
                break;
            case java.sql.Types.BIGINT:
                type = TYPE_BIGINT;
                break;
            case java.sql.Types.REAL:
                type = TYPE_REAL;
                break;
            case java.sql.Types.DOUBLE:
                type = TYPE_DOUBLE;
                break;
            case java.sql.Types.CHAR:
                type = TYPE_CHAR;
                break;
            case java.sql.Types.VARCHAR:
                type = TYPE_VARCHAR;
                break;
            case java.sql.Types.DATE:
                type = TYPE_DATE;
                break;
            case java.sql.Types.TIME:
                type = TYPE_TIME;
                break;
            case java.sql.Types.TIMESTAMP:
                type = TYPE_TIMESTAMP;
                break;
            case java.sql.Types.JAVA_OBJECT:
                type = TYPE_JAVA_OBJECT;
                break;
            case java.sql.Types.DECIMAL:
                type = TYPE_DECIMAL;
                break;
            case java.sql.Types.NUMERIC:
                type = TYPE_NUMERIC;
                break;
            case java.sql.Types.BLOB:
                type = TYPE_BLOB;
                break;
            case java.sql.Types.CLOB:
                type = TYPE_CLOB;
                break;
            default:
                System.out.println("WARNING... Unknown jdbc type specified: [" + jdbcTypeID + "]");
                type = "UNKNOWN";
        }

        return type;
    }

    /**
     * Getter for property name.
     *
     * @return Value of property name.
     */
    public String getName() {
        return getName(false);
    }

    private String javaFieldNameStyleName = "";

    public String getName(boolean javaFieldNameStyle) {
        if (javaFieldNameStyle) {
            // check to see if the name of this variable is being overriden
            String customVarName = getCustomVarName();
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

                            javaFieldNameStyleName += Character.toString(currentChar).toUpperCase();
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

    /**
     * Setter for property name.
     *
     * @param name New value of property name.
     */
    public void setName(String name) {
        javaFieldNameStyleName = "";
        this.name = name;
    }

    /**
     * Getter for property size.
     *
     * @return Value of property size.
     */
    public int getSize() {
        return size;
    }

    /**
     * Setter for property size.
     *
     * @param size New value of property size.
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * Getter for property primaryKey.
     *
     * @return Value of property primaryKey.
     */
    public boolean isPrimaryKey() {
        return primaryKey;
    }

    /**
     * Setter for property primaryKey.
     *
     * @param primaryKey New value of property primaryKey.
     */
    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    /**
     * Getter for property index.
     *
     * @return Value of property index.
     */
    public boolean isIndex() {
        return index;
    }

    /**
     * Setter for property index.
     *
     * @param index New value of property index.
     */
    public void setIndex(boolean index) {
        this.index = index;
    }

    /**
     * Getter for property increment.
     *
     * @return Value of property increment.
     */
    public boolean isIncrement() {
        return increment;
    }

    /**
     * Setter for property increment.
     *
     * @param increment New value of property increment.
     */
    public void setIncrement(boolean increment) {
        this.increment = increment;
    }

    public int getIncrementInitialValue() {
        return incrementInitialValue;
    }

    public void setIncrementInitialValue(int incrementInitialValue) {
        this.incrementInitialValue = incrementInitialValue;
    }

    /**
     * Setter for property defaultValue.
     *
     * @param defaultValue New value of property defaultValue.
     */
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Getter for property notNull.
     *
     * @return Value of property notNull.
     */
    public boolean isNotNull() {
        return notNull;
    }

    /**
     * Setter for property notNull.
     *
     * @param notNull New value of property notNull.
     */
    public void setNotNull(boolean notNull) {
        this.notNull = notNull;
    }

    /**
     * Getter for property jdbcType.
     *
     * @return Value of property jdbcType.
     */
    public String getJdbcType() {
        return jdbcType;
    }

    /**
     * Setter for property jdbcType.
     *
     * @param jdbcType New value of property jdbcType.
     */
    public void setJdbcType(String jdbcType) {
        this.jdbcType = jdbcType;
    }

    /**
     * Getter for property defaultValue.
     *
     * @return Value of property defaultValue.
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    public String getFormattedClassDefaultValue() {
        return formatValueForField(defaultValue);
    }

    /**
     * Getter for property decimals.
     *
     * @return Value of property decimals.
     */
    public int getDecimals() {
        return decimals;
    }

    /**
     * Setter for property decimals.
     *
     * @param decimals New value of property decimals.
     */
    public void setDecimals(int decimals) {
        this.decimals = decimals;
    }

    /**
     * Getter for property unique.
     *
     * @return Value of property unique.
     */
    public boolean isUnique() {
        return unique;
    }

    /**
     * Setter for property unique.
     *
     * @param unique New value of property unique.
     */
    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    /**
     * Getter for property foreignKeyField.
     *
     * @return Value of property foreignKeyField.
     */
    public String getForeignKeyField() {
        return foreignKeyField;
    }

    /**
     * Setter for property foreignKeyField.
     *
     * @param foreignKeyField New value of property foreignKeyField.
     */
    public void setForeignKeyField(String foreignKeyField) {
        this.foreignKeyField = foreignKeyField;
    }

    /**
     * Getter for property foreignKeyTable.
     *
     * @return Value of property foreignKeyTable.
     */
    public String getForeignKeyTable() {
        return foreignKeyTable;
    }

    /**
     * Setter for property foreignKeyTable.
     *
     * @param foreignKeyTable New value of property foreignKeyTable.
     */
    public void setForeignKeyTable(String foreignKeyTable) {
        this.foreignKeyTable = foreignKeyTable;
    }

    /**
     * Getter for property foreignKeyType.
     *
     * @return Value of property foreignKeyType.
     */
    public ForeignKeyType getForeignKeyType() {
        return foreignKeyType;
    }

    /**
     * Setter for property foreignKeyType.
     *
     * @param foreignKeyType New value of property foreignKeyType.
     */
    public void setForeignKeyType(ForeignKeyType foreignKeyType) {
        this.foreignKeyType = foreignKeyType;
    }

    /**
     * Getter for property lastModifiedField.
     *
     * @return Value of property lastModifiedField.
     */
    public boolean isLastModifiedTimeStampField() {
        return lastModTimeStampField;
    }

    /**
     * Setter for property lastModifiedField.
     *
     * @param lastModifiedTimeStampField
     */
    public void setLastModifiedTimeStampField(boolean lastModifiedTimeStampField) {
        this.lastModTimeStampField = lastModifiedTimeStampField;
    }

    public String getForeignKeyOrderByColumn() {
        return foreignKeyOrderByColumn;
    }

    public void setForeignKeyOrderByColumn(String foreignKeyOrderByColumn) {
        this.foreignKeyOrderByColumn = foreignKeyOrderByColumn;
    }

    public boolean isCreatedTimeStampField() {
        return createdTimeStampField;
    }

    public void setCreatedTimeStampField(boolean createdTimeStampField) {
        this.createdTimeStampField = createdTimeStampField;
    }

    public boolean isNumberDataType() {
        return jdbcType.equals(TYPE_INTEGER)
                || jdbcType.equals(TYPE_DECIMAL)
                || jdbcType.equals(TYPE_DOUBLE)
                || jdbcType.equals(TYPE_NUMERIC)
                || jdbcType.equals(TYPE_REAL)
                || jdbcType.equals(TYPE_SMALLINT)
                || jdbcType.equals(TYPE_BIGINT)
                || jdbcType.equals(TYPE_TINYINT);
//                || jdbcType.equals(TYPE_MONEY)
//                || jdbcType.equals(TYPE_FRACTION);
    }

    public boolean isEnumeration() {
        return (getEnumerations().size() > 0 || isForeignKeyIsEnumeration());
    }

    public List<String> getEnumerations() {
        return enumerations;
    }

    public void setEnumerations(List<String> enumerations) {
        this.enumerations = enumerations;
    }

    public String getEnumerationDefault() {
        return enumerationDefault;
    }

    public void setEnumerationDefault(String enumerationDefault) {
        this.enumerationDefault = enumerationDefault;
    }

    public boolean isForeignKeyIsEnumeration() {
        return foreignKeyType == ForeignKeyType.ENUM;
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

    public String getForeignKeyFetchType() {
        return foreignKeyFetchType;
    }

    public void setForeignKeyFetchType(String foreignKeyFetchType) {
        this.foreignKeyFetchType = foreignKeyFetchType;
    }

    public String getForeignKeyCascadeType() {
        return foreignKeyCascadeType;
    }

    public void setForeignKeyCascadeType(String foreignKeyCascadeType) {
        this.foreignKeyCascadeType = foreignKeyCascadeType;
    }

    public String getCustomVarName() {
        return varName;
    }

    private void setCustomVarName(String varName) {
        this.varName = varName;
    }

    @Override
    public String toString() {
        return getName();
    }

    public String formatValueForField(String inValue) {
        if (inValue == null) {
            return inValue;
        }

        String retValue = inValue;
        Class c = getJavaClassType();
        if (c == Float.class) {
            if (!inValue.endsWith("f")) {
                retValue = inValue + 'f';
            }
        } else if (c == Long.class) {
            if (!inValue.endsWith("l")) {
                retValue = inValue + 'l';
            }
        } else if (c == Double.class) {
            if (!inValue.endsWith("f")) {
                retValue = inValue + 'f';
            }
        }

        return retValue;
    }
}
