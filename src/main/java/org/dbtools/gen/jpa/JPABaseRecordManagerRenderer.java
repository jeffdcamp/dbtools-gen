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
import org.dbtools.schema.schemafile.SchemaEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jeff
 */
public class JPABaseRecordManagerRenderer {

    private JavaClass myClass;
    private boolean injectionSupport = true;
    private boolean javaeeSupport = false;

    /**
     * Creates a new instance of JPABaseRecordManagerRenderer.
     */
    public JPABaseRecordManagerRenderer() {
    }

    public void generateObjectCode(SchemaEntity entity, String packageName) {
        String TAB = JavaClass.getTab();
        String recordClassName = JPARecordClassRenderer.createClassName(entity);
        String className = getClassName(entity);
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

        if (!injectionSupport) {
            // constructor
            myClass.setCreateDefaultConstructor(false);
            myClass.addConstructor(Access.PRIVATE, null, null);

            List<JavaVariable> constParams = new ArrayList<>();
            constParams.add(new JavaVariable("EntityManager", "em"));
            String constContent = "";
            constContent += "if (em == null) {\n";
            constContent += TAB + "throw new IllegalArgumentException(\"EntityManager parameter cannot be null\");\n";
            constContent += "}\n";
            constContent += "this.entityManager = em;";
            myClass.addConstructor(Access.PUBLIC, constParams, constContent);

            // singleton factory
            String managerClassName = JPARecordClassRenderer.createClassName(entity) + "Manager";
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


            List<JavaVariable> factoryParams = new ArrayList<>();
            factoryParams.add(new JavaVariable("EntityManager", "em"));
            JavaMethod factoryMethod = myClass.addMethod(Access.PUBLIC, managerClassName, "get" + managerClassName, factoryParams, factoryMethodContet);
            factoryMethod.setStatic(true);
        }


        // variables
        myClass.addImport("javax.persistence.EntityManager");
        JavaVariable emVar = myClass.addVariable("EntityManager", "entityManager", true);
        emVar.addAnnotation("@javax.persistence.PersistenceContext");


        String recordVarParamName = "record";
        List<JavaVariable> recordClassOnlyParam = new ArrayList<>();
        recordClassOnlyParam.add(new JavaVariable(recordClassName, recordVarParamName));

        JavaMethod createMethod = myClass.addMethod(Access.PUBLIC, "void", "create", recordClassOnlyParam, "entityManager.persist(" + recordVarParamName + ");");
        addJavaEESupport(createMethod);

        String updateContent = recordClassName + " mergedRecord = entityManager.merge(" + recordVarParamName + ");\n"
                + "mergedRecord." + JPABaseRecordClassRenderer.CLEANUP_ORPHANS_METHOD_NAME + "(entityManager);  // work-around till CascadeType.DELETE-ORPHAN is supported\n";
        JavaMethod updateMethod = myClass.addMethod(Access.PUBLIC, "void", "update", recordClassOnlyParam, updateContent);
        addJavaEESupport(updateMethod);

        String deleteContent = recordClassName + " mergedRecord = entityManager.merge(" + recordVarParamName + ");\n"
                + "mergedRecord." + JPABaseRecordClassRenderer.CLEANUP_ORPHANS_METHOD_NAME + "(entityManager);  // work-around till CascadeType.DELETE-ORPHAN is supported\n"
                + "entityManager.remove(mergedRecord);\n";

        JavaMethod deleteMethod = myClass.addMethod(Access.PUBLIC, "void", "delete", recordClassOnlyParam, deleteContent);
        addJavaEESupport(deleteMethod);

        String saveContent = "if (" + recordVarParamName + ".isNewRecord()) {\n"
                + TAB + "create(" + recordVarParamName + ");\n"
                + "} else {\n"
                + TAB + "update(" + recordVarParamName + ");\n"
                + "}\n";

        JavaMethod saveMethod = myClass.addMethod(Access.PUBLIC, "void", "save", recordClassOnlyParam, saveContent);
        addJavaEESupport(saveMethod);

        List<JavaVariable> findParams = new ArrayList<>();
        findParams.add(new JavaVariable("Object", "pk"));
        myClass.addMethod(Access.PUBLIC, recordClassName, "find", findParams, "return (" + recordClassName + ") entityManager.find(" + recordClassName + ".class, pk);");

        addFindAllMethod(myClass, recordClassName);
        addFindCountMethod(myClass, recordClassName);
    }

    public void addFindCountMethod(JavaClass myClass, String recordClassName) {
        myClass.addImport("javax.persistence.Query");
        String content = "Query q = getEntityManager().createNativeQuery(\"SELECT count(0) FROM \" + " + recordClassName + ".TABLE);\n"
                + "return ((Number) q.getSingleResult()).longValue();\n";
        myClass.addMethod(Access.PUBLIC, "long", "findCount", content);
    }

    public void addFindAllMethod(JavaClass myClass, String recordClassName) {
        myClass.addImport("javax.persistence.Query");
        String content = "Query q = getEntityManager().createQuery(\"SELECT o FROM \" + " + recordClassName + ".TABLE_CLASSNAME + \" o\");\n"
                + "return q.getResultList();\n";

        myClass.addImport("java.util.List");
        myClass.addMethod(Access.PUBLIC, "List<" + recordClassName + ">", "findAll", content);
    }

    private void addJavaEESupport(JavaMethod method) {
        if (javaeeSupport) {
            method.addAnnotation("@javax.transaction.Transactional");
        }
    }

    public static String getClassName(SchemaEntity entity) {
        String recordClassName = JPARecordClassRenderer.createClassName(entity);
        return recordClassName + "BaseManager";
    }

    public void writeToFile(String outDir) {
        myClass.writeToDisk(outDir);
    }

    public void setJavaeeSupport(boolean b) {
        this.javaeeSupport = b;
    }

    public void setInjectionSupport(boolean injectionSupport) {
        this.injectionSupport = injectionSupport;
    }
}
