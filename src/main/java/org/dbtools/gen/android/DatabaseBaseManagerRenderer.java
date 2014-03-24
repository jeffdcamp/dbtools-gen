package org.dbtools.gen.android;

import org.dbtools.codegen.Access;
import org.dbtools.codegen.JavaClass;
import org.dbtools.codegen.JavaVariable;
import org.dbtools.schema.schemafile.DatabaseSchema;
import org.dbtools.schema.schemafile.SchemaDatabase;
import org.dbtools.schema.schemafile.SchemaEntity;
import org.dbtools.schema.schemafile.SchemaTable;
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
            String databaseConstName = JavaUtil.nameToJavaConst(database.getName()) + "_DATABASE_NAME";
            myClass.addConstant("String", databaseConstName, database.getName());
            createCreateDatabase(content, databaseConstName, database);
        }

        myClass.addMethod(Access.PUBLIC, "void", "onCreate", Arrays.asList(new JavaVariable("AndroidDatabase", "androidDatabase")), content.toString());
    }

    private void createCreateDatabase(StringBuilder content, String databaseConstName, SchemaDatabase database) {
        String varName = JavaUtil.sqlNameToJavaVariableName(databaseConstName);
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

    public static String getClassName(SchemaEntity table) {
        String recordClassName = AndroidRecordClassRenderer.createClassName(table);
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
