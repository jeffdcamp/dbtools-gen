/*
 * AndroidRecordClassRenderer.java
 *
 * Created on Sep 9, 2010
 *
 * Copyright 2010 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.gen.android;

import org.dbtools.codegen.java.Access;
import org.dbtools.codegen.java.JavaClass;
import org.dbtools.codegen.java.JavaVariable;
import org.dbtools.gen.GenConfig;
import org.dbtools.schema.schemafile.SchemaEntity;
import org.dbtools.schema.schemafile.SchemaEntityType;
import org.dbtools.schema.schemafile.SchemaField;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Jeff
 */
public class AndroidRecordRenderer {
    private static final String TAB = JavaClass.getTab();

    private JavaClass myClass;
    private GenConfig genConfig;

    /**
     * Creates a new instance of AndroidRecordRenderer.
     */
    public AndroidRecordRenderer() {
    }

    public void generate(SchemaEntity entity, String packageName) {
        String baseClassName = AndroidBaseRecordRenderer.createClassName(false, entity.getClassName());
        String className = createClassName(entity);
        myClass = new JavaClass(packageName, className);
        myClass.setExtends(baseClassName);

        // header comment
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
        String fileHeaderComment;
        fileHeaderComment = "/*\n";
        fileHeaderComment += " * " + className + ".java\n";
        fileHeaderComment += " *\n";
        fileHeaderComment += " * Created: " + dateFormat.format(now) + "\n";
        fileHeaderComment += " */\n";
        myClass.setFileHeaderComment(fileHeaderComment);

        if (!myClass.isEnum()) {
            myClass.addImport("android.database.Cursor");
            myClass.addImport("org.dbtools.android.domain.database.contentvalues.DBToolsContentValues");
            List<JavaVariable> constructorVarsCursor = new ArrayList<>();
            constructorVarsCursor.add(new JavaVariable("Cursor", "cursor"));
            myClass.addConstructor(Access.PUBLIC, constructorVarsCursor, "setContent(cursor);");

            List<JavaVariable> constructorVarsValues = new ArrayList<>();
            constructorVarsValues.add(new JavaVariable("DBToolsContentValues", "values"));
            myClass.addConstructor(Access.PUBLIC, constructorVarsValues, "setContent(values);");
        }

        if (entity.getType() == SchemaEntityType.VIEW) {
            createView(entity);
        }

        if (entity.getType() == SchemaEntityType.QUERY) {
            createQuery(entity);
        }
    }

    private void createView(SchemaEntity entity) {
        String entityClassName = AndroidRecordRenderer.createClassName(entity) + "Const";

        myClass.addConstant("String", "DROP_VIEW", null, false);
        myClass.appendStaticInitializer("DROP_VIEW = \"DROP VIEW IF EXISTS \" + " + entityClassName + ".TABLE + \";\";");

        StringBuilder headerComment = new StringBuilder();
        headerComment.append("// todo Replace the following the CREATE_VIEW sql (The following is a template suggestion for your view)\n");
        headerComment.append("// todo SUGGESTION: Keep the \" AS ").append(entityClassName).append(".<columnname>\" portion of the sql");
        myClass.setClassHeaderComment(headerComment.toString());


        if (genConfig.isSqlQueryBuilderSupport()) {
            createSqlBuilderView(entity);
        } else {
            createStandardView(entity);
        }
    }

    private void createStandardView(SchemaEntity entity) {
        String entityClassName = AndroidRecordRenderer.createClassName(entity) + "Const";

        StringBuilder createContent = new StringBuilder();
        createContent.append("\"CREATE VIEW IF NOT EXISTS \" + ").append(entityClassName).append(".TABLE + \" AS SELECT \" +\n");

        for (int i = 0; i < entity.getFields().size(); i++) {
            if (i > 0) {
                createContent.append(" + \", \" +\n");
            }

            createContent.append(TAB).append(TAB).append(TAB);
            SchemaField schemaField = entity.getFields().get(i);

            String fieldConstName = JavaClass.formatConstant(schemaField.getName(true));
            createContent.append(entityClassName).append(".").append("FULL_C_").append(fieldConstName);
            createContent.append(" + \" AS \" + ");
            createContent.append(entityClassName).append(".").append("C_").append(fieldConstName);
        }

        createContent.append(" +\n");
        createContent.append(TAB).append(TAB).append(TAB);
        createContent.append("\" FROM \" + ").append(entityClassName).append(".TABLE");

        myClass.addConstant("String", "CREATE_VIEW", createContent.toString(), false);

        myClass.addMethod(Access.PUBLIC, "String", "getDropSql", "return DROP_VIEW;");
        myClass.addMethod(Access.PUBLIC, "String", "getCreateSql", "return CREATE_VIEW;");
    }

    private void createQuery(SchemaEntity entity) {
        if (genConfig.isSqlQueryBuilderSupport()) {
            createSQLBuilderQuery(entity);
        } else {
            createStandardQuery(entity);
        }
    }

