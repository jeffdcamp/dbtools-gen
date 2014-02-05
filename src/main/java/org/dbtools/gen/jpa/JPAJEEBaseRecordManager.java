/*
 * JPAJEEBaseRecordManager.java
 *
 * Created on February 24, 2007, 11:03 AM
 *
 * Copyright 2007 Jeff Campbell. All rights reserved. Unauthorized reproduction 
 * is a violation of applicable law. This material contains certain 
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */

package org.dbtools.gen.jpa;

import org.dbtools.codegen.*;
import org.dbtools.schema.schemafile.SchemaTable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jeff
 */
public class JPAJEEBaseRecordManager {

    private JavaClass myClass;
    private JavaInterface localInterface;
    private JavaInterface remoteInterface;
    private JavaInterface localInterfaceBase;
    private JavaInterface remoteInterfaceBase;

    private boolean localInterfaceRequired = false;
    private boolean remoteInterfaceRequired = false;

    /**
     * Creates a new instance of JPAJEEBaseRecordManager
     */
    public JPAJEEBaseRecordManager() {
    }

    public void generateObjectCode(SchemaTable table, String packageName, String author, String version, PrintStream psLog) {
        String TAB = JavaClass.getTab();
        String recordClassName = JPARecordClassRenderer.createClassName(table);
        String className = getClassName(table);
        myClass = new JavaClass(packageName, className);

//        myClass.addImport("javax.ejb.Stateless");
//        myClass.addAnnotation("Stateless");

        // LOCAL
        String localBaseInterfaceName = getLocalBaseInterfaceName(table);
        localInterfaceBase = new JavaInterface(packageName, localBaseInterfaceName);

        String localInterfaceName = getLocalInterfaceName(table);
        localInterface = new JavaInterface(packageName, localInterfaceName);
        localInterface.addImport("javax.ejb.Local");
        localInterface.addAnnotation("Local");
        localInterface.setExtends(localBaseInterfaceName);

        // REMOTE
        String remoteBaseInterfaceName = getRemoteBaseInterfaceName(table);
        remoteInterfaceBase = new JavaInterface(packageName, remoteBaseInterfaceName);

        String remoteInterfaceName = getRemoteInterfaceName(table);
        remoteInterface = new JavaInterface(packageName, remoteInterfaceName);
        remoteInterface.addImport("javax.ejb.Remote");
        remoteInterface.addAnnotation("Remote");
        remoteInterface.setExtends(remoteBaseInterfaceName);


        if (isLocalInterfaceRequired()) {
            myClass.addImplements(localBaseInterfaceName);
        }

        if (isRemoteInterfaceRequired()) {
            myClass.addImplements(remoteBaseInterfaceName);
        }

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

        // constructor(s)
        List<JavaVariable> constParams = new ArrayList<JavaVariable>();
        constParams.add(new JavaVariable("EntityManager", "em"));
        String constContent = "";
        constContent = "if (em == null) {\n";
        constContent = TAB + "throw new IllegalArgumentException(\"EntityManager parameter cannot be null\");\n";
        constContent = "}\n";
        constContent = "this.entityManager = em;";
        myClass.addConstructor(Access.PUBLIC, constParams, constContent);

        // variables
        myClass.addImport("javax.persistence.EntityManager");
        JavaVariable emVar = myClass.addVariable("EntityManager", "entityManager");
        emVar.setGenerateGetter(true);
        myClass.addImport("javax.persistence.PersistenceContext");
        emVar.addAnnotation("PersistenceContext");

        String recordVarParamName = "record";
        List<JavaVariable> recordClassOnlyParam = new ArrayList<JavaVariable>();
        recordClassOnlyParam.add(new JavaVariable(recordClassName, recordVarParamName));

        JavaMethod persistMethod = myClass.addMethod(Access.PUBLIC, "void", "create", recordClassOnlyParam, "entityManager.persist(" + recordVarParamName + ");");
        localInterfaceBase.addMethod(persistMethod);
        remoteInterfaceBase.addMethod(persistMethod);

        String updateContent = recordClassName + " mergedRecord = entityManager.merge(" + recordVarParamName + ");\n" +
                "mergedRecord." + JPABaseRecordClassRenderer.CLEANUP_ORPHANS_METHODNAME + "(entityManager);  // work-around till CascadeType.DELETE-ORPHAN is supported\n";

        JavaMethod mergeMethod = myClass.addMethod(Access.PUBLIC, "void", "update", recordClassOnlyParam, updateContent);
        localInterfaceBase.addMethod(mergeMethod);
        remoteInterfaceBase.addMethod(mergeMethod);

        String deleteContent = recordClassName + " mergedRecord = entityManager.merge(" + recordVarParamName + ");\n" +
                "mergedRecord." + JPABaseRecordClassRenderer.CLEANUP_ORPHANS_METHODNAME + "(entityManager);  // work-around till CascadeType.DELETE-ORPHAN is supported\n" +
                "entityManager.remove(mergedRecord);\n";

        JavaMethod deleteMethod = myClass.addMethod(Access.PUBLIC, "void", "delete", recordClassOnlyParam, deleteContent);
        localInterfaceBase.addMethod(deleteMethod);
        remoteInterfaceBase.addMethod(deleteMethod);

        List<JavaVariable> findParams = new ArrayList<JavaVariable>();
        findParams.add(new JavaVariable("Object", "pk"));
        JavaMethod findMethod = myClass.addMethod(Access.PUBLIC, recordClassName, "find", findParams, "return (" + recordClassName + ") entityManager.find(" + recordClassName + ".class, pk);");
        localInterfaceBase.addMethod(findMethod);
        remoteInterfaceBase.addMethod(findMethod);
    }

    public static String getClassName(SchemaTable table) {
        String recordClassName = JPARecordClassRenderer.createClassName(table);
        return recordClassName + "BaseManager";
    }

    public static String getLocalInterfaceName(SchemaTable table) {
        return JPAJEERecordManager.getClassName(table) + "Local";
    }

    public static String getRemoteInterfaceName(SchemaTable table) {
        return JPAJEERecordManager.getClassName(table) + "Remote";
    }

    public static String getLocalBaseInterfaceName(SchemaTable table) {
        return JPAJEERecordManager.getClassName(table) + "LocalBase";
    }

    public static String getRemoteBaseInterfaceName(SchemaTable table) {
        return JPAJEERecordManager.getClassName(table) + "RemoteBase";
    }

    public void writeToFile(String outDir) {
        myClass.writeToDisk(outDir);

        if (isLocalInterfaceRequired()) {
            localInterface.writeToDisk(outDir, false);
            localInterfaceBase.writeToDisk(outDir);
        }

        if (isRemoteInterfaceRequired()) {
            remoteInterface.writeToDisk(outDir, false);
            remoteInterfaceBase.writeToDisk(outDir);
        }
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
