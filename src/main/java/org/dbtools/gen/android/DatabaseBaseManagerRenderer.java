package org.dbtools.gen.android;

import org.dbtools.codegen.Access;
import org.dbtools.codegen.JavaClass;
import org.dbtools.codegen.JavaVariable;
import org.dbtools.gen.AnnotationConsts;
import org.dbtools.schema.schemafile.*;
import org.dbtools.util.JavaUtil;

import java.util.Arrays;

/**
 * User: jcampbell
 * Date: 1/18/14
 */
public class DatabaseBaseManagerRenderer {

    private JavaClass myClass;
    private static final String TAB = JavaClass.getTab();

    private String packageBase;
    private String outDir;
    private boolean jsr305Support = false; // @Nonnull support
    private boolean encryptionSupport = false; // use SQLCipher
    private boolean includeDatabaseNameInPackage  = false;

    public void generate(DatabaseSchema databaseSchema) {
        System.out.println("Generating DatabaseBaseManager...");

        String className = "DatabaseBaseManager";
        myClass = new JavaClass(packageBase, className);
        myClass.setExtends("AndroidDatabaseManager"); // extend the generated base class
        myClass.setAbstract(true);
        addImports();

        // header comment
        String fileHeaderComment;
        fileHeaderComment = "/*\n";
        fileHeaderComment += " * " + className + ".java\n";
        fileHeaderComment += " *\n";
        fileHeaderComment += " * GENERATED FILE - DO NOT EDIT\n";
        fileHeaderComment += " *\n";
        fileHeaderComment += " */\n";
        myClass.setFileHeaderComment(fileHeaderComment);

        // Since this is generated code.... suppress all warnings
        myClass.addAnnotation("@SuppressWarnings(\"all\")");

        // constructor
        myClass.setCreateDefaultConstructor(false);

        createOnCreate(databaseSchema);
        createOnCreateViews(databaseSchema);

        myClass.writeToDisk(outDir, true);
    }

    private void addImports() {
        myClass.addImport("android.util.Log");
        myClass.addImport("org.dbtools.android.domain.AndroidDatabase");

        if (!encryptionSupport) {
            myClass.addImport("org.dbtools.android.domain.AndroidDatabaseManager");
            myClass.addImport("android.database.sqlite.SQLiteDatabase");
            myClass.addImport("org.dbtools.android.domain.AndroidBaseManager");
        } else {
            myClass.addImport("org.dbtools.android.domain.secure.AndroidDatabaseManager");
            myClass.addImport("org.dbtools.android.domain.secure.AndroidBaseManager");
            myClass.addImport("net.sqlcipher.database.SQLiteDatabase");
        }
    }

    private void createOnCreate(DatabaseSchema databaseSchema) {
        StringBuilder content = new StringBuilder();
        content.append("Log.i(TAG, \"Creating database: \" + androidDatabase.getName());\n");

        for (SchemaDatabase database : databaseSchema.getDatabases()) {
            String databaseName = database.getName();
            databaseName = databaseName.replace(".", ""); // remove any periods (example: "mydb.sqlite")

            String databaseConstName = JavaUtil.nameToJavaConst(databaseName) + "_DATABASE_NAME";
            String databaseMethodName = JavaUtil.nameToJavaConst(databaseName) + "_TABLES";
            myClass.addConstant("String", databaseConstName, database.getName());
            createCreateDatabase(content, databaseConstName, databaseMethodName, database);
        }

        JavaVariable param = new JavaVariable("AndroidDatabase", "androidDatabase");
        if (jsr305Support) {
            param.addAnnotation(AnnotationConsts.NONNULL);
        }

        myClass.addMethod(Access.PUBLIC, "void", "onCreate", Arrays.asList(param), content.toString());
    }

    private void createCreateDatabase(StringBuilder content, String databaseConstName, String databaseMethodName, SchemaDatabase database) {
        String varName = JavaUtil.sqlNameToJavaVariableName(databaseMethodName);
        String createDatabaseMethodName = "create" + Character.toUpperCase(varName.charAt(0)) + varName.substring(1);

        content.append("if (androidDatabase.getName().equals(" + databaseConstName + ")) {\n");
        content.append(TAB).append(createDatabaseMethodName).append("(androidDatabase);\n");
        content.append("}\n");

        StringBuilder createDatabaseContent = new StringBuilder();
        if (!encryptionSupport) {
            createDatabaseContent.append("SQLiteDatabase database = androidDatabase.getSqLiteDatabase();\n");
        } else {
            createDatabaseContent.append("SQLiteDatabase database = androidDatabase.getSecureSqLiteDatabase();\n");
        }
        createDatabaseContent.append("database.beginTransaction();\n");

        // include database name in base package name
        String databaseBasePackage = createDatabaseBasePackage(database);

        createDatabaseContent.append("\n// Enum Tables\n");
        for (SchemaTable table : database.getTables()) {
            if (table.isEnumerationTable()) {
                createDatabaseContent.append("AndroidBaseManager.createTable(database, ")
                        .append(JavaUtil.createTableImport(databaseBasePackage, table.getClassName()))
                        .append(".CREATE_TABLE);\n");
            }
        }

        createDatabaseContent.append("\n// Tables\n");
        for (SchemaTable table : database.getTables()) {
            if (!table.isEnumerationTable()) {
                createDatabaseContent.append("AndroidBaseManager.createTable(database, ")
                        .append(JavaUtil.createTableImport(databaseBasePackage, table.getClassName()))
                        .append(".CREATE_TABLE);\n");
            }
        }

        createDatabaseContent.append("\n");
        createDatabaseContent.append("database.setTransactionSuccessful();\n");
        createDatabaseContent.append("database.endTransaction();\n");

        JavaVariable param = new JavaVariable("AndroidDatabase", "androidDatabase");
        if (jsr305Support) {
            param.addAnnotation(AnnotationConsts.NONNULL);
        }

        myClass.addMethod(Access.PUBLIC, "void", createDatabaseMethodName, Arrays.asList(param), createDatabaseContent.toString());
    }

