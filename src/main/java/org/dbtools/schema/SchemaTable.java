/*
 * Table.java
 *
 * Created on February 23, 2002
 *
 * Copyright 2006 Jeff Campbell. All rights reserved. Unauthorized reproduction 
 * is a violation of applicable law. This material contains certain 
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.schema;

import org.dbtools.util.XMLUtil;
import org.dom4j.Attribute;
import org.dom4j.Element;

import java.util.*;

/**
 * @author Jeff Campbell
 */
public class SchemaTable {

    private String name;
    private String className;
    private Map<String, String> tableParameters = new HashMap<String, String>();
    private SchemaField primaryKey;
    private List<SchemaField> fields = new ArrayList<SchemaField>();
    private List<List<String>> uniqueDeclarations = new ArrayList<List<String>>();
    private List<String> enumerations = new ArrayList<String>();
    private Map<String, String> enumValues = new HashMap<String, String>();

    public SchemaTable(String name) {
        this.name = name;
    }

    @SuppressWarnings("unchecked")
    public SchemaTable(String db, Element tableElement) {
        name = tableElement.attribute("name").getValue();

        Attribute classNameAttr = tableElement.attribute("className");
        if (classNameAttr != null) {
            this.className = classNameAttr.getValue();
        } else {
            this.className = createJavaStyleName(name);
        }

        // get table parameters
        for (Element pElement : (List<Element>) tableElement.elements("tableParameter")) {

            // add parameters... if db is specified
            String dbName = pElement.attribute("db").getValue();
            if (db != null && db.toUpperCase().equals(dbName.toString().toUpperCase())) {
                tableParameters.put(pElement.attribute("name").getValue(), pElement.attribute("value").getValue());
            }
        }

        // enumeration (to generate a enum class)
        String enumArrayStr = XMLUtil.getAttribute(tableElement, "enumerations", false, null);
        if (enumArrayStr != null && enumArrayStr.length() > 0) {


            // cleanup the enumerations by doing:
            // 1. removing extra spaces in enum area
            StringBuilder cleanEnumArrayStr = new StringBuilder();
            boolean afterEqual = false;
            for (char c : enumArrayStr.toCharArray()) {
                if (c == '=') {
                    afterEqual = true;
                }

                if (c == ' ' && !afterEqual) {
                    //skip
                } else {
                    cleanEnumArrayStr.append(c);
                }
            }

            //  VALUE1=Jeff,VALUE2=Ricky
            String enumItem = "";
            String enumValue = "";
            afterEqual = false;
            int charCount = 0;
            for (char c : enumArrayStr.toCharArray()) {
                charCount++;

                if (c == ',') {
                    addEnumItem(enumValue.trim(), enumItem.trim());

                    enumItem = "";
                    enumValue = "";
                    afterEqual = false;
                } else {
                    if (c == '=' && !afterEqual) {
                        afterEqual = true;
                        continue;
                    }

                    if (!afterEqual) {
                        enumItem += c;
                    } else {
                        enumValue += c;
                    }

                    // if this is the last character.... store
                    if (charCount == enumArrayStr.length()) {
                        addEnumItem(enumValue.trim(), enumItem.trim());
                    }
                }

            }
        }


        // get fields
        for (Element fieldElement : (List<Element>) tableElement.elements("field")) {
            SchemaField newField = addField(fieldElement);

            // identify if the field is the primary key.
            if (newField.isPrimaryKey()) {
                if (getPrimaryKey() != null) {
                    throw new IllegalStateException("Cannot have 2 primary key fields for table [" + getName() + "].[" + newField.getName() + "]");
                }
                setPrimaryKey(newField);
            }
        }

        // get table unique keys
        for (Element uElement : (List<Element>) tableElement.elements("unique")) {
            // get table unique fields
            List<String> uniqueFields = new ArrayList<String>();
            Iterator<Element> uniqueFieldsItr = uElement.elementIterator("uniqueField");
            Element ufElement;

            while (uniqueFieldsItr.hasNext()) {

                ufElement = uniqueFieldsItr.next();

                // add parameters... if db is specified
                String uniqueField = ufElement.attribute("name").getValue();
                uniqueFields.add(uniqueField);
            }

            uniqueDeclarations.add(uniqueFields);
        }
    }

    public SchemaField addField(SchemaField newField) {
        fields.add(newField);
        return newField;
    }

