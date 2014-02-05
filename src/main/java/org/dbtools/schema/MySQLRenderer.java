/*
 * SchemaRenderer.java
 *
 * Created on February 23, 2002
 *
 * Copyright 2006 Jeff Campbell. All rights reserved. Unauthorized reproduction 
 * is a violation of applicable law. This material contains certain 
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.schema;

import org.dbtools.schema.schemafile.*;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Jeff Campbell
 */
public class MySQLRenderer extends SchemaRenderer {

    public static final String RENDERER_NAME = "mysql";

    public MySQLRenderer() {
        super();
        this.setDbVendorName(RENDERER_NAME);
    }

    public MySQLRenderer(PrintStream ps) {
        super(ps);
        this.setDbVendorName(RENDERER_NAME);
    }

    @Override
    public String generateSchema(SchemaDatabase database, String[] tablesToGenerate, String[] viewsToGenerate, boolean dropTables, boolean createInserts) {
        showProgress("Generating SQL schema using MySQL renderer ...", true);
        StringBuilder schema = new StringBuilder();
        List<ForeignKey> foreignKeysToCreate = new ArrayList<>();

        List<SchemaTable> requestedTables = getTablesToGenerate(database, tablesToGenerate);
        List<SchemaView> requestedViews = getViewsToGenerate(database, viewsToGenerate);

        // drop schema
        if (dropTables) {
            generateDropSchema(true, false, schema, requestedTables, requestedViews);
        }

        // create tables
        for (SchemaTable table : requestedTables) {
            // add table header
            // reset values for new table
            SchemaField primaryKey = null;
            List<SchemaField> indexFields = new ArrayList<>();

            schema.append("CREATE TABLE ");
            schema.append(table.getName());
            schema.append(" (\n");

            // add fields
            List<SchemaField> fields = table.getFields();
            SchemaField enumPKField = null;
            SchemaField enumValueField = null;

            int incrementFieldInitialValue = 1;

            for (int j = 0; j < fields.size(); j++) {
                SchemaField field = fields.get(j);

                if (field.isPrimaryKey()) {
                    primaryKey = field;                // add field
                    // name
                }
                schema.append("\t");
                schema.append(field.getName());

                // datatype
                schema.append(" ");
                schema.append(getSqlType(field.getJdbcDataType()));

                //check for size for datatype
                if (field.getSize() > 0) {
                    int digits = field.getSize() + field.getDecimals();

                    schema.append("(");
                    schema.append(digits);

                    // check for decimals
                    if (field.getDecimals() > 0) {
                        schema.append(", ").append(field.getDecimals());
                    }

                    schema.append(")");
                }

                // not null
                if (field.isNotNull()) {
                    schema.append(" NOT NULL");
                }
                if (field.isIncrement()) {
                    incrementFieldInitialValue = field.getIncrementInitialValue();

                    if (!table.isEnumerationTable()) {
                        schema.append(" AUTO_INCREMENT");
                    }
                }
                String defaultValue = field.getDefaultValue();
                if (defaultValue != null && !defaultValue.equals("")) {
                    // check the formatted value... if it is a Date... then 'now' default is NOT allowed
                    String defaultValueFormatted = formatDefaultValue(field);
                    if (defaultValueFormatted.length() > 0) {
                        schema.append(" DEFAULT ").append(defaultValueFormatted);
                    }
                }

                if (field.isUnique()) {
                    schema.append(" UNIQUE");
                }
                if (field.isIndex()) {
                    indexFields.add(field);
                }

                // add foreign key
                if (!field.getForeignKeyField().equals("")) {
                    foreignKeysToCreate.add(new ForeignKey(table.getName(), field.getName(), field.getForeignKeyTable(), field.getForeignKeyField()));
                }


                schema.append("");

                // if this is the last one, then don't put a ','
                if (j == fields.size() - 1) {
                    // add the primary key
                    if (primaryKey != null) {
                        schema.append(",\n");
                        schema.append("\tPRIMARY KEY(");
                        schema.append(primaryKey.getName());
                        schema.append(")");
                    } else {
                        //schema.append("\n");
                    }

                    // add index fields
                    for (SchemaField indexField : indexFields) {
                        schema.append(",\n\tINDEX ").append(indexField.getName()).append("_IDX (").append(indexField.getName()).append(")");
                    }

                    // add forengn keys fields
                    //if (foreignKeyFields.size() > 0) {
                    //    for (int k = 0; k < foreignKeyFields.size(); k++) {
                    //        SchemaField foreignKeyField = (SchemaField) foreignKeyFields.get(k);
                    //        schema.append(",\n\tFOREIGN KEY (");
                    //
                    //        schema.append(foreignKeyField.getName()).append(") REFERENCES ").append(foreignKeyField.getForeignKeyTable()).append(" (").append(foreignKeyField.getForeignKeyField()).append(")");
                    //    }
                    //}

                } else {
                    schema.append(",\n");
                }

                // check for enumFields
                if (enumPKField == null && field.isPrimaryKey()) {
                    enumPKField = field;
                }
                if (enumValueField == null && field.getJdbcDataType() == SchemaFieldType.VARCHAR) {
                    enumValueField = field;
                }
            }

            // check for uniqueDeclarations
            List uniqueDeclarations = table.getUniqueDeclarations();
            for (Object uniqueDeclaration : uniqueDeclarations) {
                String uniqueFieldString = "";

                List uniqueFields = (ArrayList) uniqueDeclaration;
                for (int k = 0; k < uniqueFields.size(); k++) {
                    String uniqueField = (String) uniqueFields.get(k);

                    if (k > 0) {
                        uniqueFieldString += ", ";
                    }
                    uniqueFieldString += uniqueField;
                }

                schema.append(",\n\tUNIQUE(").append(uniqueFieldString).append(")");
            }

            String tableType = table.getParameter("tableType");
            // add table footer
            // add table type if needed
            if (tableType != null && !tableType.equals("")) {
                schema.append("\n) TYPE=");
                schema.append(tableType);
                schema.append(";\n\n");
            } else {
                schema.append("\n);\n\n");
            }

            // check for default auto-encrement
            if (!table.isEnumerationTable() && incrementFieldInitialValue > 1) {
                schema.append("\nALTER TABLE ").append(table.getName()).append(" AUTO_INCREMENT = ").append(incrementFieldInitialValue).append(";\n\n");
            }

            generateEnumSchema(schema, table, getAlreadyCreatedEnum(), enumPKField, enumValueField, createInserts);
        }

        // create foreign keys
        for (ForeignKey fk : foreignKeysToCreate) {
            schema.append("ALTER TABLE ").append(fk.getPrimaryKeyTable()).append("\n");
            schema.append("ADD CONSTRAINT ").append(fk.getPrimaryKeyTable()).append("_").append(fk.getPrimaryKeyField()).append("_FK\n");
            schema.append("FOREIGN KEY (").append(fk.getPrimaryKeyField()).append(")\n");
            schema.append("REFERENCES ").append(fk.getForeignKeyTable()).append(" (").append(fk.getForeignKeyField()).append(");\n");
            schema.append("\n");
        }

        return schema.toString();
    }

    @Override
    public String formatDefaultValue(SchemaField field) {
        String defaultValue = field.getDefaultValue();
        String newDefaultValue = "";

        Class javaType = field.getJavaClassType();
        if (javaType == Date.class) {
            if (defaultValue.equalsIgnoreCase("now")) {
                newDefaultValue = ""; // now is NOT supported by MySQL
                System.out.println("WARNING: MySQL does NOT support now or now() as a default for date fields... skipping DEFAULT");
            }
        } else {
            newDefaultValue = super.formatDefaultValue(field);
        }

        return newDefaultValue;
    }
}
