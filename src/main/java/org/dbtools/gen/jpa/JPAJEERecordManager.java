/*
 * JPAJEERecordManager.java
 *
 * Created on February 24, 2007, 11:03 AM
 *
 * Copyright 2007 Jeff Campbell. All rights reserved. Unauthorized reproduction 
 * is a violation of applicable law. This material contains certain 
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */

package org.dbtools.gen.jpa;

import org.dbtools.codegen.JavaClass;
import org.dbtools.schema.SchemaTable;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Jeff
 */
public class JPAJEERecordManager {

    private JavaClass myClass;

    private boolean localInterfaceRequired = false;
    private boolean remoteInterfaceRequired = false;

    /**
     * Creates a new instance of JPAJEERecordManager
     */
    public JPAJEERecordManager() {
    }

    public void generateObjectCode(SchemaTable table, String packageName, String author, String version, PrintStream psLog) {
        String className = getClassName(table);
        myClass = new JavaClass(packageName, className);
        myClass.setExtends(JPAJEEBaseRecordManager.getClassName(table));

        if (isLocalInterfaceRequired()) {
            myClass.addImplements(JPAJEEBaseRecordManager.getLocalInterfaceName(table));
        }

        if (isRemoteInterfaceRequired()) {
            myClass.addImplements(JPAJEEBaseRecordManager.getRemoteInterfaceName(table));
        }


        myClass.addImport("javax.ejb.Stateless");
        myClass.addAnnotation("Stateless");

        // header comment
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
        String fileHeaderComment;
        fileHeaderComment = "/*\n";
        fileHeaderComment += " * " + className + ".java\n";
        fileHeaderComment += " *\n";
        fileHeaderComment += " * Created Generated: " + dateFormat.format(now) + "\n";
        fileHeaderComment += " *\n";
        fileHeaderComment += " */\n";
        myClass.setFileHeaderComment(fileHeaderComment);

        // constructor(s)

        // variables
    }

    public static String getClassName(SchemaTable table) {
        String recordClassName = JPARecordClassRenderer.createClassName(table);
        return recordClassName + "Manager";
    }

    public void writeToFile(String outDir) {
        myClass.writeToDisk(outDir);
    }

    public boolean isLocalInterfaceRequired() {
        return localInterfaceRequired;
    }

    public void setLocalInterfaceRequired(boolean localInterfaceRequired) {
        this.localInterfaceRequired = localInterfaceRequired;
    }

    public boolean isRemoteInterfaceRequired() {
        return remoteInterfaceRequired;
    }

    public void setRemoteInterfaceRequired(boolean remoteInterfaceRequired) {
        this.remoteInterfaceRequired = remoteInterfaceRequired;
    }

}
