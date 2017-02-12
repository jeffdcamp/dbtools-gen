/*
 * AndroidRecordManager.java
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
import org.dbtools.codegen.java.JavaMethod;
import org.dbtools.codegen.java.JavaVariable;
import org.dbtools.gen.GenConfig;
import org.dbtools.schema.schemafile.SchemaEntity;
import org.dbtools.schema.schemafile.SchemaEntityType;
import org.dbtools.schema.schemafile.SchemaField;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * @author Jeff
 */
public class AndroidManagerRenderer {
    private static final String TAB = JavaClass.getTab();

    private JavaClass myClass;

    private GenConfig genConfig;

    /**
     * Creates a new instance of AndroidManagerRenderer.
     */
    public void generate(SchemaEntity entity, String packageName) {
        String className = getClassName(entity);
        myClass = new JavaClass(packageName, className);
        myClass.setExtends(AndroidBaseManagerRenderer.getClassName(entity)); // extend the generated base class

        // header comment
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
        String fileHeaderComment;
        fileHeaderComment = "/*\n";
        fileHeaderComment += " * " + className + ".java\n";
        fileHeaderComment += " *\n";
        fileHeaderComment += " * Generated on: " + dateFormat.format(now) + "\n";
        fileHeaderComment += " *\n";
        fileHeaderComment += " */\n";
        myClass.setFileHeaderComment(fileHeaderComment);

        // Injection support
        if (genConfig.isInjectionSupport()) {
            myClass.addAnnotation("javax.inject.Singleton");
        }

        // constructor
        String databaseManagerPackage = packageName.substring(0, packageName.lastIndexOf('.'));
        if (genConfig.isIncludeDatabaseNameInPackage()) {
            databaseManagerPackage = databaseManagerPackage.substring(0, databaseManagerPackage.lastIndexOf('.'));
        }
        myClass.addImport(databaseManagerPackage + ".DatabaseManager");
        myClass.setCreateDefaultConstructor(false);

        JavaVariable constructorParam = new JavaVariable("DatabaseManager", "databaseManager");
        JavaMethod defaultConstructor = myClass.addConstructor(Access.PUBLIC, Arrays.asList(constructorParam), "super(databaseManager);");
        if (genConfig.isInjectionSupport()) {
            defaultConstructor.addAnnotation("javax.inject.Inject");
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

        myClass.addConstant("String", "DROP_VIEW", "\"DROP VIEW IF EXISTS \" + " + entityClassName + ".TABLE + \";\";", false);

        // begin header
        StringBuilder headerComment = new StringBuilder();
        headerComment.append("// todo Replace the following the CREATE_VIEW sql (The following is a template suggestion for your view)\n");


        if (genConfig.isSqlQueryBuilderSupport()) {
            createSqlBuilderView(entity);
            headerComment.append("// todo SUGGESTION: Keep the second parameter of each filter(<replace>, <keep>)");
        } else {
            createStandardView(entity);
            headerComment.append("// todo SUGGESTION: Keep the \" AS ").append(entityClassName).append(".<columnname>\" portion of the sql");
        }

        myClass.setClassHeaderComment(headerComment.toString());
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
    }

    private void createQuery(SchemaEntity entity) {
        String recordClassName = AndroidRecordRenderer.createClassName(entity);
        myClass.addMethod(Access.PUBLIC, "String", "getQuery", "return QUERY;").addAnnotation("Override");

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
        headerComment.append("// todo SUGGESTION: Keep the \" AS ").append(entityClassName).append(".<columnname>\" portion of the sql");
        myClass.setClassHeaderComment(headerComment.toString());

        StringBuilder createContent = new StringBuilder();
        createContent.append(" \"SELECT \" +\n");

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
        createContent.append("\" FROM SOME TABLE(S)\"");

        myClass.addConstant("String", "QUERY", createContent.toString(), false);
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

        myClass.addConstant("String", "CREATE_VIEW", createContent.toString(), false);

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
        createContent.append(".table(\"SOME TABLE\")\n");
        createContent.append(TAB).append(TAB).append(TAB);
        createContent.append(".buildQuery()");

        myClass.addConstant("String", "QUERY", createContent.toString(), false);
    }

    public static String getClassName(SchemaEntity entity) {
        String recordClassName = AndroidRecordRenderer.createClassName(entity);
        return recordClassName + "Manager";
    }

    public void writeToFile(String outDir) {
        myClass.writeToDisk(outDir);
    }

    public void setGenConfig(GenConfig genConfig) {
        this.genConfig = genConfig;
    }
}
