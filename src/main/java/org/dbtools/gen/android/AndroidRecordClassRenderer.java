/*
 * AndroidRecordClassRenderer.java
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
public class AndroidRecordClassRenderer {

    private JavaClass myClass;

    /**
     * Creates a new instance of AndroidRecordClassRenderer.
     */
    public AndroidRecordClassRenderer() {
    }

    public void generate(SchemaEntity entity, String packageName) {
        String baseClassName = AndroidBaseRecordClassRenderer.createClassName(false, entity.getClassName());
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

        if (!myClass.isEnum()) {
            myClass.addImport("android.database.Cursor");
            myClass.addImport("android.content.ContentValues");
            List<JavaVariable> constructorVarsCursor = new ArrayList<>();
            constructorVarsCursor.add(new JavaVariable("Cursor", "cursor"));
            myClass.addConstructor(Access.PUBLIC, constructorVarsCursor, "setContent(cursor);");

            List<JavaVariable> constructorVarsValues = new ArrayList<>();
            constructorVarsValues.add(new JavaVariable("ContentValues", "values"));
            myClass.addConstructor(Access.PUBLIC, constructorVarsValues, "setContent(values);");
        }
    }

    public static String createClassName(SchemaEntity entity) {
        return entity.getClassName();
    }

    public void writeToFile(String directoryname) {
        myClass.writeToDisk(directoryname);
    }
}
