package org.dbtools.gen.android;

import org.dbtools.codegen.java.Access;
import org.dbtools.codegen.java.JavaClass;
import org.dbtools.codegen.java.JavaVariable;
import org.dbtools.gen.AnnotationConsts;
import org.dbtools.gen.GenConfig;
import org.dbtools.schema.schemafile.*;
import org.dbtools.util.JavaUtil;

import java.util.Arrays;

/**
 * User: jcampbell
 * Date: 1/18/14
 */
public class DatabaseBaseManagerRenderer {

    private JavaClass myClass;
    private JavaClass myConstClass;
    private String constClassName = "DatabaseManagerConst";
    private static final String TAB = JavaClass.getTab();

    private String packageBase;
    private String outDir;
    private GenConfig genConfig;

    public void generate(DatabaseSchema databaseSchema) {
        System.out.println("Generating DatabaseBaseManager...");

        String className = "DatabaseBaseManager";
        myClass = new JavaClass(packageBase, className);
        myClass.setExtends("AndroidDatabaseManager"); // extend the generated base class
        myClass.setAbstract(true);

        myConstClass = new JavaClass(packageBase, constClassName);

        addHeaders(myClass);
        addHeaders(myConstClass);

        addImports();

        // constructor
        myClass.setCreateDefaultConstructor(false);

        createOnCreate(databaseSchema);
        createOnCreateViews(databaseSchema);

        myClass.writeToDisk(outDir, true);
        myConstClass.writeToDisk(outDir, true);
    }

    private void addHeaders(JavaClass someClass) {
        // header comment
        String fileHeaderComment;
        fileHeaderComment = "/*\n";
        fileHeaderComment += " * " + someClass.getName() + ".java\n";
        fileHeaderComment += " *\n";
        fileHeaderComment += " * GENERATED FILE - DO NOT EDIT\n";
        fileHeaderComment += " *\n";
        fileHeaderComment += " */\n";
        someClass.setFileHeaderComment(fileHeaderComment);

        // Since this is generated code.... suppress all warnings
        someClass.addAnnotation("@SuppressWarnings(\"all\")");
    }

    private void addImports() {
        myClass.addImport("android.util.Log");
        myClass.addImport("org.dbtools.android.domain.AndroidDatabase");
        myClass.addImport("org.dbtools.android.domain.AndroidBaseManager");
        myClass.addImport("org.dbtools.android.domain.AndroidDatabaseManager");
        myClass.addImport("org.dbtools.android.domain.database.DatabaseWrapper");
    }

    private void createOnCreate(DatabaseSchema databaseSchema) {
        StringBuilder content = new StringBuilder();
        content.append("Log.i(TAG, \"Creating database: \" + androidDatabase.getName());\n");

        for (SchemaDatabase database : databaseSchema.getDatabases()) {
            String databaseName = database.getName();
            databaseName = databaseName.replace(".", ""); // remove any periods (example: "mydb.sqlite")

            String databaseConstName = JavaUtil.nameToJavaConst(databaseName) + "_DATABASE_NAME";
            String databaseMethodName = JavaUtil.nameToJavaConst(databaseName) + "_TABLES";
            myConstClass.addConstant("String", databaseConstName, database.getName());
            createCreateDatabase(content, databaseConstName, databaseMethodName, database);
        }

        JavaVariable param = new JavaVariable("AndroidDatabase", "androidDatabase");
        if (genConfig.isJsr305Support()) {
            param.addAnnotation(AnnotationConsts.NONNULL);
        }

        myClass.addMethod(Access.PUBLIC, "void", "onCreate", Arrays.asList(param), content.toString());
    }

