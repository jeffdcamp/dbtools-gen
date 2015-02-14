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
import org.dbtools.gen.GenConfig;
import org.dbtools.schema.schemafile.SchemaEntity;

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

        myClass.addMethod(Access.PUBLIC, "Class", "getRecordClass", "return " + recordClassName + ".class;");
        myClass.addMethod(Access.PUBLIC, "String", "getTableName", "return " + recordClassName + ".TABLE;");
        myClass.addMethod(Access.PUBLIC, "String", "getTableClassName", "return " + recordClassName + ".TABLE_CLASSNAME;");
        myClass.addMethod(Access.PUBLIC, "String", "getPrimaryKey", "return " + recordClassName + ".PRIMARY_KEY_COLUMN;");
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
