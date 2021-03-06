/*
 * DerbyRenderer.java
 *
 * Created on July 17, 2006
 *
 * Copyright 2006 Jeff Campbell. All rights reserved. Unauthorized reproduction 
 * is a violation of applicable law. This material contains certain 
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */

/*
 * http://db.apache.org/derby/docs/10.1/ref/ 
 */
package org.dbtools.renderer;

import org.dbtools.schema.ForeignKey;
import org.dbtools.schema.schemafile.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * @author Jeff Campbell
 */
public class DerbyRenderer extends SchemaRenderer {

    public static final String RENDERER_NAME = "derby";

    public DerbyRenderer() {
        super();
        this.setDbVendorName(RENDERER_NAME);
    }

    @Override
    public String generateSchema(SchemaDatabase database, String[] tablesToGenerate, String[] viewsToGenerate, boolean dropTables, boolean createInserts) {
        showProgress("Generating SQL schema using Derby renderer ...", true);
        StringBuilder schema = new StringBuilder();
        List<ForeignKey> foreignKeysToCreate = new ArrayList<>();

        List<SchemaTable> requestedTables = getTablesToGenerate(database, tablesToGenerate);
        List<SchemaView> requestedViews = getViewsToGenerate(database, viewsToGenerate);

        // drop schema
        if (dropTables) {
            generateDropSchema(false, true, schema, requestedTables, requestedViews);
        }


        // create tables
        for (SchemaTable table : requestedTables) {
            // reset values for new table
            List<SchemaTableField> indexFields = new ArrayList<>();
            List<SchemaTableField> uniqueFields = new ArrayList<>();

            // add table header
            schema.append("CREATE TABLE ");
            schema.append(table.getName());
            schema.append(" (\n");

            // add fields
            List<SchemaTableField> fields = table.getFields();
            SchemaTableField enumPKField = null;
            SchemaTableField enumValueField = null;

            for (int j = 0; j < fields.size(); j++) {
                SchemaTableField field = fields.get(j);

                // add field
                // name
                schema.append("\t");
                schema.append(field.getName());

                // datatype
                schema.append(" ");
                schema.append(getSqlType(field.getJdbcDataType()));

                //check for size for datatype
                if (field.getSize() > 0 && field.getJdbcDataType() == SchemaFieldType.VARCHAR) {
                    schema.append("(");
                    schema.append(field.getSize());

                    schema.append(")");
                }

                String defaultValue = field.getDefaultValue();
                if (defaultValue != null && !defaultValue.equals("")) {
                    schema.append(" DEFAULT ").append(formatDefaultValue(field));
                }

                // not null
                if (field.isNotNull() && !field.isIncrement()) {
                    schema.append(" NOT NULL");
                }
                if (field.isIncrement()) {
                    schema.append(" GENERATED BY DEFAULT AS IDENTITY (START WITH ").append(field.getIncrementInitialValue()).append(")");
                }
                if (field.isPrimaryKey()) {
                    schema.append(" PRIMARY KEY");
                }
                if (field.isUnique()) {
                    uniqueFields.add(field);
                }
                if (field.isIndex()) {
                    indexFields.add(field);                // add foreign key
                }
                if (!field.getForeignKeyField().equals("")) {
                    foreignKeysToCreate.add(new ForeignKey(table.getName(), field.getName(), field.getForeignKeyTable(), field.getForeignKeyField()));
                }


                schema.append("");

                // if this is the last one, then don't put a ','
                if (j == fields.size() - 1) {
                    // add unique fields
                    if (uniqueFields.size() > 0) {
                        schema.append(",\n\tUNIQUE(");

                        for (int k = 0; k < uniqueFields.size(); k++) {
                            SchemaTableField uField = uniqueFields.get(k);
                            if (k != 0) {
                                schema.append(", ");
                            }
                            schema.append(uField.getName());
                        }

                        schema.append(")");
                    }

                    // add forengn keys fields
//                    if (foreignKeyFields.size() > 0) {
//                        for (int k = 0; k < foreignKeyFields.size(); k++) {
//                            SchemaField foreignKeyField = (SchemaField) foreignKeyFields.get(k);
//                            schema.append(",\n\tFOREIGN KEY (");
//                            
//                            schema.append(foreignKeyField.getName()).append(") REFERENCES ").append(foreignKeyField.getForeignKeyTable()).append(" (").append(foreignKeyField.getForeignKeyField()).append(")");
//                        }
//                    }
                } else {
                    // more fields to come...
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
            generateUniqueConstraints(schema, table);

            // check for indexDeclarations
            generateIndexes(schema, table);

            // add table footer
            schema.append("\n);");

            // create indexes
            for (SchemaTableField indexField : indexFields) {
                schema.append("\nCREATE INDEX ").append(table.getName()).append(indexField.getName()).append("_IDX ON ").append(table.getName()).append(" (").append(indexField.getName()).append(");");
            }

            schema.append("\n\n");

            generateEnumSchema(schema, table, getAlreadyCreatedEnum(), enumPKField, enumValueField, createInserts);
        } // end of tables

        // create foreign keys
        for (ForeignKey fk : foreignKeysToCreate) {
            schema.append("ALTER TABLE ").append(fk.getPrimaryKeyTable()).append("\n");
            schema.append("ADD CONSTRAINT ").append(fk.getPrimaryKeyTable()).append("_").append(fk.getPrimaryKeyField()).append("_FK\n");
            schema.append("FOREIGN KEY (").append(fk.getPrimaryKeyField()).append(")\n");
            schema.append("REFERENCES ").append(fk.getForeignKeyTable()).append(" (").append(fk.getForeignKeyField()).append(");\n");
            schema.append("\n");
        }

        // create views
//        for (SchemaView view : requestedViews) {
//            // header
//            schema.append("CREATE VIEW ").append(view.getName()).append(" AS \n");
//
//            // SELECT
//            schema.append("  SELECT \n");
//
//            Iterator vfItr = view.getViewFields().iterator();
//            while (vfItr.hasNext()) {
//                SchemaViewField viewField = (SchemaViewField) vfItr.next();
//
//                schema.append("\t").append(viewField.getExpression()).append(" ").append(viewField.getName());
//
//                if (vfItr.hasNext()) {
//                    schema.append(",\n");
//                } else {
//                    schema.append("\n");
//                }
//            }
//
//            schema.append("  ").append(view.getViewPostSelectClause()).append(";");
//
//            // end
//            schema.append("\n\n");
//        } // end of views

        // return 
        return schema.toString();
    }

    @Override
    public String formatDefaultValue(SchemaTableField field) {
        String defaultValue = field.getDefaultValue();
        String newDefaultValue = "";

        Class javaType = field.getJavaClassType();
        if (javaType == boolean.class) {
            if (defaultValue.equalsIgnoreCase("TRUE") || defaultValue.equals("1")) {
                newDefaultValue = "1";
            } else {
                newDefaultValue = "0";
            }
        } else if (javaType == Date.class) {
            if (defaultValue.equalsIgnoreCase("now")) {
                newDefaultValue = "CURRENT_TIMESTAMP";
            }

        } else {
            newDefaultValue = super.formatDefaultValue(field);
        }

        return newDefaultValue;
    }

    @Override
    public String generatePostSchema(SchemaDatabase database, String[] tablesToGenerate) {
        StringBuilder postSchema = new StringBuilder();
        postSchema.append("\n\n");

        List requestedTables = getTablesToGenerate(database, tablesToGenerate);
        for (Object requestedTable : requestedTables) {
            // get table
            SchemaTable table = (SchemaTable) requestedTable;
            boolean containsSequence = false;

            List fields = table.getFields();
            Iterator fItr = fields.iterator();
            while (fItr.hasNext() && !containsSequence) {
                SchemaTableField field = (SchemaTableField) fItr.next();
                if (field.isIncrement()) {
                    containsSequence = true;
                }
            }

            // subselect does not work in the following query.... moving to incrementInitialValue
//            if (containsSequence) {
//                postSchema.append("ALTER TABLE " + table.getName() + " ALTER COLUMN ID RESTART WITH (SELECT MAX(ID) FROM " + table.getName() + ")+1;\n");
//            }
        }

        return postSchema.toString();
    }
}
