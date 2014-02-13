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

import org.dbtools.codegen.Access;
import org.dbtools.codegen.JavaClass;
import org.dbtools.codegen.JavaMethod;
import org.dbtools.codegen.JavaVariable;
import org.dbtools.schema.schemafile.SchemaTable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Jeff
 */
public class AndroidRecordClassRenderer {

    private JavaClass myClass;
    private JavaClass myTestClass;

    /**
     * Creates a new instance of AndroidRecordClassRenderer.
     */
    public AndroidRecordClassRenderer() {
    }

    public void generate(SchemaTable table, String packageName) {
        String baseClassName = AndroidBaseRecordClassRenderer.createClassName(table);
        String className = createClassName(table);
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
            myClass.addImport("android.content.ContentValues");
            List<JavaVariable> constructorVarsCursor = new ArrayList<>();
            constructorVarsCursor.add(new JavaVariable("Cursor", "cursor"));
            myClass.addConstructor(Access.PUBLIC, constructorVarsCursor, "setContent(cursor);");

            List<JavaVariable> constructorVarsValues = new ArrayList<>();
            constructorVarsValues.add(new JavaVariable("ContentValues", "values"));
            myClass.addConstructor(Access.PUBLIC, constructorVarsValues, "setContent(values);");
        }
    }

    private void initTestClass(String className) {
        myTestClass.addImport("org.junit.*");
        myTestClass.addImport("static org.junit.Assert.*");
        myTestClass.setCreateDefaultConstructor(true);

        // variables
        myTestClass.addVariable(className, "testRecord");


        // methods
        JavaMethod setUpMethod = myTestClass.addMethod(Access.PUBLIC, "void", "setUp", "testRecord = new " + className + "();\nassertNotNull(testRecord);");
        setUpMethod.addAnnotation("Before");

        JavaMethod tearDownMethod = myTestClass.addMethod(Access.PUBLIC, "void", "tearDown", null);
        tearDownMethod.addAnnotation("After");

        // create empty test to get rid of warnings
        JavaMethod emptyTestMethod = myTestClass.addMethod(Access.PUBLIC, "void", "emptyTest", "");
        emptyTestMethod.addAnnotation("Test");
    }

    public static String createClassName(SchemaTable table) {
        return table.getClassName();
    }

    public void writeToFile(String directoryname) {
        myClass.writeToDisk(directoryname);
    }

    public void writeTestsToFile(String directoryname, SchemaTable table, String packageName) {
        String className = createClassName(table);
        myTestClass = new JavaClass(packageName, className + "Test");
        initTestClass(className);

        myTestClass.writeToDisk(directoryname);
    }
}
