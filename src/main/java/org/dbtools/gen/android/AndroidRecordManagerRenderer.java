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

import org.dbtools.codegen.Access;
import org.dbtools.codegen.JavaClass;
import org.dbtools.codegen.JavaVariable;
import org.dbtools.schema.schemafile.SchemaEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Jeff
 */
public class AndroidRecordManagerRenderer {

    private JavaClass myClass;

    private boolean injectionSupport = false;

    /**
     * Creates a new instance of AndroidRecordManagerRenderer.
     */
    public void generate(SchemaEntity table, String packageName) {
        String className = getClassName(table);
        myClass = new JavaClass(packageName, className);
        myClass.setExtends(AndroidBaseRecordManagerRenderer.getClassName(table)); // extend the generated base class

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
        if (injectionSupport) {
            myClass.addAnnotation("javax.inject.Singleton");
        }

        // constructor
        myClass.setCreateDefaultConstructor(false);

        List<JavaVariable> constParams = new ArrayList<>();
        String constContent = "";
        myClass.addConstructor(Access.PUBLIC, constParams, constContent);
    }

    public static String getClassName(SchemaEntity table) {
        String recordClassName = AndroidRecordClassRenderer.createClassName(table);
        return recordClassName + "Manager";
    }

    public void writeToFile(String outDir) {
        myClass.writeToDisk(outDir);
    }

    public void setInjectionSupport(boolean injectionSupport) {
        this.injectionSupport = injectionSupport;
    }
}
