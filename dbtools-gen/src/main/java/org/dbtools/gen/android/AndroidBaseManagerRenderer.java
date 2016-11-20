/*
 * AndroidBaseRecordManager.java
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
import org.dbtools.gen.AnnotationConsts;
import org.dbtools.gen.GenConfig;
import org.dbtools.schema.schemafile.SchemaEntity;
import org.dbtools.schema.schemafile.SchemaEntityType;
import org.dbtools.schema.schemafile.SchemaTable;

import java.util.Arrays;

/**
 * @author Jeff
 */
public class AndroidBaseManagerRenderer {

    private JavaClass myClass;
    private GenConfig genConfig;

    public void generate(SchemaEntity entity, String packageName, AndroidGeneratedEntityInfo generatedEntityInfo) {
        String recordClassName = AndroidRecordRenderer.createClassName(entity);
        String className = getClassName(entity);
        myClass = new JavaClass(packageName, className);
        myClass.setAbstract(true);

        // header comment
        // Do not place date in file because it will cause a new check-in to scm        
        String fileHeaderComment;
        fileHeaderComment = "/*\n";
        fileHeaderComment += " * " + className + ".java\n";
        fileHeaderComment += " *\n";
        fileHeaderComment += " * GENERATED FILE - DO NOT EDIT\n";
        fileHeaderComment += " * \n";
        fileHeaderComment += " */\n";
        myClass.setFileHeaderComment(fileHeaderComment);

        // Since this is generated code.... suppress all warnings
        myClass.addAnnotation("@SuppressWarnings(\"all\")");

        // generate all of the main methods
        createManager(entity, packageName, recordClassName, generatedEntityInfo);
    }

