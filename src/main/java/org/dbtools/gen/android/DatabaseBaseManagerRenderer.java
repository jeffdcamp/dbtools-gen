package org.dbtools.gen.android;

import org.dbtools.codegen.Access;
import org.dbtools.codegen.JavaClass;
import org.dbtools.codegen.JavaVariable;
import org.dbtools.schema.schemafile.*;
import org.dbtools.util.JavaUtil;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * User: jcampbell
 * Date: 1/18/14
 */
public class DatabaseBaseManagerRenderer {

    private JavaClass myClass;
    private static final String TAB = JavaClass.getTab();

    private String packageBase;
    private String outDir;
    private boolean encryptionSupport = false; // use SQLCipher

    public void generate(DatabaseSchema databaseSchema) {
        System.out.println("Generating DatabaseBaseManager...");

        String className = "DatabaseBaseManager";
        myClass = new JavaClass(packageBase, className);
        myClass.setExtends("AndroidDatabaseManager"); // extend the generated base class
        myClass.setAbstract(true);
        addImports();

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

        createOnCreate(databaseSchema);
        createOnCreateViews(databaseSchema);

        myClass.writeToDisk(outDir, true);
    }

    private void addImports() {
        myClass.addImport("android.util.Log");
        myClass.addImport("org.dbtools.android.domain.AndroidDatabase");
        myClass.addImport("org.dbtools.android.domain.AndroidDatabaseManager");

        if (!encryptionSupport) {
            myClass.addImport("android.database.sqlite.SQLiteDatabase");
            myClass.addImport("org.dbtools.android.domain.AndroidBaseManager");
        } else {
            myClass.addImport("org.dbtools.android.domain.secure.AndroidBaseManager");
            myClass.addImport("net.sqlcipher.database.SQLiteDatabase");
        }
    }

    private void createOnCreate(DatabaseSchema databaseSchema) {
        StringBuilder content = new StringBuilder();
        content.append("Log.i(TAG, \"Creating database: \" + androidDatabase.getName());\n");

        for (SchemaDatabase database : databaseSchema.getDatabases()) {
            String databaseConstName = JavaUtil.nameToJavaConst(database.getName()) + "_DATABASE" + "_NAME";
            String databaseMethodName = JavaUtil.nameToJavaConst(database.getName()) + "_TABLES";
            myClass.addConstant("String", databaseConstName, database.getName());
            createCreateDatabase(content, databaseConstName, databaseMethodName, database);
        }

        myClass.addMethod(Access.PUBLIC, "void", "onCreate", Arrays.asList(new JavaVariable("AndroidDatabase", "androidDatabase")), content.toString());
    }

    private void createCreateDatabase(StringBuilder content, String databaseConstName, String databaseMethodName, SchemaDatabase database) {
        String varName = JavaUtil.sqlNameToJavaVariableName(databaseMethodName);
        String createDatabaseMethodName = "create" + Character.toUpperCase(varName.charAt(0)) + varName.substring(1);

        content.append("if (androidDatabase.getName().equals(" + databaseConstName + ")) {\n");
        content.append(TAB).append(createDatabaseMethodName).append("(androidDatabase);\n");
        content.append("}\n");

        StringBuilder createDatabaseContent = new StringBuilder();
        createDatabaseContent.append("SQLiteDatabase database = androidDatabase.getSqLiteDatabase();\n");
        createDatabaseContent.append("database.beginTransaction();\n");

        createDatabaseContent.append("\n// Enum Tables\n");
        for (SchemaTable table : database.getTables()) {
            if (table.isEnumerationTable()) {
                myClass.addImport(JavaUtil.createTableImport(packageBase, table.getClassName()));
                createDatabaseContent.append("AndroidBaseManager.createTable(database, " + table.getClassName() + ".CREATE_TABLE);\n");
            }
        }

        createDatabaseContent.append("\n// Tables\n");
        for (SchemaTable table : database.getTables()) {
            if (!table.isEnumerationTable()) {
                myClass.addImport(JavaUtil.createTableImport(packageBase, table.getClassName()));
                createDatabaseContent.append("AndroidBaseManager.createTable(database, " + table.getClassName() + ".CREATE_TABLE);\n");
            }
        }

        createDatabaseContent.append("\n");
        createDatabaseContent.append("database.setTransactionSuccessful();\n");
        createDatabaseContent.append("database.endTransaction();\n");
        myClass.addMethod(Access.PUBLIC, "void", createDatabaseMethodName, Arrays.asList(new JavaVariable("AndroidDatabase", "androidDatabase")), createDatabaseContent.toString());
    }

