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

import org.dbtools.codegen.java.JavaClass;
import org.dbtools.gen.GenConfig;
import org.dbtools.schema.schemafile.SchemaEntity;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Jeff
 */
public class AndroidRecordRenderer {

    private JavaClass myClass;
    private GenConfig genConfig;

    /**
     * Creates a new instance of AndroidRecordRenderer.
     */
    public AndroidRecordRenderer() {
    }

    public void generate(SchemaEntity entity, String packageName) {
        String baseClassName = AndroidBaseRecordRenderer.createClassName(false, entity.getClassName());
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
            myClass.addImport("org.dbtools.android.domain.database.contentvalues.DBToolsContentValues");
            myClass.setCreateDefaultConstructor(false);
        }


    }



    public static String createClassName(SchemaEntity entity) {
        return entity.getClassName();
    }

    public void writeToFile(String directoryName) {
        myClass.writeToDisk(directoryName);
    }

    public void setGenConfig(GenConfig genConfig) {
        this.genConfig = genConfig;
    }
}
