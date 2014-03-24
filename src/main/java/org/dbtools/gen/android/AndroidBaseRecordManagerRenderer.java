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
import org.dbtools.codegen.JavaVariable;
import org.dbtools.schema.schemafile.SchemaEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Jeff
 */
public class AndroidBaseRecordManagerRenderer {

    private JavaClass myClass;

    private boolean injectionSupport = false;
    private boolean encryptionSupport = false; // use SQLCipher

    public void generate(SchemaEntity table, String packageName) {
        String recordClassName = AndroidRecordClassRenderer.createClassName(table);
        String className = getClassName(table);
        myClass = new JavaClass(packageName, className);

        // header comment
        // Do not place date in file because it will cause a new check-in to scm        
        String fileHeaderComment;
        fileHeaderComment = "/*\n";
        fileHeaderComment += " * " + className + ".java\n";
        fileHeaderComment += " *\n";
        fileHeaderComment += " * GENERATED FILE - DO NOT EDIT\n";
        fileHeaderComment += " * CHECKSTYLE:OFF\n";
        fileHeaderComment += " * \n";
        fileHeaderComment += " */\n";
        myClass.setFileHeaderComment(fileHeaderComment);

        // Since this is generated code.... suppress all warnings
        myClass.addAnnotation("@SuppressWarnings(\"all\")");

        // constructor
        myClass.setCreateDefaultConstructor(true);

        if (!encryptionSupport) {
            myClass.addImport("org.dbtools.android.domain.AndroidBaseManager");
        } else {
            myClass.addImport("org.dbtools.android.domain.secure.AndroidBaseManager");
        }

        // both inject and non-inject must use the passed in db for database updates
        if (injectionSupport) {
            createInjectionManager(packageName, recordClassName);
        } else {
            createNoInjectionManager(recordClassName);
        }
    }

    private void createInjectionManager(String packageName, String recordClassName) {
        myClass.setExtends("AndroidBaseManager<" + recordClassName + ">");
        myClass.addMethod(Access.PUBLIC, "String", "getDatabaseName", "return " + recordClassName + ".DATABASE;");
        myClass.addMethod(Access.PUBLIC, "String", "getTableName", "return " + recordClassName + ".TABLE;");
        myClass.addMethod(Access.PUBLIC, "String", "getPrimaryKey", "return " + recordClassName + "." + AndroidBaseRecordClassRenderer.PRIMARY_KEY_COLUMN + ";");
        myClass.addMethod(Access.PUBLIC, "String[]", "getAllKeys", "return " + recordClassName + ".ALL_KEYS;");
        myClass.addMethod(Access.PUBLIC, "String", "getDropTableSQL", "return " + recordClassName + ".DROP_TABLE;");
        myClass.addMethod(Access.PUBLIC, "String", "getCreateTableSQL", "return " + recordClassName + ".CREATE_TABLE;");
        myClass.addMethod(Access.PUBLIC, recordClassName, "newRecord", "return new " + recordClassName + "();");

        myClass.addImport(packageName.substring(0, packageName.lastIndexOf('.')) + ".DatabaseManager");
        JavaVariable dbManagerVariable = myClass.addVariable("DatabaseManager", "databaseManager");
        dbManagerVariable.setAccess(Access.DEFAULT_NONE);
        dbManagerVariable.addAnnotation("javax.inject.Inject");

        myClass.addMethod(Access.PUBLIC, "SQLiteDatabase", "getReadableDatabase", Arrays.asList(new JavaVariable("String", "databaseName")), "return databaseManager.getReadableDatabase(databaseName);");
        myClass.addMethod(Access.PUBLIC, "SQLiteDatabase", "getWritableDatabase", Arrays.asList(new JavaVariable("String", "databaseName")), "return databaseManager.getWritableDatabase(databaseName);");

        if (!encryptionSupport) {
            myClass.addImport("android.database.sqlite.SQLiteDatabase");
        } else {
            myClass.addImport("net.sqlcipher.database.SQLiteDatabase");
        }
    }