    private void createOnCreateViews(DatabaseSchema databaseSchema) {
        StringBuilder createContent = new StringBuilder();
        StringBuilder dropContent = new StringBuilder();

        createContent.append("Log.i(TAG, \"Creating database views: \" + androidDatabase.getName());\n");
        dropContent.append("Log.i(TAG, \"Dropping database views: \" + androidDatabase.getName());\n");

        for (SchemaDatabase database : databaseSchema.getDatabases()) {
            String databaseName = database.getName();
            databaseName = databaseName.replace(".", ""); // remove any periods (example: "mydb.sqlite")

            String databaseConstName = JavaUtil.nameToJavaConst(databaseName) + "_DATABASE_NAME";
            String databaseMethodName = JavaUtil.nameToJavaConst(databaseName) + "_VIEWS";
            createCreateViews(createContent, databaseConstName, databaseMethodName, database);
            createDropViews(dropContent, databaseConstName, databaseMethodName, database);
        }

        JavaVariable param = new JavaVariable("AndroidDatabase", "androidDatabase");
        if (jsr305Support) {
            param.addAnnotation(AnnotationConsts.NONNULL);
        }

        myClass.addMethod(Access.PUBLIC, "void", "onCreateViews", Arrays.asList(param), createContent.toString());
        myClass.addMethod(Access.PUBLIC, "void", "onDropViews", Arrays.asList(param), dropContent.toString());
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

        if (!encryptionSupport) {
            createDatabaseViewsContent.append("SQLiteDatabase database = androidDatabase.getSqLiteDatabase();\n");
        } else {
            createDatabaseViewsContent.append("SQLiteDatabase database = androidDatabase.getSecureSqLiteDatabase();\n");
        }
        createDatabaseViewsContent.append("database.beginTransaction();\n");

        // include database name in base package name
        String databaseBasePackage = createDatabaseBasePackage(database);

        createDatabaseViewsContent.append("\n// Views\n");
        for (SchemaView view : database.getViews()) {
            createDatabaseViewsContent.append("AndroidBaseManager.createTable(database, ")
                    .append(JavaUtil.createTableImport(databaseBasePackage, view.getClassName()))
                    .append(".CREATE_VIEW);\n");
        }

        createDatabaseViewsContent.append("\n");
        createDatabaseViewsContent.append("database.setTransactionSuccessful();\n");
        createDatabaseViewsContent.append("database.endTransaction();\n");

        JavaVariable param = new JavaVariable("AndroidDatabase", "androidDatabase");
        if (jsr305Support) {
            param.addAnnotation(AnnotationConsts.NONNULL);
        }

        myClass.addMethod(Access.PUBLIC, "void", createDatabaseViewsMethodName, Arrays.asList(param), createDatabaseViewsContent.toString());
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
        if (!encryptionSupport) {
            createDatabaseViewsContent.append("SQLiteDatabase database = androidDatabase.getSqLiteDatabase();\n");
        } else {
            createDatabaseViewsContent.append("SQLiteDatabase database = androidDatabase.getSecureSqLiteDatabase();\n");
        }
        createDatabaseViewsContent.append("database.beginTransaction();\n");

        // include database name in base package name
        String databaseBasePackage = createDatabaseBasePackage(database);

        createDatabaseViewsContent.append("\n// Views\n");
        for (SchemaView view : database.getViews()) {
            createDatabaseViewsContent.append("AndroidBaseManager.dropTable(database, ")
                    .append(JavaUtil.createTableImport(databaseBasePackage, view.getClassName()))
                    .append(".DROP_VIEW);\n");
        }

        createDatabaseViewsContent.append("\n");
        createDatabaseViewsContent.append("database.setTransactionSuccessful();\n");
        createDatabaseViewsContent.append("database.endTransaction();\n");

        JavaVariable param = new JavaVariable("AndroidDatabase", "androidDatabase");
        if (jsr305Support) {
            param.addAnnotation(AnnotationConsts.NONNULL);
        }

        myClass.addMethod(Access.PUBLIC, "void", createDatabaseViewsMethodName, Arrays.asList(param), createDatabaseViewsContent.toString());
    }

    public static String getClassName(SchemaEntity table) {
        String recordClassName = AndroidRecordRenderer.createClassName(table);
        return recordClassName + "Manager";
    }

    private String createDatabaseBasePackage(SchemaDatabase database) {
        return packageBase + (includeDatabaseNameInPackage ? "." + database.getName().toLowerCase() : "");
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

    public void setJsr305Support(boolean jsr305Support) {
        this.jsr305Support = jsr305Support;
    }

    public void setIncludeDatabaseNameInPackage(boolean includeDatabaseNameInPackage) {
        this.includeDatabaseNameInPackage = includeDatabaseNameInPackage;
    }
}