    private void createCreateDatabase(StringBuilder content, String databaseConstName, String databaseMethodName, SchemaDatabase database) {
        String varName = JavaUtil.sqlNameToJavaVariableName(databaseMethodName);
        String createDatabaseMethodName = "create" + Character.toUpperCase(varName.charAt(0)) + varName.substring(1);

        content.append("if (androidDatabase.getName().equals(" + constClassName + "." + databaseConstName + ")) {\n");
        content.append(TAB).append(createDatabaseMethodName).append("(androidDatabase);\n");
        content.append("}\n");

        StringBuilder createDatabaseContent = new StringBuilder();
        createDatabaseContent.append("DatabaseWrapper database = androidDatabase.getDatabaseWrapper();\n");
        createDatabaseContent.append("database.beginTransaction();\n");

        // include database name in base package name
        String databaseBasePackage = createDatabaseBasePackage(database);

        createDatabaseContent.append("\n// Enum Tables\n");
        for (SchemaTable table : database.getTables()) {
            if (table.isEnumerationTable()) {
                createDatabaseContent.append("AndroidBaseManager.createTable(database, ")
                        .append(JavaUtil.createTableImport(databaseBasePackage, table.getClassName()) + "Const")
                        .append(".CREATE_TABLE);\n");
            }
        }

        createDatabaseContent.append("\n// Tables\n");
        for (SchemaTable table : database.getTables()) {
            if (!table.isEnumerationTable()) {
                createDatabaseContent.append("AndroidBaseManager.createTable(database, ")
                        .append(JavaUtil.createTableImport(databaseBasePackage, table.getClassName()) + "Const")
                        .append(".CREATE_TABLE);\n");
            }
        }

        createDatabaseContent.append("\n");
        createDatabaseContent.append("database.setTransactionSuccessful();\n");
        createDatabaseContent.append("database.endTransaction();\n");

        JavaVariable param = new JavaVariable("AndroidDatabase", "androidDatabase");
        if (genConfig.isJsr305Support()) {
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
        if (genConfig.isJsr305Support()) {
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

        content.append("if (androidDatabase.getName().equals(" + constClassName + "." + databaseConstName + ")) {\n");
        content.append(TAB).append(createDatabaseViewsMethodName).append("(androidDatabase);\n");
        content.append("}\n");

        StringBuilder createDatabaseViewsContent = new StringBuilder();

        createDatabaseViewsContent.append("DatabaseWrapper database = androidDatabase.getDatabaseWrapper();\n");
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
        if (genConfig.isJsr305Support()) {
            param.addAnnotation(AnnotationConsts.NONNULL);
        }

        myClass.addMethod(Access.PUBLIC, "void", createDatabaseViewsMethodName, Arrays.asList(param), createDatabaseViewsContent.toString());
    }

    private void createDropViews(StringBuilder content, String databaseConstName, String databaseMethodName, SchemaDatabase database) {
        if (database.getViews().isEmpty()) {
            return;
        }

        String varName = JavaUtil.sqlNameToJavaVariableName(databaseMethodName);
        String dropDatabaseViewsMethodName = "drop" + Character.toUpperCase(varName.charAt(0)) + varName.substring(1);

        content.append("if (androidDatabase.getName().equals(" + constClassName + "." + databaseConstName + ")) {\n");
        content.append(TAB).append(dropDatabaseViewsMethodName).append("(androidDatabase);\n");
        content.append("}\n");

        StringBuilder dropDatabaseViewsContent = new StringBuilder();
        dropDatabaseViewsContent.append("DatabaseWrapper database = androidDatabase.getDatabaseWrapper();\n");
        dropDatabaseViewsContent.append("database.beginTransaction();\n");

        // include database name in base package name
        String databaseBasePackage = createDatabaseBasePackage(database);

        dropDatabaseViewsContent.append("\n// Views\n");
        for (SchemaView view : database.getViews()) {
            dropDatabaseViewsContent.append("AndroidBaseManager.dropTable(database, ")
                    .append(JavaUtil.createTableImport(databaseBasePackage, view.getClassName()))
                    .append(".DROP_VIEW);\n");
        }

        dropDatabaseViewsContent.append("\n");
        dropDatabaseViewsContent.append("database.setTransactionSuccessful();\n");
        dropDatabaseViewsContent.append("database.endTransaction();\n");

        JavaVariable param = new JavaVariable("AndroidDatabase", "androidDatabase");
        if (genConfig.isJsr305Support()) {
            param.addAnnotation(AnnotationConsts.NONNULL);
        }

        myClass.addMethod(Access.PUBLIC, "void", dropDatabaseViewsMethodName, Arrays.asList(param), dropDatabaseViewsContent.toString());
    }

    public static String getClassName(SchemaEntity table) {
        String recordClassName = AndroidRecordRenderer.createClassName(table);
        return recordClassName + "Manager";
    }

    private String createDatabaseBasePackage(SchemaDatabase database) {
        return packageBase + (genConfig.isIncludeDatabaseNameInPackage() ? "." + database.getName().toLowerCase() : "");
    }

    public void setPackageBase(String packageBase) {
        this.packageBase = packageBase;
    }

    public void setOutDir(String outDir) {
        this.outDir = outDir;
    }

    public void setGenConfig(GenConfig genConfig) {
        this.genConfig = genConfig;
    }
}