    private void createNoInjectionManager(String recordClassName) {
        String TAB = JavaClass.getTab();

        String baseManagerCall = "AndroidBaseManager.";
        myClass.addImport("android.content.ContentValues");
        myClass.addImport("android.database.Cursor");

        String dbParam = "db, ";
        myClass.addImport("android.database.sqlite.SQLiteDatabase"); // removed to allow easier swapping out of SQLiteDatabase
        List<JavaVariable> sqliteParams = new ArrayList<>();
        sqliteParams.add(new JavaVariable("SQLiteDatabase", "db"));
        String dropTableContent = baseManagerCall + "executeSQL(db, " + recordClassName + ".DROP_TABLE);";
        myClass.addMethod(Access.PUBLIC, "void", "dropTable", sqliteParams, dropTableContent).setStatic(!injectionSupport);

        String createTableContent = baseManagerCall + "executeSQL(db, " + recordClassName + ".CREATE_TABLE);";
        myClass.addMethod(Access.PUBLIC, "void", "createTable", sqliteParams, createTableContent).setStatic(!injectionSupport);

        List<JavaVariable> crudParams = new ArrayList<>();
        crudParams.add(new JavaVariable("SQLiteDatabase", "db"));
        crudParams.add(new JavaVariable(recordClassName, "record"));
        myClass.addMethod(Access.PUBLIC, "boolean", "save", crudParams, "return " + baseManagerCall + "save(db, record);").setStatic(!injectionSupport);
        myClass.addMethod(Access.PUBLIC, "long", "insert", crudParams, "return " + baseManagerCall + "insert(db, record);").setStatic(!injectionSupport);
        myClass.addMethod(Access.PUBLIC, "int", "update", crudParams, "return " + baseManagerCall + "update(db, record);").setStatic(!injectionSupport);
        myClass.addMethod(Access.PUBLIC, "long", "delete", crudParams, "return " + baseManagerCall + "delete(db, record);").setStatic(!injectionSupport);

        // UPDATE
        List<JavaVariable> updateParams3 = new ArrayList<>();
        updateParams3.add(new JavaVariable("SQLiteDatabase", "db"));
        updateParams3.add(new JavaVariable("ContentValues", "values"));
        updateParams3.add(new JavaVariable("long", "rowID"));
        myClass.addMethod(Access.PUBLIC, "int", "update", updateParams3,
                "return " + baseManagerCall + "update(" + dbParam + recordClassName + ".TABLE, values, " + recordClassName + "." + AndroidBaseRecordClassRenderer.PRIMARY_KEY_COLUMN + ", rowID);").setStatic(!injectionSupport);

        List<JavaVariable> updateParams4 = new ArrayList<>();
        updateParams4.add(new JavaVariable("SQLiteDatabase", "db"));
        updateParams4.add(new JavaVariable("ContentValues", "values"));
        updateParams4.add(new JavaVariable("String", "where"));
        updateParams4.add(new JavaVariable("String[]", "whereArgs"));
        myClass.addMethod(Access.PUBLIC, "int", "update", updateParams4,
                "return " + baseManagerCall + "update(" + dbParam + recordClassName + ".TABLE, values, where, whereArgs);").setStatic(!injectionSupport);

        // DELETE
        List<JavaVariable> deleteParams2 = new ArrayList<>();
        deleteParams2.add(new JavaVariable("SQLiteDatabase", "db"));
        deleteParams2.add(new JavaVariable("long", "rowID"));
        myClass.addMethod(Access.PUBLIC, "long", "delete", deleteParams2,
                "return " + baseManagerCall + "delete(" + dbParam + recordClassName + ".TABLE, " + recordClassName + "." + AndroidBaseRecordClassRenderer.PRIMARY_KEY_COLUMN + ", rowID);").setStatic(!injectionSupport);

        List<JavaVariable> deleteParams3 = new ArrayList<>();
        deleteParams3.add(new JavaVariable("SQLiteDatabase", "db"));
        deleteParams3.add(new JavaVariable("String", "where"));
        deleteParams3.add(new JavaVariable("String[]", "whereArgs"));
        myClass.addMethod(Access.PUBLIC, "long", "delete", deleteParams3,
                "return " + baseManagerCall + "delete(" + dbParam + recordClassName + ".TABLE, where, whereArgs);").setStatic(!injectionSupport);

        String dbVar = "db";

        // FIND BY SELECTION
        String findCursorBySelectionContent = "Cursor cursor = " + dbVar + ".query(true, " + recordClassName + ".TABLE, "
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
        myClass.addMethod(Access.PUBLIC, "Cursor", "findCursorBySelection", findParams, findCursorBySelectionContent).setStatic(!injectionSupport);

        // FIND BY ROW ID
        String selectionByRowID = recordClassName + "." + AndroidBaseRecordClassRenderer.PRIMARY_KEY_COLUMN +  "+ \"=\" + rowID";

        String findCursorByRowIDContent = "return findCursorBySelection(" + dbParam + selectionByRowID + ", null);";

        List<JavaVariable> findByRowIdParams = new ArrayList<>();
        findByRowIdParams.add(new JavaVariable("SQLiteDatabase", "db"));
        findByRowIdParams.add(new JavaVariable("long", "rowID"));
        myClass.addMethod(Access.PUBLIC, "Cursor", "findCursorByRowID", findByRowIdParams, findCursorByRowIDContent).setStatic(!injectionSupport);

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
        myClass.addMethod(Access.PUBLIC, recordClassName, "findBySelection", findParams2, findObjectBySelectionContent).setStatic(!injectionSupport);

        // FIND Object by selection
        List<JavaVariable> findBySelectionParams = new ArrayList<>();
        findBySelectionParams.add(new JavaVariable("SQLiteDatabase", "db"));
        findBySelectionParams.add(new JavaVariable("String", "selection"));
        myClass.addMethod(Access.PUBLIC, recordClassName, "findBySelection", findBySelectionParams, "return findBySelection(db, selection, null);").setStatic(!injectionSupport);

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
        myClass.addMethod(Access.PUBLIC, "List<" + recordClassName + ">", "findAllBySelection", findAllParams, findAllObjectBySelectionContent).setStatic(!injectionSupport);

        // FIND All Object by selection
        List<JavaVariable> findAllNoOrderByParams = new ArrayList<>();
        findAllNoOrderByParams.add(new JavaVariable("SQLiteDatabase", "db"));
        findAllNoOrderByParams.add(new JavaVariable("String", "selection"));
        myClass.addMethod(Access.PUBLIC, "List<" + recordClassName + ">", "findAllBySelection", findAllNoOrderByParams, "return findAllBySelection(" + dbParam + "selection, null);").setStatic(!injectionSupport);

        // Find Object by Row ID
        String findObjectByRowIDContent = "return findBySelection(" + dbParam + selectionByRowID + ", null);\n";

        List<JavaVariable> findParams3 = new ArrayList<>();
        findParams3.add(new JavaVariable("SQLiteDatabase", "db"));
        findParams3.add(new JavaVariable("long", "rowID"));
        myClass.addMethod(Access.PUBLIC, recordClassName, "findByRowID", findParams3, findObjectByRowIDContent).setStatic(!injectionSupport);

        // FIND COUNT
        String findCountContent = "long count = -1;\n\n";
        findCountContent += "Cursor c = " + dbVar + ".query(" + recordClassName + ".TABLE, new String[]{\"count(1)\"}, null, null, null, null, null);\n";
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
        String recordClassName = AndroidRecordClassRenderer.createClassName(table);
        return recordClassName + "BaseManager";
    }

    public void writeToFile(String outDir) {
        myClass.writeToDisk(outDir);
    }

    public void setInjectionSupport(boolean injectionSupport) {
        this.injectionSupport = injectionSupport;
    }

    public void setEncryptionSupport(boolean encryptionSupport) {
        this.encryptionSupport = encryptionSupport;
    }
}
