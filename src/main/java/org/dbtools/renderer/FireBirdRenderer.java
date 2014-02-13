/*
 * FireBirdSQLRenderer.java
 *
 * Created on February 23, 2002
 *
 * Copyright 2006 Jeff Campbell. All rights reserved. Unauthorized reproduction 
 * is a violation of applicable law. This material contains certain 
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */

/*
 * http://www.ibphoenix.com/main.nfs?a=ibphoenix&page=ibp_60_sqlref
 */
package org.dbtools.renderer;

import org.dbtools.schema.ForeignKey;
import org.dbtools.schema.schemafile.*;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Jeff Campbell
 */
public class FireBirdRenderer extends SchemaRenderer {

    public static final String RENDERER_NAME = "firebird";
    private int indexNumber = 0;

    public FireBirdRenderer() {
        super();
        this.setDbVendorName(RENDERER_NAME);
    }

    public FireBirdRenderer(PrintStream ps) {
        super(ps);
        this.setDbVendorName(RENDERER_NAME);
    }

    @Override
    public String generateSchema(SchemaDatabase database, String[] tablesToGenerate, String[] viewsToGenerate, boolean dropTables, boolean createInserts) {
        showProgress("Generating SQL schema using FireBird renderer ...", true);
        StringBuilder schema = new StringBuilder();
        List<ForeignKey> foreignKeysToCreate = new ArrayList<>();
        SchemaTableField incrementField = null;

        List<SchemaTable> requestedTables = getTablesToGenerate(database, tablesToGenerate);
        List<SchemaView> requestedViews = getViewsToGenerate(database, viewsToGenerate);

        // drop schema
        if (dropTables) {
            generateDropSchema(false, true, schema, requestedTables, requestedViews);
        }

        // create tables
        for (SchemaTable table : requestedTables) {
            // add table header
            // reset values for new table
            List<SchemaTableField> indexFields = new ArrayList<>();

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
                if (field.getSize() > 0) {
                    schema.append("(");
                    schema.append(field.getSize());

                    // check for decimals
                    if (field.getDecimals() > 0) {
                        schema.append(", ").append(field.getDecimals());
                    }

                    schema.append(")");
                }

                // not null (must come after default and before primary key)
                // look below for official NOT NULL
                if (field.isNotNull() && field.isPrimaryKey()) {
                    schema.append(" NOT NULL");
                }
                if (field.isPrimaryKey()) {
                    schema.append(" PRIMARY KEY");
                }
                if (field.isIncrement()) {
                    incrementField = field;
                }
                String defaultValue = field.getDefaultValue();
                if (defaultValue != null && !defaultValue.equals("")) {
                    schema.append(" DEFAULT ").append(formatDefaultValue(field));
                    /*
                   if (defaultValue.toUpperCase().equals("NULL")) {
                   schema.append(" DEFAULT NULL");
                   } else {
                   schema.append(" DEFAULT '");
                   schema.append(defaultValue);
                   schema.append("'");
                   }
                    */
                }

                // not null (must come after default and before primary key)
                if ((field.isNotNull() || field.isUnique()) && !field.isPrimaryKey()) // make sure any unique keys are NOT NULL
                {
                    schema.append(" NOT NULL");
                }
                if (field.isUnique()) {
                    schema.append(" UNIQUE");
                }
                if (field.isIndex()) {
                    indexFields.add(field);                // add foreign key
                }
                if (!field.getForeignKeyField().equals("")) {
                    foreignKeysToCreate.add(new ForeignKey(table.getName(), field.getName(), field.getForeignKeyTable(), field.getForeignKeyField()));
                    //inline
                    //schema.append(" REFERENCES");
                    //schema.append(" ").append(field.getForeignKeyTable());
                    //schema.append("(").append(field.getForeignKeyField()).append(")");
                }

                // if this is the last one, then don't put a ','
                if (j == fields.size() - 1) {
//                    // add index fields
//                    for (int k = 0; k < indexFields.size(); k++) {
//                        SchemaField iField = (SchemaField) indexFields.get(k);
//                        schema.append(",\n\tINDEX ").append(iField.getName()).append("_IDX (").append(iField.getName()).append(")");
//                    }
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

            String tableType = table.getParameter("tableType");
            // add table footer
            // add table type if needed
            if (tableType != null && !tableType.equals("")) {
                schema.append("\n) TYPE=");
                schema.append(tableType);
                schema.append(";\n");
            } else {
                schema.append("\n);\n");
            }

            // check to see if we need to create a generator
            if (incrementField != null) {
                schema.append("CREATE GENERATOR gen" + table.getName() + "ID;\n");
            }

            // add index fields
            for (SchemaTableField iField : indexFields) {
                // create the unique index name... limit it to 25 characters
//                int MAXLENGTH = 16;
//                String indexName = table.getName()+iField.getName();
//                int endIndex = (indexName.length() > MAXLENGTH ? MAXLENGTH : indexName.length());
//                indexName = indexName.substring(0, endIndex) + "_IDX";
                indexNumber++;
                String indexName = "IDX_" + indexNumber;
                schema.append("CREATE INDEX ").append(indexName).append(" ON " + table.getName()).append(" (").append(iField.getName()).append(");\n");
            }

            schema.append("\n\n");

            generateEnumSchema(schema, table, getAlreadyCreatedEnum(), enumPKField, enumValueField, createInserts);
        }

        // create foreign keys
        for (ForeignKey fk : foreignKeysToCreate) {
            schema.append("ALTER TABLE " + fk.getPrimaryKeyTable() + "\n");
            schema.append("ADD CONSTRAINT " + fk.getPrimaryKeyTable() + "_" + fk.getPrimaryKeyField() + "_FK\n");
            schema.append("FOREIGN KEY (" + fk.getPrimaryKeyField() + ")\n");
            schema.append("REFERENCES " + fk.getForeignKeyTable() + " (" + fk.getForeignKeyField() + ");\n");
            schema.append("\n");
        }

        // create views
//        for (SchemaView view : requestedViews) {
//            // header
//            schema.append("CREATE VIEW " + view.getName() + " ");
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
//            schema.append("(" + aliases + ")");
//
//            // AS SELECT
//            schema.append(" AS\n  SELECT \n" + selectItems);
//
//            // POSTSELECT
//            schema.append("  " + view.getViewPostSelectClause() + ";");
//
//            // end
//            schema.append("\n\n");
//        } // end of views


        return schema.toString();
    }

    @Override
    public String generatePostSchema(SchemaDatabase database, String[] tablesToGenerate) {
        //ps.println("Generating Post SQL schema using FirebirdSQL renderer ...");
        StringBuffer postSchema = new StringBuffer();
        postSchema.append("\n\n");

        List requestedTables = getTablesToGenerate(database, tablesToGenerate);
        for (int i = 0; i < requestedTables.size(); i++) {
            // get table
            SchemaTable table = (SchemaTable) requestedTables.get(i);
            boolean containsSequence = false;
            int incrementInitialValue = -1;

            List fields = table.getFields();
            Iterator fItr = fields.iterator();
            while (fItr.hasNext() && !containsSequence) {
                SchemaTableField field = (SchemaTableField) fItr.next();
                if (field.isIncrement()) {
                    containsSequence = true;
                    incrementInitialValue = field.getIncrementInitialValue();
                }
            }

            if (containsSequence) {
                //select setval ('carrier_id_seq', max(id)) from carrier
                //postSchema.append("SET GENERATOR ('"+ table.getName() +"_"+ sequenceField +"_seq', max("+ sequenceField +")) FROM "+ table.getName() + ";\n\n");
                if (incrementInitialValue == SchemaTableField.DEFAULT_INITIAL_INCREMENT) {
                    postSchema.append("SELECT GEN_ID (gen" + table.getName() + "ID, (SELECT MAX(ID) FROM " + table.getName() + ") - GEN_ID (gen" + table.getName() + "ID,0) ) FROM RDB$DATABASE;\n");
                } else {
                    postSchema.append("SELECT GEN_ID (gen" + table.getName() + "ID, " + incrementInitialValue + ") FROM RDB$DATABASE;\n");
                }
            }
        }

        return postSchema.toString();
    }
}