    private void createOnCreateViews(DatabaseSchema databaseSchema) {
        StringBuilder createContent = new StringBuilder();
        StringBuilder dropContent = new StringBuilder();

        createContent.append("Log.i(TAG, \"Creating database views: \" + androidDatabase.getName());\n");
        dropContent.append("Log.i(TAG, \"Dropping database views: \" + androidDatabase.getName());\n");

        for (SchemaDatabase database : databaseSchema.getDatabases()) {
            String databaseConstName = JavaUtil.nameToJavaConst(database.getName()) + "_DATABASE_NAME";
            String databaseMethodName = JavaUtil.nameToJavaConst(database.getName()) + "_VIEWS";
            createCreateViews(createContent, databaseConstName, databaseMethodName, database);
            createDropViews(dropContent, databaseConstName, databaseMethodName, database);
        }

        myClass.addMethod(Access.PUBLIC, "void", "onCreateViews", Arrays.asList(new JavaVariable("AndroidDatabase", "androidDatabase")), createContent.toString());
        myClass.addMethod(Access.PUBLIC, "void", "onDropViews", Arrays.asList(new JavaVariable("AndroidDatabase", "androidDatabase")), dropContent.toString());
    }

    private void createCreateViews(StringBuilder content, String databaseConstName, String databaseMethodName, SchemaDatabase database) {
        if (database.getViews().isEmpty()) {
            return;
        }

        String varName = JavaUtil.sqlNameToJavaVariableName(databaseMethodName);
        String createDatabaseViewsMethodName = "create" + Character.toUpperCase(varName.charAt(0)) + varName.substring(1);

        content.append("if (androidDatabase.getName().equals(" + databaseConstName + ")) {\n");
        content.append(TAB).append(createDatabaseViewsMethodName).append("(androidDatabase);\n");
        content.append("}\n");

        StringBuilder createDatabaseViewsContent = new StringBuilder();
        createDatabaseViewsContent.append("SQLiteDatabase database = androidDatabase.getSqLiteDatabase();\n");
        createDatabaseViewsContent.append("database.beginTransaction();\n");

        createDatabaseViewsContent.append("\n// Views\n");
        for (SchemaView view : database.getViews()) {
            myClass.addImport(JavaUtil.createTableImport(packageBase, view.getClassName()));
            createDatabaseViewsContent.append("AndroidBaseManager.createTable(database, " + view.getClassName() + ".CREATE_VIEW);\n");
        }

        createDatabaseViewsContent.append("\n");
        createDatabaseViewsContent.append("database.setTransactionSuccessful();\n");
        createDatabaseViewsContent.append("database.endTransaction();\n");
        myClass.addMethod(Access.PUBLIC, "void", createDatabaseViewsMethodName, Arrays.asList(new JavaVariable("AndroidDatabase", "androidDatabase")), createDatabaseViewsContent.toString());
    }

    private void createDropViews(StringBuilder content, String databaseConstName, String databaseMethodName, SchemaDatabase database) {
        if (database.getViews().isEmpty()) {
            return;
        }

        String varName = JavaUtil.sqlNameToJavaVariableName(databaseMethodName);
        String createDatabaseViewsMethodName = "drop" + Character.toUpperCase(varName.charAt(0)) + varName.substring(1);

        content.append("if (androidDatabase.getName().equals(" + databaseConstName + ")) {\n");
        content.append(TAB).append(createDatabaseViewsMethodName).append("(androidDatabase);\n");
        content.append("}\n");

        StringBuilder createDatabaseViewsContent = new StringBuilder();
        createDatabaseViewsContent.append("SQLiteDatabase database = androidDatabase.getSqLiteDatabase();\n");
        createDatabaseViewsContent.append("database.beginTransaction();\n");

        createDatabaseViewsContent.append("\n// Views\n");
        for (SchemaView view : database.getViews()) {
            myClass.addImport(JavaUtil.createTableImport(packageBase, view.getClassName()));
            createDatabaseViewsContent.append("AndroidBaseManager.dropTable(database, " + view.getClassName() + ".DROP_VIEW);\n");
        }

        createDatabaseViewsContent.append("\n");
        createDatabaseViewsContent.append("database.setTransactionSuccessful();\n");
        createDatabaseViewsContent.append("database.endTransaction();\n");
        myClass.addMethod(Access.PUBLIC, "void", createDatabaseViewsMethodName, Arrays.asList(new JavaVariable("AndroidDatabase", "androidDatabase")), createDatabaseViewsContent.toString());
    }

    public static String getClassName(SchemaEntity table) {
        String recordClassName = AndroidRecordRenderer.createClassName(table);
        return recordClassName + "Manager";
    }

    public void setPackageBase(String packageBase) {
        this.packageBase = packageBase;
    }

    public void setOutDir(String outDir) {
        this.outDir = outDir;
    }

    public void setEncryptionSupport(boolean encryptionSupport) {
        this.encryptionSupport = encryptionSupport;
    }
}