    private void createManager(SchemaEntity entity, String packageName, String recordClassName, AndroidGeneratedEntityInfo generatedEntityInfo) {
        String recordConstClassName = recordClassName + "Const";
        SchemaEntityType type = entity.getType();

        String databaseManagerPackage = packageName.substring(0, packageName.lastIndexOf('.'));
        if (genConfig.isIncludeDatabaseNameInPackage()) {
            databaseManagerPackage = databaseManagerPackage.substring(0, databaseManagerPackage.lastIndexOf('.'));
        }

        myClass.addImport(databaseManagerPackage + ".DatabaseManager");
        myClass.addImport("org.dbtools.android.domain.database.DatabaseWrapper");

        switch (type) {
            case TABLE:
                SchemaTable tableEntity = (SchemaTable) entity;
                if (tableEntity.isReadonly()) {
                    myClass.addImport(genConfig.isRxJavaSupport() ? "org.dbtools.android.domain.RxAndroidBaseManagerReadOnly" : "org.dbtools.android.domain.AndroidBaseManagerReadOnly");
                    myClass.setExtends(genConfig.isRxJavaSupport() ? "RxAndroidBaseManagerReadOnly<" + recordClassName + ">" : "AndroidBaseManagerReadOnly<" + recordClassName + ">");
                } else {
                    myClass.addImport(genConfig.isRxJavaSupport() ? "org.dbtools.android.domain.RxAndroidBaseManagerWritable" : "org.dbtools.android.domain.AndroidBaseManagerWritable");
                    myClass.setExtends(genConfig.isRxJavaSupport() ? "RxAndroidBaseManagerWritable<" + recordClassName + ">" : "AndroidBaseManagerWritable<" + recordClassName + ">");
                }
                break;
            case VIEW:
            case QUERY:
                myClass.addImport(genConfig.isRxJavaSupport() ? "org.dbtools.android.domain.RxAndroidBaseManagerReadOnly" : "org.dbtools.android.domain.AndroidBaseManagerReadOnly");
                myClass.setExtends(genConfig.isRxJavaSupport() ? "RxAndroidBaseManagerReadOnly<" + recordClassName + ">" : "AndroidBaseManagerReadOnly<" + recordClassName + ">");
                break;
        }

        addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getDatabaseName", "return " + recordConstClassName + ".DATABASE;"));
        addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, recordClassName, "newRecord", "return new " + recordClassName + "();"));

        if (type != SchemaEntityType.QUERY) {
            addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getTableName", "return " + recordConstClassName + ".TABLE;"));
        }

        addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String[]", "getAllColumns", "return " + recordConstClassName + ".ALL_COLUMNS;"));

        JavaVariable databaseNameParam = new JavaVariable("String", "databaseName");
        if (genConfig.isJsr305Support()) {
            databaseNameParam.addAnnotation(AnnotationConsts.NONNULL);
        }

        addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "DatabaseWrapper", "getReadableDatabase", Arrays.asList(databaseNameParam), "return databaseManager.getReadableDatabase(databaseName);"));
        addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "DatabaseWrapper", "getReadableDatabase", null, "return databaseManager.getReadableDatabase(getDatabaseName());"));

        addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "DatabaseWrapper", "getWritableDatabase", Arrays.asList(databaseNameParam), "return databaseManager.getWritableDatabase(databaseName);"));
        addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "DatabaseWrapper", "getWritableDatabase", null, "return databaseManager.getWritableDatabase(getDatabaseName());"));

        addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "org.dbtools.android.domain.AndroidDatabase", "getAndroidDatabase", Arrays.asList(databaseNameParam), "return databaseManager.getDatabase(databaseName);"));

        // constructor
        myClass.setCreateDefaultConstructor(false);
        myClass.addVariable("DatabaseManager", "databaseManager");
        JavaVariable constructorParam = new JavaVariable("DatabaseManager", "databaseManager");
        myClass.addConstructor(Access.PUBLIC, Arrays.asList(constructorParam), "this.databaseManager = databaseManager;");

        myClass.addMethod(Access.PUBLIC, "org.dbtools.android.domain.config.DatabaseConfig", "getDatabaseConfig", "return databaseManager.getDatabaseConfig();");

        switch (type) {
            default:
            case TABLE:
                if (generatedEntityInfo.isPrimaryKeyAdded()) {
                    addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getPrimaryKey", "return " + recordConstClassName + "." + AndroidBaseRecordRenderer.PRIMARY_KEY_COLUMN + ";"));
                } else {
                    addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getPrimaryKey", "return \"NO_PRIMARY_KEY\";"));
                }
                addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getDropSql", "return " + recordConstClassName + ".DROP_TABLE;"));
                addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getCreateSql", "return " + recordConstClassName + ".CREATE_TABLE;"));
                addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getInsertSql", "return " + recordConstClassName + ".INSERT_STATEMENT;"));
                addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getUpdateSql", "return " + recordConstClassName + ".UPDATE_STATEMENT;"));
                break;
            case VIEW:
                addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getPrimaryKey", "return null;"));
                addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getDropSql", "return " + recordClassName + ".DROP_VIEW;"));
                addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getCreateSql", "return " + recordClassName + ".CREATE_VIEW;"));
                addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getInsertSql", "return \"\";"));
                addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getUpdateSql", "return \"\";"));
                break;
            case QUERY:
                JavaMethod getQueryMethod = myClass.addMethod(Access.PUBLIC, "String", "getQuery", "");
                getQueryMethod.setAbstract(true);

                addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getTableName", "return getQuery();"));
                addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getPrimaryKey", "return null;"));
                addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getDropSql", "return \"\";"));
                addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getCreateSql", "return \"\";"));
                addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getInsertSql", "return \"\";"));
                addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getUpdateSql", "return \"\";"));
                break;
        }
    }

    private void addMethodAnnotations(String annotation, JavaMethod javaMethod) {
        if (genConfig.isJsr305Support()) {
            javaMethod.addAnnotation(annotation);
        }
    }

    public static String getClassName(SchemaEntity table) {
        String recordClassName = AndroidRecordRenderer.createClassName(table);
        return recordClassName + "BaseManager";
    }

    public void writeToFile(String outDir) {
        myClass.writeToDisk(outDir);
    }

    public void setGenConfig(GenConfig genConfig) {
        this.genConfig = genConfig;
    }
}