    private void createStandardQuery(SchemaEntity entity) {
        String entityClassName = AndroidRecordRenderer.createClassName(entity) + "Const";
        StringBuilder headerComment = new StringBuilder();
        headerComment.append("// todo Replace the following the QUERY sql (The following is a template suggestion for your query)\n");
        headerComment.append("// todo BE SURE TO KEEP THE OPENING AND CLOSING PARENTHESES (so queries can be run as sub-select: select * from (select a, b from t) )\n");
        headerComment.append("// todo SUGGESTION: Keep the \" AS ").append(entityClassName).append(".<columnname>\" portion of the sql");
        myClass.setClassHeaderComment(headerComment.toString());

        StringBuilder createContent = new StringBuilder();
        createContent.append("\"(\" +\n");
        createContent.append(TAB).append(TAB).append(TAB).append("\"SELECT \" +\n");

        for (int i = 0; i < entity.getFields().size(); i++) {
            if (i > 0) {
                createContent.append(" + \", \" +\n");
            }

            createContent.append(TAB).append(TAB).append(TAB);
            SchemaField schemaField = entity.getFields().get(i);

            String fieldConstName = JavaClass.formatConstant(schemaField.getName(true));
            createContent.append(entityClassName).append(".").append("FULL_C_").append(fieldConstName);
            createContent.append(" + \" AS \" + ");
            createContent.append(entityClassName).append(".").append("C_").append(fieldConstName);
        }

        createContent.append(" +\n");
        createContent.append(TAB).append(TAB).append(TAB);
        createContent.append("\" FROM SOME TABLE(S)\" +\n");
        createContent.append(TAB).append(TAB).append(TAB);
        createContent.append("\")\"");

        myClass.addConstant("String", "QUERY", createContent.toString(), false);
        myClass.addConstant("String", "QUERY_RAW", "\"SELECT * FROM \" + QUERY", false);
    }

    private void createSqlBuilderView(SchemaEntity entity) {
        String entityConstClassName = AndroidRecordRenderer.createClassName(entity) + "Const";

        StringBuilder createContent = new StringBuilder();
        createContent.append("\"CREATE VIEW IF NOT EXISTS \" + ").append(entityConstClassName).append(".TABLE + \" AS \" +\n");

        createContent.append(TAB).append(TAB).append(TAB);
        myClass.addImport("org.dbtools.query.sql.SQLQueryBuilder");
        createContent.append("new SQLQueryBuilder()").append("\n");


        for (int i = 0; i < entity.getFields().size(); i++) {
            if (i > 0) {
                createContent.append("\n");
            }

            createContent.append(TAB).append(TAB).append(TAB);
            SchemaField schemaField = entity.getFields().get(i);

            String fieldConstName = JavaClass.formatConstant(schemaField.getName(true));
            createContent.append(".field(");

            createContent.append(entityConstClassName).append(".").append("FULL_C_").append(fieldConstName);
            createContent.append(", ");
            createContent.append(entityConstClassName).append(".").append("C_").append(fieldConstName);
            createContent.append(")");
        }

        createContent.append("\n");
        createContent.append(TAB).append(TAB).append(TAB);
        createContent.append(".table(").append(entityConstClassName).append(".TABLE)");
        createContent.append("\n");
        createContent.append(TAB).append(TAB).append(TAB);
        createContent.append(".buildQuery()");

        myClass.addConstant("String", "CREATE_VIEW", null, false);
        myClass.appendStaticInitializer("CREATE_VIEW = " + createContent.toString() + ";");

        myClass.addMethod(Access.PUBLIC, "String", "getDropSql", "return DROP_VIEW;");
        myClass.addMethod(Access.PUBLIC, "String", "getCreateSql", "return CREATE_VIEW;");

    }

    private void createSQLBuilderQuery(SchemaEntity entity) {
        String entityClassName = AndroidRecordRenderer.createClassName(entity) + "Const";
        StringBuilder headerComment = new StringBuilder();
        headerComment.append("// todo Replace the following the QUERY sql (The following is a template suggestion for your query)\n");
        headerComment.append("// todo SUGGESTION: Keep the second parameter of each filter(<replace>, <keep>)");
        myClass.setClassHeaderComment(headerComment.toString());

        StringBuilder createContent = new StringBuilder();
        myClass.addImport("org.dbtools.query.sql.SQLQueryBuilder");
        createContent.append("new SQLQueryBuilder()").append("\n");


        for (int i = 0; i < entity.getFields().size(); i++) {
            if (i > 0) {
                createContent.append("\n");
            }

            createContent.append(TAB).append(TAB).append(TAB);
            SchemaField schemaField = entity.getFields().get(i);

            String fieldConstName = JavaClass.formatConstant(schemaField.getName(true));
            createContent.append(".field(");
            createContent.append(entityClassName).append(".").append("FULL_C_").append(fieldConstName);
            createContent.append(", ");
            createContent.append(entityClassName).append(".").append("C_").append(fieldConstName);
            createContent.append(")");
        }

        createContent.append("\n");
        createContent.append(TAB).append(TAB).append(TAB);
        createContent.append(".table(\"FROM SOME TABLE(S)\")\n");
        createContent.append(".buildQuery();\n");

        myClass.addConstant("String", "QUERY", null, false);
        myClass.appendStaticInitializer("QUERY = " + createContent.toString());
    }

    public static String createClassName(SchemaEntity entity) {
        return entity.getClassName();
    }

    public void writeToFile(String directoryName) {
        myClass.writeToDisk(directoryName);
    }

    public void setGenConfig(GenConfig genConfig) {
        this.genConfig = genConfig;
    }
}
