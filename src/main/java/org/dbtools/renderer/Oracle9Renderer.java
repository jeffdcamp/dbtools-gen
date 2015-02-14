/*
 * Oracle9Renderer.java
 *
 * Created on October 30, 2002
 *
 * Copyright 2006 Jeff Campbell. All rights reserved. Unauthorized reproduction 
 * is a violation of applicable law. This material contains certain 
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.renderer;

import org.dbtools.schema.ForeignKey;
import org.dbtools.schema.schemafile.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jeff Campbell
 */
public class Oracle9Renderer extends SchemaRenderer {

    public static final String RENDERER_NAME = "oracle";
    private static final String NOW = "LOCALTIMESTAMP";

    public Oracle9Renderer() {
        super();
        this.setDbVendorName(RENDERER_NAME);
    }

    @Override
    public String generateSchema(SchemaDatabase database, String[] tablesToGenerate, String[] viewsToGenerate, boolean dropTables, boolean createInserts) {
        showProgress("Generating SQL schema using Oracle9 renderer ...", true);
        StringBuilder schema = new StringBuilder();
        List<ForeignKey> foreignKeysToCreate = new ArrayList<ForeignKey>();

        List<SchemaTable> requestedTables = getTablesToGenerate(database, tablesToGenerate);
        List<SchemaView> requestedViews = getViewsToGenerate(database, viewsToGenerate);

        // drop schema
        if (dropTables) {
            generateDropSchema(false, true, schema, requestedTables, requestedViews);
        }

        // create tables
        for (SchemaTable table : requestedTables) {
            // add table header
            List<SchemaTableField> fields = table.getFields();

            // determine sequence name
            String sequencerName = null;
            int sequencerStartValue = 1;
            for (SchemaTableField field : fields) {
                String fieldSeqName = field.getSequencerName();
                if (fieldSeqName != null && fieldSeqName.length() > 0) {
                    sequencerName = fieldSeqName;
                    sequencerStartValue = field.getSequencerStartValue();
                    break;
                }
            }

            // reset values for new table
            SchemaTableField primaryKey = null;
            List<SchemaTableField> indexFields = new ArrayList<SchemaTableField>();

            schema.append("CREATE TABLE ");
            schema.append(table.getName());
            schema.append(" (\n");

            // add fields
            SchemaTableField enumPKField = null;
            SchemaTableField enumValueField = null;

            for (int j = 0; j < fields.size(); j++) {
                SchemaTableField field = fields.get(j);

                if (field.isPrimaryKey()) {
                    primaryKey = field;
                }

                // add field
                // name
                schema.append("\t");
                schema.append(field.getName());

                // datatype
                schema.append(" ");

                schema.append(getSqlType(field.getJdbcDataType()));

                //check for size for datatype
                if (field.getSize() > 0) {
                    schema.append("(");
                    schema.append(field.getSize());

                    // check for decimals
                    if (field.getDecimals() > 0) {
                        schema.append(", ").append(field.getDecimals());
                    }

                    schema.append(")");
                }

                String defaultValue = field.getDefaultValue();
                if (defaultValue != null && !defaultValue.equals("")) {
                    String newDefaultValue = formatDefaultValue(field);

                    if (newDefaultValue.toUpperCase().equals("'NOW'")) {
                        newDefaultValue = NOW;
                    }

                    schema.append(" DEFAULT ").append(newDefaultValue);
                } else if (field.isNotNull()) {
                    schema.append(" NOT NULL");
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

                List uniqueFieldsCombo = (List) uniqueDeclaration;
                for (int k = 0; k < uniqueFieldsCombo.size(); k++) {
                    String uniqueField = (String) uniqueFieldsCombo.get(k);

                    if (k > 0) {
                        uniqueFieldString += ", ";
                    }

                    uniqueFieldString += uniqueField;
                }

                schema.append(",\n\tUNIQUE(").append(uniqueFieldString).append(")");
            }

            // add table footer
            schema.append("\n);");

            // create indexes
            for (SchemaTableField indexField : indexFields) {
                schema.append("\nCREATE INDEX ").append(table.getName()).append(indexField.getName()).append("_IDX ON ").append(table.getName()).append(" (").append(indexField.getName()).append(");");
            }
            schema.append("\n");
            generateEnumSchema(schema, table, getAlreadyCreatedEnum(), enumPKField, enumValueField, createInserts);

            // check to see if we need to create a sequence
            if (sequencerName != null && sequencerName.length() > 0) {
                if (table.isEnumerationTable() && super.isCreateEnumInserts()) {
                    schema.append("CREATE SEQUENCE ").append(sequencerName).append(" START WITH ").append(table.getEnumerations().length()).append(";\n");
                } else {
                    schema.append("CREATE SEQUENCE ").append(sequencerName).append(" START WITH ").append(sequencerStartValue).append(";\n");
                }
            }

            schema.append("\n\n");
        }

        // create foreign keys
        Map<String, Integer> fkCounter = new HashMap<String, Integer>();
        final String GLOBAL_FK_CONST_NAME = "DB_FK";
        for (ForeignKey fk : foreignKeysToCreate) {
            schema.append("ALTER TABLE ").append(fk.getPrimaryKeyTable()).append("\n");

            // limited to 30 characters in Oracle
            String fkTableName = fk.getPrimaryKeyTable();
            String constraintName = fkTableName + "_" + fk.getPrimaryKeyField() + "_FK";
            if (constraintName.length() >= 30) {
                int i = 1;
                if (fkCounter.containsKey(fkTableName)) {
                    i = fkCounter.get(fkTableName);
                    i++;
                }

                fkCounter.put(fkTableName, i);
                constraintName = fk.getPrimaryKeyTable() + "_FK" + i;

                // if it is STILL too long
                if (constraintName.length() >= 30) {
                    constraintName = GLOBAL_FK_CONST_NAME + i;
                }
            }

            schema.append("ADD CONSTRAINT ").append(constraintName).append("\n");
            schema.append("FOREIGN KEY (").append(fk.getPrimaryKeyField()).append(")\n");
            schema.append("REFERENCES ").append(fk.getForeignKeyTable()).append(" (").append(fk.getForeignKeyField()).append(") ENABLE;\n");
            schema.append("\n");
        }

        // create views
//        for (SchemaView view : requestedViews) {
//
//            // header
//            schema.append("CREATE VIEW ").append(view.getName()).append(" ");
//
//            // get fields
//            String aliases = "";
//            String selectItems = "";
//
//            Iterator vfItr = view.getViewFields().iterator();
//            while (vfItr.hasNext()) {
//                SchemaViewField viewField = (SchemaViewField) vfItr.next();
//
//                aliases += viewField.getName();
//                selectItems += "\t" + viewField.getExpression();
//
//                if (vfItr.hasNext()) {
//                    aliases += ", ";
//                    selectItems += ",\n";
//                } else {
//                    selectItems += "\n";
//                }
//            }
//
//            // aliases
//            schema.append("(").append(aliases).append(")");
//
//            // AS SELECT
//            schema.append(" AS\n  SELECT \n").append(selectItems);
//
//            // POSTSELECT
//            schema.append("  ").append(view.getViewPostSelectClause()).append(";");
//
//            // end
//            schema.append("\n\n");
//        } // end of views

        return schema.toString();
    }

    @Override
    public String generatePostSchema(SchemaDatabase database, String[] tablesToGenerate) {
        showProgress("Generating Post SQL schema using Oracle9 renderer ...", true);
        return "";
    }

    @Override
    public void generateDropSchema(boolean addIfExists, boolean ifExistsAtEndOfStmnt, StringBuilder schema, List<SchemaTable> tablesToGenerate, List<SchemaView> viewsToGenerate) {
        for (SchemaTable table : tablesToGenerate) {
            for (SchemaTableField field : table.getFields()) {
                String sequencerName = field.getSequencerName();
                if (sequencerName != null && sequencerName.length() > 0) {
                    schema.append("DROP SEQUENCE ").append(sequencerName).append(";\n");
                }
            }
        }

        super.generateDropSchema(false, true, schema, tablesToGenerate, viewsToGenerate);
    }
}
