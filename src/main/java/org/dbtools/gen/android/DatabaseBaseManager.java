package org.dbtools.gen.android;

import org.dbtools.codegen.Access;
import org.dbtools.codegen.JavaClass;
import org.dbtools.codegen.JavaVariable;
import org.dbtools.schema.schemafile.SchemaEntity;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * User: jcampbell
 * Date: 1/18/14
 */
public class DatabaseBaseManager {

    private JavaClass myClass;

    public void generate(SchemaEntity table, String packageName) {
        String className = getClassName(table);
        myClass = new JavaClass(packageName, className);
        myClass.setExtends("AndroidDatabaseManager"); // extend the generated base class
        myClass.setAbstract(true);

        myClass.addImport("org.dbtools.android.domain.AndroidBaseManager");
        myClass.addImport("org.dbtools.android.domain.AndroidDatabase");
        myClass.addImport("org.dbtools.android.domain.AndroidDatabaseManager");

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

        // constructor
        myClass.setCreateDefaultConstructor(false);

        createOnCreate();
    }

    private void createOnCreate() {
        StringBuilder content = new StringBuilder();
        content.append("Log.i(TAG, \"Creating database: \" + androidDatabase.getName());\n");
        content.append("if (androidDatabase.getName()) {\n");
        content.append("}\n");

        myClass.addMethod(Access.PUBLIC, "void", "onCreate", Arrays.asList(new JavaVariable("AndroidDatabase", "androidDatabase")), content.toString());
    }

    private void createCreateDatabase() {

    }

    public static String getClassName(SchemaEntity table) {
        String recordClassName = AndroidRecordClassRenderer.createClassName(table);
        return recordClassName + "Manager";
    }
}