    private void addEnumItem(String enumValue, String enumItem) {
        enumerations.add(enumItem);

        if (enumValue.length() != 0) {
            enumValues.put(enumItem, enumValue);
        } else {
            // make the enumItem into a enumValue
            char prevEnumChar = ' ';
            String newEnumValue = "";
            int enumItemItr = 0;
            for (char enumChar : enumItem.toCharArray()) {
                if (enumItemItr == 0 || prevEnumChar == '_') {
                    newEnumValue += Character.toUpperCase(enumChar);
                } else if (enumChar == '_') {
                    // replace _ with space
                    newEnumValue += " ";
                } else {
                    newEnumValue += Character.toLowerCase(enumChar);
                }

                prevEnumChar = enumChar;
                enumItemItr++;
            }

            enumValues.put(enumItem, newEnumValue);
        }
    }

    private SchemaField addField(Element fieldElement) {
        return addField(new SchemaField(fieldElement));
    }

    /**
     * Getter for property name.
     *
     * @return Value of property name.
     */
    public java.lang.String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public static String createJavaStyleName(String tableName) {
        String javaClassnameStyleName = "";
        // check to see if all letters are uppercase
        boolean isAllUppercase = false;
        for (char currentChar : tableName.toCharArray()) {
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
            nameToConvert = tableName.toLowerCase();
        } else {
            nameToConvert = tableName;
        }

        for (int i = 0; i < nameToConvert.length(); i++) {
            char currentChar = nameToConvert.charAt(i);

            // ALWAYS Upper-case the first letter
            if (i == 0) {
                javaClassnameStyleName += Character.toString(currentChar).toUpperCase();
            } else {
                // REMOVE _ and replace next letter with an uppercase letter
                switch (currentChar) {
                    case '_':
                        // move to the next letter
                        i++;
                        currentChar = nameToConvert.charAt(i);

                        javaClassnameStyleName += Character.toString(currentChar).toUpperCase();
                        break;
                    default:
                        javaClassnameStyleName += currentChar;
                }
            }
        }
        return javaClassnameStyleName;
    }

    /**
     * Getter for property primaryKey.
     *
     * @return Value of property primaryKey.
     */
    public SchemaField getPrimaryKey() {
        return primaryKey;
    }

    /**
     * Setter for property primaryKey.
     *
     * @param primaryKey New value of property primaryKey.
     */
    public void setPrimaryKey(SchemaField primaryKey) {
        this.primaryKey = primaryKey;
    }

    /**
     * Getter for property fields.
     *
     * @return Value of property fields.
     */
    public List<SchemaField> getFields() {
        return Collections.unmodifiableList(fields);
    }

    /**
     * Returns a list of Fields that reference a specified SchemaTable
     *
     * @param tableName Name of table that fields reference
     * @return List of Fields (foreign key)
     */
    public List<SchemaField> getForeignKeyFields(String tableName) {
        List<SchemaField> fkFields = new ArrayList<SchemaField>();

        for (SchemaField field : fields) {
            String fkTable = field.getForeignKeyTable();
            if (fkTable != null && fkTable.equalsIgnoreCase(tableName)) {
                fkFields.add(field);
            }
        }

        return fkFields;
    }

    /**
     * Returns a list of Fields that reference another table.
     *
     * @return List of Fields (foreign key)
     */
    public List<SchemaField> getForeignKeyFields() {
        List<SchemaField> fkFields = new ArrayList<SchemaField>();

        for (SchemaField field : fields) {
            String fkTable = field.getForeignKeyTable();
            if (fkTable != null && fkTable.length() > 0) {
                fkFields.add(field);
            }
        }

        return fkFields;
    }

    /**
     * Setter for property fields.
     *
     * @param fields New value of property fields.
     */
    public void setFields(List<SchemaField> fields) {
        this.fields = fields;
    }

    public String getParameter(String key) {
        return tableParameters.get(key);
    }

    /**
     * Getter for property uniqueDeclarations.
     *
     * @return Value of property uniqueDeclarations.
     */
    public List<List<String>> getUniqueDeclarations() {
        return Collections.unmodifiableList(uniqueDeclarations);
    }

    /**
     * Setter for property uniqueDeclarations.
     *
     * @param uniqueDeclarations New value of property uniqueDeclarations.
     */
    public void setUniqueDeclarations(List<List<String>> uniqueDeclarations) {
        this.uniqueDeclarations = uniqueDeclarations;
    }

    public Element toXML(Element parent) {
        Element element = parent.addElement("table");
        element.addAttribute("name", name);

        if (className != null && className.length() > 0) {
            element.addAttribute("className", name);
        }

        for (SchemaField field : getFields()) {
            field.toXML(element);
        }

        return element;
    }

    public List<String> getEnumerations() {
        return Collections.unmodifiableList(enumerations);
    }

    public void setEnumerations(List<String> enumerations) {
        this.enumerations = enumerations;
    }

    public Map<String, String> getEnumValues() {
        return Collections.unmodifiableMap(enumValues);
    }

    public boolean isEnumerationTable() {
        return enumerations.size() > 0;
    }

    @Override
    public String toString() {
        return getName();
    }
}
