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

import org.dbtools.codegen.java.Access;
import org.dbtools.codegen.java.JavaClass;
import org.dbtools.codegen.java.JavaMethod;
import org.dbtools.codegen.java.JavaVariable;
import org.dbtools.gen.GenConfig;
import org.dbtools.schema.schemafile.SchemaEntity;
import org.dbtools.schema.schemafile.SchemaEntityType;

/**
 * @author Jeff
 */
public class JPABaseRecordManagerRenderer {

    private JavaClass myClass;
    private GenConfig genConfig;

    /**
     * Creates a new instance of JPABaseRecordManagerRenderer.
     */
    public JPABaseRecordManagerRenderer() {
    }

    public void generateObjectCode(SchemaEntity entity, String packageName) {
        String TAB = JavaClass.getTab();
        SchemaEntityType type = entity.getType();
        String recordClassName = JPARecordClassRenderer.createClassName(entity);
        String className = getClassName(entity);
        myClass = new JavaClass(packageName, className);

        myClass.addImport("org.dbtools.jpa.domain.JPABaseManager");
        myClass.setExtends("JPABaseManager<" + recordClassName + ">");

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

        myClass.addImport("javax.persistence.EntityManager");
        if (genConfig.isJavaeeSupport()) {
            JavaVariable entityManagerVariable = myClass.addVariable("EntityManager", "entityManager", true);
            entityManagerVariable.addAnnotation("javax.persistence.PersistenceContext");
        } else if (genConfig.isInjectionSupport()) {
            JavaVariable entityManagerVariable = myClass.addVariable("EntityManager", "entityManager", true);
            entityManagerVariable.setAccess(Access.DEFAULT_NONE);
            entityManagerVariable.addAnnotation("javax.inject.Inject");
        } else {
            myClass.addVariable("EntityManager", "entityManager", true);
        }


        myClass.addMethod(Access.PUBLIC, "Class<" + recordClassName + ">", "getRecordClass", "return " + recordClassName + ".class;");
        myClass.addMethod(Access.PUBLIC, "String", "getTableName", "return " + recordClassName + ".TABLE;");
        myClass.addMethod(Access.PUBLIC, "String", "getTableClassName", "return " + recordClassName + ".TABLE_CLASSNAME;");

        switch (type) {
            default:
            case TABLE:
                myClass.addMethod(Access.PUBLIC, "String", "getPrimaryKey", "return " + recordClassName + "." + JPABaseRecordRenderer.PRIMARY_KEY_COLUMN + ";");
                myClass.addMethod(Access.PUBLIC, "String", "getPrimaryKeyProperty", "return " + recordClassName + "." + JPABaseRecordRenderer.PRIMARY_KEY_PROPERTY_COLUMN + ";");
                break;
            case VIEW:
            case QUERY:
                myClass.addMethod(Access.PUBLIC, "String", "getPrimaryKey", "return null;");
                myClass.addMethod(Access.PUBLIC, "String", "getPrimaryKeyProperty", "return null;");
                break;
        }
    }

    public void addFindCountMethod(JavaClass myClass, String recordClassName) {
        myClass.addImport("javax.persistence.Query");
        String content = "Query q = entityManager.createNativeQuery(\"SELECT count(0) FROM \" + " + recordClassName + ".TABLE);\n"
                + "return ((Number) q.getSingleResult()).longValue();\n";
        myClass.addMethod(Access.PUBLIC, "long", "findCount", content);
    }

    public void addFindAllMethod(JavaClass myClass, String recordClassName) {
        myClass.addImport("javax.persistence.Query");
        String content = "Query q = entityManager.createQuery(\"SELECT o FROM \" + " + recordClassName + ".TABLE_CLASSNAME + \" o\");\n"
                + "return q.getResultList();\n";

        myClass.addImport("java.util.List");
        myClass.addMethod(Access.PUBLIC, "List<" + recordClassName + ">", "findAll", content);
    }

    public void addDeleteAllMethod(JavaClass myClass, String recordClassName) {
        myClass.addImport("javax.persistence.Query");
        String content = "Query q = entityManager.createNativeQuery(\"DELETE FROM \" + " + recordClassName + ".TABLE);\n"
                + "q.executeUpdate();\n";
        myClass.addMethod(Access.PUBLIC, "void", "deleteAll", content);
    }

    private void addJavaEESupport(JavaMethod method) {
        if (genConfig.isJavaeeSupport()) {
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

    public void setGenConfig(GenConfig genConfig) {
        this.genConfig = genConfig;
    }
}
