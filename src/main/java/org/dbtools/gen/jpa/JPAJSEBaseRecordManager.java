/*
 * JPAJSEBaseRecordManager.java
 *
 * Created on February 24, 2007, 11:03 AM
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
import org.dbtools.schema.schemafile.SchemaTable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jeff
 */
public class JPAJSEBaseRecordManager {

    private JavaClass myClass;
    private boolean springSupport = false;

    /**
     * Creates a new instance of JPAJSEBaseRecordManager.
     */
    public JPAJSEBaseRecordManager() {
    }

    public void generateObjectCode(SchemaTable table, String packageName, String author, String version, PrintStream psLog) {
        String TAB = JavaClass.getTab();
        String recordClassName = JPARecordClassRenderer.createClassName(table);
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
        myClass.setCreateDefaultConstructor(false);
        myClass.addConstructor(Access.PRIVATE, null, null);

        List<JavaVariable> constParams = new ArrayList<JavaVariable>();
        constParams.add(new JavaVariable("EntityManager", "em"));
        String constContent = "";
        constContent = "if (em == null) {\n";
        constContent = TAB + "throw new IllegalArgumentException(\"EntityManager parameter cannot be null\");\n";
        constContent = "}\n";
        constContent = "this.entityManager = em;";
        myClass.addConstructor(Access.PUBLIC, constParams, constContent);

        // singleton factory
        String managerClassName = JPARecordClassRenderer.createClassName(table) + "Manager";
        JavaVariable managerFactoryVar = myClass.addVariable(managerClassName, "manager");
        managerFactoryVar.setStatic(true);
        managerFactoryVar.setVolatile(true);
        managerFactoryVar.setGenerateSetter(true);  // for Mock Unit testing

        String factoryMethodContet = "";
        factoryMethodContet += "if (manager == null) {\n";
        factoryMethodContet += TAB + "manager = new " + managerClassName + "(em);\n";
        factoryMethodContet += "}\n\n";
        factoryMethodContet += "manager.setEntityManager(em);\n";
        factoryMethodContet += "\n";
        factoryMethodContet += "return manager;\n";


        List<JavaVariable> factoryParams = new ArrayList<JavaVariable>();
        factoryParams.add(new JavaVariable("EntityManager", "em"));
        JavaMethod factoryMethod = myClass.addMethod(Access.PUBLIC, managerClassName, "get" + managerClassName, factoryParams, factoryMethodContet);
        factoryMethod.setStatic(true);


        // variables
        myClass.addImport("javax.persistence.EntityManager");
        JavaVariable emVar = myClass.addVariable("EntityManager", "entityManager", true);
        emVar.addAnnotation("@javax.persistence.PersistenceContext");


        String recordVarParamName = "record";
        List<JavaVariable> recordClassOnlyParam = new ArrayList<JavaVariable>();
        recordClassOnlyParam.add(new JavaVariable(recordClassName, recordVarParamName));

        JavaMethod createMethod = myClass.addMethod(Access.PUBLIC, "void", "create", recordClassOnlyParam, "entityManager.persist(" + recordVarParamName + ");");
        addSpringSupport(createMethod);

        String updateContent = recordClassName + " mergedRecord = entityManager.merge(" + recordVarParamName + ");\n"
                + "mergedRecord." + JPABaseRecordClassRenderer.CLEANUP_ORPHANS_METHOD_NAME + "(entityManager);  // work-around till CascadeType.DELETE-ORPHAN is supported\n";
        JavaMethod updateMethod = myClass.addMethod(Access.PUBLIC, "void", "update", recordClassOnlyParam, updateContent);
        addSpringSupport(updateMethod);

        String deleteContent = recordClassName + " mergedRecord = entityManager.merge(" + recordVarParamName + ");\n"
                + "mergedRecord." + JPABaseRecordClassRenderer.CLEANUP_ORPHANS_METHOD_NAME + "(entityManager);  // work-around till CascadeType.DELETE-ORPHAN is supported\n"
                + "entityManager.remove(mergedRecord);\n";

        JavaMethod deleteMethod = myClass.addMethod(Access.PUBLIC, "void", "delete", recordClassOnlyParam, deleteContent);
        addSpringSupport(deleteMethod);

        String saveContent = "if (" + recordVarParamName + ".isNewRecord()) {\n"
                + TAB + "create(" + recordVarParamName + ");\n"
                + "} else {\n"
                + TAB + "update(" + recordVarParamName + ");\n"
                + "}\n";

        JavaMethod saveMethod = myClass.addMethod(Access.PUBLIC, "void", "save", recordClassOnlyParam, saveContent);
        addSpringSupport(saveMethod);

        List<JavaVariable> findParams = new ArrayList<JavaVariable>();
        findParams.add(new JavaVariable("Object", "pk"));
        myClass.addMethod(Access.PUBLIC, recordClassName, "find", findParams, "return (" + recordClassName + ") entityManager.find(" + recordClassName + ".class, pk);");

        myClass.addImport("javax.persistence.Query");
        String findCountContent = "Query q = getEntityManager().createNativeQuery(\"SELECT count(0) FROM \" + " + recordClassName + ".TABLE);\n"
                + "return ((Number) q.getSingleResult()).longValue();\n";
        myClass.addMethod(Access.PUBLIC, "long", "findCount", findCountContent);
    }

    private void addSpringSupport(JavaMethod method) {
        if (springSupport) {
            method.addAnnotation("@org.springframework.transaction.annotation.Transactional");
        }
    }

    public static String getClassName(SchemaTable table) {
        String recordClassName = JPARecordClassRenderer.createClassName(table);
        return recordClassName + "BaseManager";
    }

    public void writeToFile(String outDir) {
        myClass.writeToDisk(outDir);
    }

    void setSpringSupport(boolean b) {
        this.springSupport = b;
    }
}
