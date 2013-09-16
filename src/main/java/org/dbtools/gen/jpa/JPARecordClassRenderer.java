/*
 * JPARecordClassRenderer.java
 *
 * Created on November  2, 2002
 *
 * Copyright 2007 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.gen.jpa;

import org.dbtools.codegen.Access;
import org.dbtools.codegen.JavaClass;
import org.dbtools.codegen.JavaMethod;
import org.dbtools.codegen.JavaVariable;
import org.dbtools.schema.SchemaDatabase;
import org.dbtools.schema.SchemaTable;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Jeff
 */
public class JPARecordClassRenderer {

    private JavaClass myClass;
    private JavaClass myTestClass;
    private boolean uselegacyJUnit = false;

    /**
     * Creates a new instance of JPARecordClassRenderer.
     */
    public JPARecordClassRenderer() {
    }

    public void generateObjectCode(SchemaDatabase dbSchema, SchemaTable table, String packageName, String author, String version, PrintStream psLog) {
        if (psLog == null) {
            psLog = System.out;
        }
//        psLog.println("Generating Record class...");

        String baseClassName = JPABaseRecordClassRenderer.createClassName(table);
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

        // JPA
        myClass.addAnnotation("@javax.persistence.Entity()");
        //myClass.addImplements("java.io.Serializable");
        myClass.addImport("javax.persistence.SchemaTable");
        myClass.addAnnotation("@SchemaTable(name=" + baseClassName + ".TABLE)");

//        psLog.println("Generation complete!");
    }

    private void initTestClass(String className) {
        if (uselegacyJUnit) {
            myTestClass.addImport("junit.framework.*");
            myTestClass.setExtends("TestCase");
        } else {
            myTestClass.addImport("org.junit.*");
            myTestClass.addImport("static org.junit.Assert.*");
        }


        if (uselegacyJUnit) {
            List<JavaVariable> params = new ArrayList<JavaVariable>();
            params.add(new JavaVariable("String", "testName"));
            myTestClass.addConstructor(Access.PUBLIC, params, "super(testName);");
        } else {
            myTestClass.setCreateDefaultConstructor(true);
        }

        // variables
        myTestClass.addVariable(className, "testRecord");


        // methods
        JavaMethod setUpMethod = myTestClass.addMethod(Access.PUBLIC, "void", "setUp", "testRecord = new " + className + "();\nassertNotNull(testRecord);");
        if (!uselegacyJUnit) {
            setUpMethod.addAnnotation("Before");
        }

        JavaMethod tearDownMethod = myTestClass.addMethod(Access.PUBLIC, "void", "tearDown", null);
        if (!uselegacyJUnit) {
            tearDownMethod.addAnnotation("After");
        }

        // create empty test to get rid of warnings
        if (uselegacyJUnit) {
            myTestClass.addMethod(Access.PUBLIC, "void", "testEmpty", "");
        } else {
            JavaMethod emptyTestMethod = myTestClass.addMethod(Access.PUBLIC, "void", "emptyTest", "");
            emptyTestMethod.addAnnotation("Test");
        }
    }

    public static String createClassName(SchemaTable table) {
        return table.getClassName();
    }

    public String getFilename() {
        return myClass.getFilename();
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
