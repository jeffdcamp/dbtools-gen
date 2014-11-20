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


import org.dbtools.codegen.Access;
import org.dbtools.codegen.JavaClass;
import org.dbtools.codegen.JavaMethod;
import org.dbtools.codegen.JavaVariable;
import org.dbtools.gen.AnnotationConsts;
import org.dbtools.gen.GenConfig;
import org.dbtools.schema.schemafile.SchemaEntity;
import org.dbtools.schema.schemafile.SchemaEntityType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Jeff
 */
public class AndroidBaseManagerRenderer {

    private static final String TAB = JavaClass.getTab();
    private JavaClass myClass;

    private GenConfig genConfig;

    public void generate(SchemaEntity entity, String packageName) {
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

        // constructor
        myClass.setCreateDefaultConstructor(true);

        if (!genConfig.isEncryptionSupport()) {
            myClass.addImport("org.dbtools.android.domain.AndroidBaseManager");
        } else {
            myClass.addImport("org.dbtools.android.domain.secure.AndroidBaseManager");
        }

        // both inject and non-inject must use the passed in db for database updates
        if (genConfig.isInjectionSupport()) {
            createInjectionManager(entity, packageName, recordClassName);
        } else {
            createNoInjectionManager(entity, recordClassName);
        }

        if (genConfig.isOttoSupport()) {
            JavaVariable busVariable = myClass.addVariable("com.squareup.otto.Bus", "bus", true);
            busVariable.setAccess(Access.DEFAULT_NONE);

            if (genConfig.isInjectionSupport()) {
                busVariable.addAnnotation("javax.inject.Inject");
            }
        }
    }

