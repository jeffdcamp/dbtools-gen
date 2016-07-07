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

import org.dbtools.codegen.java.JavaClass;
import org.dbtools.schema.schemafile.SchemaEntity;
import org.dbtools.schema.schemafile.SchemaTable;
import org.dbtools.schema.schemafile.SchemaTableUnique;
import org.dbtools.schema.schemafile.SchemaUniqueField;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author Jeff
 */
public class JPARecordClassRenderer {

    private JavaClass myClass;

    /**
     * Creates a new instance of JPARecordClassRenderer.
     */
    public JPARecordClassRenderer() {
    }

    public void generate(SchemaEntity entity, String packageName) {
        String baseClassName = JPABaseRecordRenderer.createClassName(entity);
        String className = createClassName(entity);
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
        myClass.addImport("javax.persistence.Table");
        StringBuilder tableAnnotationBuilder = new StringBuilder("@Table(name=").append(baseClassName).append(".TABLE");
        if (entity instanceof SchemaTable && !((SchemaTable) entity).getUniqueDeclarations().isEmpty()) {
            myClass.addImport("javax.persistence.UniqueConstraint");
            tableAnnotationBuilder.append(", uniqueConstraints={")
                    .append(getUniqueConstraints(((SchemaTable) entity).getUniqueDeclarations()))
                    .append("}");
        }
        tableAnnotationBuilder.append(")");
        myClass.addAnnotation(tableAnnotationBuilder.toString());
    }

    private String getUniqueConstraints(List<SchemaTableUnique> uniqueDeclarations) {
        StringBuilder constraints = new StringBuilder();
        StringBuilder fields = new StringBuilder();
        for (SchemaTableUnique uniqueDeclaration : uniqueDeclarations) {
            if (constraints.length() > 0) {
                constraints.append(", ");
            }
            constraints.append("@UniqueConstraint(columnNames={");
            for (SchemaUniqueField schemaUniqueField : uniqueDeclaration.getUniqueFields()) {
                if (fields.length() > 0) {
                    fields.append(", ");
                }
                fields.append("\"").append(schemaUniqueField.getName()).append("\"");
            }
            constraints.append(fields.toString());
            fields.setLength(0);
            constraints.append("})");
        }
        return constraints.toString();
    }

    public static String createClassName(SchemaEntity entity) {
        return entity.getClassName();
    }

    public void writeToFile(String directoryName) {
        myClass.writeToDisk(directoryName);
    }
}
