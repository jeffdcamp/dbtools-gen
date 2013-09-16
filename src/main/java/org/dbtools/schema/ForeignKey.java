/*
 * ForeignKey.java
 *
 * Created on March 18, 2003
 *
 * Copyright 2006 Jeff Campbell. All rights reserved. Unauthorized reproduction 
 * is a violation of applicable law. This material contains certain 
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */

package org.dbtools.schema;

/**
 * @author jeff
 */
public class ForeignKey {
    public static final int TYPE_IMPORTED_KEYS = 10;
    public static final int TYPE_EXPORTED_KEYS = 20;
    private int type = TYPE_IMPORTED_KEYS;

    private String primaryKeyTable;
    private String primaryKeyField;
    private String primaryKeyName;

    private String foreignKeyTable;
    private String foreignKeyField;
    private String foreignKeyName;

    private String keySequence;

    /**
     * Creates a new instance of ForeignKey
     */
    public ForeignKey() {
    }

    public ForeignKey(String primaryKeyTable, String primaryKeyField, String foreignKeyTable, String foreignKeyField) {
        this.primaryKeyTable = primaryKeyTable;
        this.primaryKeyField = primaryKeyField;
        this.foreignKeyTable = foreignKeyTable;
        this.foreignKeyField = foreignKeyField;
    }

    @Override
    public String toString() {
        switch (type) {
            default:
            case TYPE_IMPORTED_KEYS:
                return foreignKeyField + " -> " + primaryKeyTable + "." + primaryKeyField + " | (" + foreignKeyName + " -> " + primaryKeyName + ")";
            case TYPE_EXPORTED_KEYS:
                return primaryKeyField + " <- " + foreignKeyTable + "." + foreignKeyField + " | (" + primaryKeyName + " <- " + foreignKeyName + ")";
        }
    }

    public String getSimpleDescription() {
        switch (type) {
            default:
            case TYPE_IMPORTED_KEYS:
                return foreignKeyField + " -> " + primaryKeyTable + "." + primaryKeyField;
            case TYPE_EXPORTED_KEYS:
                return primaryKeyField + " <- " + foreignKeyTable + "." + foreignKeyField;
        }
    }

    public String getDetailedDescription() {
        switch (type) {
            default:
            case TYPE_IMPORTED_KEYS:
                return foreignKeyName + " -> " + primaryKeyName;
            case TYPE_EXPORTED_KEYS:
                return primaryKeyName + " <- " + foreignKeyName;
        }
    }

    /**
     * Getter for property primaryKeyTable.
     *
     * @return Value of property primaryKeyTable.
     */
    public java.lang.String getPrimaryKeyTable() {
        return primaryKeyTable;
    }

    /**
     * Setter for property primaryKeyTable.
     *
     * @param primaryKeyTable New value of property primaryKeyTable.
     */
    public void setPrimaryKeyTable(java.lang.String primaryKeyTable) {
        this.primaryKeyTable = primaryKeyTable;
    }

    /**
     * Getter for property primaryKeyField.
     *
     * @return Value of property primaryKeyField.
     */
    public java.lang.String getPrimaryKeyField() {
        return primaryKeyField;
    }

    /**
     * Setter for property primaryKeyField.
     *
     * @param primaryKeyField New value of property primaryKeyField.
     */
    public void setPrimaryKeyField(java.lang.String primaryKeyField) {
        this.primaryKeyField = primaryKeyField;
    }

    /**
     * Getter for property foreignKeyTable.
     *
     * @return Value of property foreignKeyTable.
     */
    public java.lang.String getForeignKeyTable() {
        return foreignKeyTable;
    }

    /**
     * Setter for property foreignKeyTable.
     *
     * @param foreignKeyTable New value of property foreignKeyTable.
     */
    public void setForeignKeyTable(java.lang.String foreignKeyTable) {
        this.foreignKeyTable = foreignKeyTable;
    }

    /**
     * Getter for property foreignKeyField.
     *
     * @return Value of property foreignKeyField.
     */
    public java.lang.String getForeignKeyField() {
        return foreignKeyField;
    }

    /**
     * Setter for property foreignKeyField.
     *
     * @param foreignKeyField New value of property foreignKeyField.
     */
    public void setForeignKeyField(java.lang.String foreignKeyField) {
        this.foreignKeyField = foreignKeyField;
    }

    /**
     * Getter for property keySequence.
     *
     * @return Value of property keySequence.
     */
    public java.lang.String getKeySequence() {
        return keySequence;
    }

    /**
     * Setter for property keySequence.
     *
     * @param keySequence New value of property keySequence.
     */
    public void setKeySequence(java.lang.String keySequence) {
        this.keySequence = keySequence;
    }

    /**
     * Getter for property primaryKeyName.
     *
     * @return Value of property primaryKeyName.
     */
    public java.lang.String getPrimaryKeyName() {
        return primaryKeyName;
    }

    /**
     * Setter for property primaryKeyName.
     *
     * @param primaryKeyName New value of property primaryKeyName.
     */
    public void setPrimaryKeyName(java.lang.String primaryKeyName) {
        this.primaryKeyName = primaryKeyName;
    }

    /**
     * Getter for property foreignKeyName.
     *
     * @return Value of property foreignKeyName.
     */
    public java.lang.String getForeignKeyName() {
        return foreignKeyName;
    }

    /**
     * Setter for property foreignKeyName.
     *
     * @param foreignKeyName New value of property foreignKeyName.
     */
    public void setForeignKeyName(java.lang.String foreignKeyName) {
        this.foreignKeyName = foreignKeyName;
    }

    /**
     * Getter for property type.
     *
     * @return Value of property type.
     */
    public int getType() {
        return type;
    }

    /**
     * Setter for property type.
     *
     * @param type New value of property type.
     */
    public void setType(int type) {
        this.type = type;
    }
}