    private void createInjectionManager(SchemaEntity entity, String packageName, String recordClassName) {
        SchemaEntityType type = entity.getType();

        String databaseManagerPackage = packageName.substring(0, packageName.lastIndexOf('.'));
        if (genConfig.isIncludeDatabaseNameInPackage()) {
            databaseManagerPackage = databaseManagerPackage.substring(0, databaseManagerPackage.lastIndexOf('.'));
        }

        myClass.addImport(databaseManagerPackage + ".DatabaseManager");
        myClass.setExtends("AndroidBaseManager<" + recordClassName + ">");
        addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getDatabaseName", "return " + recordClassName + ".DATABASE;"));
        addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, recordClassName, "newRecord", "return new " + recordClassName + "();"));

        if (type != SchemaEntityType.QUERY) {
            addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getTableName", "return " + recordClassName + ".TABLE;"));
        }

        addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String[]", "getAllKeys", "return " + recordClassName + ".ALL_KEYS;"));

        JavaVariable databaseNameParam = new JavaVariable("String", "databaseName");
        if (genConfig.isJsr305Support()) {
            databaseNameParam.addAnnotation(AnnotationConsts.NONNULL);
        }

        addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "SQLiteDatabase", "getReadableDatabase", Arrays.asList(databaseNameParam), "return databaseManager.getReadableDatabase(databaseName);"));
        addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "SQLiteDatabase", "getReadableDatabase", null, "return databaseManager.getReadableDatabase(getDatabaseName());"));

        addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "SQLiteDatabase", "getWritableDatabase", Arrays.asList(databaseNameParam), "return databaseManager.getWritableDatabase(databaseName);"));
        addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "SQLiteDatabase", "getWritableDatabase", null, "return databaseManager.getWritableDatabase(getDatabaseName());"));

        addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "org.dbtools.android.domain.AndroidDatabase", "getAndroidDatabase", Arrays.asList(databaseNameParam), "return databaseManager.getDatabase(databaseName);"));

        JavaVariable dbManagerVariable = myClass.addVariable("DatabaseManager", "databaseManager");
        dbManagerVariable.setAccess(Access.DEFAULT_NONE);
        dbManagerVariable.addAnnotation("javax.inject.Inject");

        switch (type) {
            default:
            case TABLE:
                addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getPrimaryKey", "return " + recordClassName + "." + AndroidBaseRecordRenderer.PRIMARY_KEY_COLUMN + ";"));
                addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getDropSql", "return " + recordClassName + ".DROP_TABLE;"));
                addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getCreateSql", "return " + recordClassName + ".CREATE_TABLE;"));
                break;
            case VIEW:
                addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getPrimaryKey", "return null;"));
                addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getDropSql", "return " + recordClassName + ".DROP_VIEW;"));
                addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getCreateSql", "return " + recordClassName + ".CREATE_VIEW;"));
                break;
            case QUERY:
                JavaMethod getQueryMethod = myClass.addMethod(Access.PUBLIC, "String", "getQuery", "");
                getQueryMethod.setAbstract(true);

                addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getTableName", "return getQuery();"));
                addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getPrimaryKey", "return null;"));
                addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getDropSql", "return \"\";"));
                addMethodAnnotations(AnnotationConsts.NONNULL, myClass.addMethod(Access.PUBLIC, "String", "getCreateSql", "return \"\";"));
                break;
        }

        if (!genConfig.isEncryptionSupport()) {
            myClass.addImport("android.database.sqlite.SQLiteDatabase");
        } else {
            myClass.addImport("net.sqlcipher.database.SQLiteDatabase");
        }

        // keep save from being called
        if (entity.getType() == SchemaEntityType.VIEW || entity.getType() == SchemaEntityType.QUERY) {
            List<JavaVariable> params = new ArrayList<>();
            params.add(databaseNameParam);

            JavaVariable recordParam = new JavaVariable(recordClassName, "e");
            if (genConfig.isJsr305Support()) {
                recordParam.addAnnotation(AnnotationConsts.NONNULL);
            }

            params.add(recordParam);
            JavaMethod method = myClass.addMethod(Access.PUBLIC, "boolean", "save", params, "throw new IllegalStateException(\"Cannot call SAVE on a " + recordClassName + " View or Query\");");
            method.addAnnotation("Override");
        }
    }

    private void addMethodAnnotations(String annotation, JavaMethod javaMethod) {
        if (genConfig.isJsr305Support()) {
            javaMethod.addAnnotation(annotation);
        }
    }

    private void createNoInjectionManager(SchemaEntity entity, String recordClassName) {
        String baseManagerCall = "AndroidBaseManager.";
        myClass.addImport("android.content.ContentValues");
        myClass.addImport("android.database.Cursor");

        String dbParam = "db, ";
        myClass.addImport("android.database.sqlite.SQLiteDatabase"); // removed to allow easier swapping out of SQLiteDatabase
        List<JavaVariable> sqliteParams = new ArrayList<>();
        sqliteParams.add(new JavaVariable("SQLiteDatabase", "db"));

        if (entity.getType() == SchemaEntityType.TABLE) {
            List<JavaVariable> crudParams = new ArrayList<>();
            crudParams.add(new JavaVariable("SQLiteDatabase", "db"));
            crudParams.add(new JavaVariable(recordClassName, "record"));
            myClass.addMethod(Access.PUBLIC, "boolean", "save", crudParams, "return " + baseManagerCall + "save(db, record);").setStatic(!genConfig.isInjectionSupport());
            myClass.addMethod(Access.PUBLIC, "long", "insert", crudParams, "return " + baseManagerCall + "insert(db, record);").setStatic(!genConfig.isInjectionSupport());
            myClass.addMethod(Access.PUBLIC, "int", "update", crudParams, "return " + baseManagerCall + "update(db, record);").setStatic(!genConfig.isInjectionSupport());
            myClass.addMethod(Access.PUBLIC, "long", "delete", crudParams, "return " + baseManagerCall + "delete(db, record);").setStatic(!genConfig.isInjectionSupport());

            // UPDATE
            List<JavaVariable> updateParams3 = new ArrayList<>();
            updateParams3.add(new JavaVariable("SQLiteDatabase", "db"));
            updateParams3.add(new JavaVariable("ContentValues", "values"));
            updateParams3.add(new JavaVariable("long", "rowId"));
            myClass.addMethod(Access.PUBLIC, "int", "update", updateParams3,
                    "return " + baseManagerCall + "update(" + dbParam + recordClassName + ".TABLE, values, " + recordClassName + "." + AndroidBaseRecordRenderer.PRIMARY_KEY_COLUMN + ", rowId);").setStatic(!genConfig.isInjectionSupport());

            List<JavaVariable> updateParams4 = new ArrayList<>();
            updateParams4.add(new JavaVariable("SQLiteDatabase", "db"));
            updateParams4.add(new JavaVariable("ContentValues", "values"));
            updateParams4.add(new JavaVariable("String", "where"));
            updateParams4.add(new JavaVariable("String[]", "whereArgs"));
            myClass.addMethod(Access.PUBLIC, "int", "update", updateParams4,
                    "return " + baseManagerCall + "update(" + dbParam + recordClassName + ".TABLE, values, where, whereArgs);").setStatic(!genConfig.isInjectionSupport());

            // DELETE
            List<JavaVariable> deleteParams2 = new ArrayList<>();
            deleteParams2.add(new JavaVariable("SQLiteDatabase", "db"));
            deleteParams2.add(new JavaVariable("long", "rowId"));
            myClass.addMethod(Access.PUBLIC, "long", "delete", deleteParams2,
                    "return " + baseManagerCall + "delete(" + dbParam + recordClassName + ".TABLE, " + recordClassName + "." + AndroidBaseRecordRenderer.PRIMARY_KEY_COLUMN + ", rowId);").setStatic(!genConfig.isInjectionSupport());

            List<JavaVariable> deleteParams3 = new ArrayList<>();
            deleteParams3.add(new JavaVariable("SQLiteDatabase", "db"));
            deleteParams3.add(new JavaVariable("String", "where"));
            deleteParams3.add(new JavaVariable("String[]", "whereArgs"));
            myClass.addMethod(Access.PUBLIC, "long", "delete", deleteParams3,
                    "return " + baseManagerCall + "delete(" + dbParam + recordClassName + ".TABLE, where, whereArgs);").setStatic(!genConfig.isInjectionSupport());

            // FIND BY ROW ID
            String selectionByRowId = recordClassName + "." + AndroidBaseRecordRenderer.PRIMARY_KEY_COLUMN + "+ \"=\" + rowId";

            String findCursorByRowIdContent = "return findCursorBySelection(" + dbParam + selectionByRowId + ", null);";

            List<JavaVariable> findByRowIdParams = new ArrayList<>();
            findByRowIdParams.add(new JavaVariable("SQLiteDatabase", "db"));
            findByRowIdParams.add(new JavaVariable("long", "rowId"));
            myClass.addMethod(Access.PUBLIC, "Cursor", "findCursorByRowId", findByRowIdParams, findCursorByRowIdContent).setStatic(!genConfig.isInjectionSupport());

            // Find Object by Row ID
            String findObjectByRowIdContent = "return findBySelection(" + dbParam + selectionByRowId + ", null);\n";

            List<JavaVariable> findParams3 = new ArrayList<>();
            findParams3.add(new JavaVariable("SQLiteDatabase", "db"));
            findParams3.add(new JavaVariable("long", "rowId"));
            myClass.addMethod(Access.PUBLIC, recordClassName, "findByRowId", findParams3, findObjectByRowIdContent).setStatic(!genConfig.isInjectionSupport());

            String dropSqlContent = baseManagerCall + "executeSql(db, " + recordClassName + ".DROP_TABLE);";
            myClass.addMethod(Access.PUBLIC, "void", "dropSql", sqliteParams, dropSqlContent).setStatic(!genConfig.isInjectionSupport());

            String createSqlContent = baseManagerCall + "executeSql(db, " + recordClassName + ".CREATE_TABLE);";
            myClass.addMethod(Access.PUBLIC, "void", "createSql", sqliteParams, createSqlContent).setStatic(!genConfig.isInjectionSupport());
        } else {
            String dropSqlContent = baseManagerCall + "executeSql(db, " + recordClassName + ".DROP_VIEW);";
            myClass.addMethod(Access.PUBLIC, "void", "dropSql", sqliteParams, dropSqlContent).setStatic(!genConfig.isInjectionSupport());

            String createSqlContent = baseManagerCall + "executeSql(db, " + recordClassName + ".CREATE_VIEW);";
            myClass.addMethod(Access.PUBLIC, "void", "createSql", sqliteParams, createSqlContent).setStatic(!genConfig.isInjectionSupport());
        }

        // FIND BY SELECTION
        String findCursorBySelectionContent = "Cursor cursor = db.query(true, " + recordClassName + ".TABLE, "
                + recordClassName + ".ALL_KEYS,"
                + " selection,"
                + " null, null, null, orderBy, null);\n\n";

        findCursorBySelectionContent += "if (cursor != null) {\n";
        findCursorBySelectionContent += TAB + "if (cursor.moveToFirst()) {\n";
        findCursorBySelectionContent += TAB + TAB + "return cursor;\n";
        findCursorBySelectionContent += TAB + "}\n";
        findCursorBySelectionContent += TAB + "cursor.close();\n";
        findCursorBySelectionContent += "}\n";
        findCursorBySelectionContent += "return null;\n";

        List<JavaVariable> findParams = new ArrayList<>();
        findParams.add(new JavaVariable("SQLiteDatabase", "db"));
        findParams.add(new JavaVariable("String", "selection"));
        findParams.add(new JavaVariable("String", "orderBy"));
        myClass.addMethod(Access.PUBLIC, "Cursor", "findCursorBySelection", findParams, findCursorBySelectionContent).setStatic(!genConfig.isInjectionSupport());

        // FIND Object by selection AND order
        String findObjectBySelectionContent = "Cursor cursor = findCursorBySelection(" + dbParam + "selection, null);\n";
        findObjectBySelectionContent += "if (cursor != null) {\n";
        findObjectBySelectionContent += TAB + recordClassName + " record = new " + recordClassName + "();\n";
        findObjectBySelectionContent += TAB + "record.setContent(cursor);\n";
        findObjectBySelectionContent += TAB + "cursor.close();\n";
        findObjectBySelectionContent += TAB + "return record;\n";
        findObjectBySelectionContent += "} else {\n";
        findObjectBySelectionContent += TAB + "return null;\n";
        findObjectBySelectionContent += "}\n";

        List<JavaVariable> findParams2 = new ArrayList<>();
        findParams2.add(new JavaVariable("SQLiteDatabase", "db"));
        findParams2.add(new JavaVariable("String", "selection"));
        findParams2.add(new JavaVariable("String", "orderBy"));
        myClass.addMethod(Access.PUBLIC, recordClassName, "findBySelection", findParams2, findObjectBySelectionContent).setStatic(!genConfig.isInjectionSupport());

        // FIND Object by selection
        List<JavaVariable> findBySelectionParams = new ArrayList<>();
        findBySelectionParams.add(new JavaVariable("SQLiteDatabase", "db"));
        findBySelectionParams.add(new JavaVariable("String", "selection"));
        myClass.addMethod(Access.PUBLIC, recordClassName, "findBySelection", findBySelectionParams, "return findBySelection(db, selection, null);").setStatic(!genConfig.isInjectionSupport());

        // FIND All Object by selection AND order
        myClass.addImport("java.util.List");
        myClass.addImport("java.util.ArrayList");
        String findAllObjectBySelectionContent = "Cursor cursor = findCursorBySelection(" + dbParam + "selection, orderBy);\n";
        findAllObjectBySelectionContent += "List<" + recordClassName + "> foundItems = new ArrayList<" + recordClassName + ">();\n";
        findAllObjectBySelectionContent += "if (cursor != null) {\n";
        findAllObjectBySelectionContent += TAB + "do {\n";
        findAllObjectBySelectionContent += TAB + TAB + recordClassName + " record = new " + recordClassName + "();\n";
        findAllObjectBySelectionContent += TAB + TAB + "record.setContent(cursor);\n";
        findAllObjectBySelectionContent += TAB + TAB + "foundItems.add(record);\n";
        findAllObjectBySelectionContent += TAB + "} while (cursor.moveToNext());\n";
        findAllObjectBySelectionContent += TAB + "cursor.close();\n";
        findAllObjectBySelectionContent += "}\n";
        findAllObjectBySelectionContent += "return foundItems;\n";

        List<JavaVariable> findAllParams = new ArrayList<>();
        findAllParams.add(new JavaVariable("SQLiteDatabase", "db"));
        findAllParams.add(new JavaVariable("String", "selection"));
        findAllParams.add(new JavaVariable("String", "orderBy"));
        myClass.addMethod(Access.PUBLIC, "List<" + recordClassName + ">", "findAllBySelection", findAllParams, findAllObjectBySelectionContent).setStatic(!genConfig.isInjectionSupport());

        // FIND All Object by selection
        List<JavaVariable> findAllNoOrderByParams = new ArrayList<>();
        findAllNoOrderByParams.add(new JavaVariable("SQLiteDatabase", "db"));
        findAllNoOrderByParams.add(new JavaVariable("String", "selection"));
        myClass.addMethod(Access.PUBLIC, "List<" + recordClassName + ">", "findAllBySelection", findAllNoOrderByParams, "return findAllBySelection(" + dbParam + "selection, null);").setStatic(!genConfig.isInjectionSupport());

        // FIND COUNT
        String findCountContent = "long count = -1;\n\n";
        findCountContent += "Cursor c = db.query(" + recordClassName + ".TABLE, new String[]{\"count(1)\"}, null, null, null, null, null);\n";
        findCountContent += "if (c != null) {\n";
        findCountContent += TAB + "if (c.getCount() > 0) {\n";
        findCountContent += TAB + TAB + "c.moveToFirst();\n";
        findCountContent += TAB + TAB + "count = c.getLong(0);\n";
        findCountContent += TAB + TAB + "}\n";
        findCountContent += TAB + "c.close();\n";
        findCountContent += "}\n";
        findCountContent += "return count;";

        List<JavaVariable> findCountParams2 = new ArrayList<>();
        findCountParams2.add(new JavaVariable("SQLiteDatabase", "db"));
        myClass.addMethod(Access.PUBLIC, "long", "findCount", findCountParams2, findCountContent);
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
