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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * @author Jeff
 */
public class AndroidManagerRenderer {

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


        if (entity.getType() == SchemaEntityType.QUERY) {
            String recordClassName = AndroidRecordRenderer.createClassName(entity);
            myClass.addMethod(Access.PUBLIC, "String", "getQuery", "return " + recordClassName + ".QUERY;").addAnnotation("Override");
        }
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
