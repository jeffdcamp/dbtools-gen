package org.dbtools.gen.android;

import org.dbtools.codegen.java.Access;
import org.dbtools.codegen.java.JavaClass;
import org.dbtools.codegen.java.JavaVariable;
import org.dbtools.gen.GenConfig;
import org.dbtools.schema.schemafile.DatabaseSchema;
import org.dbtools.schema.schemafile.SchemaDatabase;
import org.dbtools.util.JavaUtil;

import java.util.Arrays;
import java.util.List;

public class AppDatabaseConfigRenderer {

    private JavaClass myClass;
    public final String dbConstClassName = "DatabaseManagerConst";

    private String packageBase;
    private String outDir;
    private GenConfig genConfig;

    public void generate(DatabaseSchema databaseSchema) {
        System.out.println("Generating AppDatabaseConfigRenderer...");

        String className = "AppDatabaseConfig";
        myClass = new JavaClass(packageBase, className);


        myClass.addImplements("DatabaseConfig"); // extend the generated base class
        myClass.setCreateDefaultConstructor(false);
        List<JavaVariable> params = Arrays.asList(new JavaVariable("Application", "application"));
        myClass.addConstructor(Access.PUBLIC, params, "this.application = application;");
        addImports();

        myClass.addVariable("Application", "application");

        createIdentifyDatabases(databaseSchema);
        createCreateNewDatabaseWrapper();
        createNewDBToolsContentValues();
        createNewDBToolsLogger();

        myClass.writeToDisk(outDir, false);
    }

    private void createIdentifyDatabases(DatabaseSchema databaseSchema) {
        myClass.addImport("org.dbtools.android.domain.AndroidDatabaseBaseManager");
        StringBuilder content = new StringBuilder();

        for (SchemaDatabase database : databaseSchema.getDatabases()) {
            String databaseName = database.getName(true);

            String databaseConstName = JavaUtil.nameToJavaConst(databaseName) + "_DATABASE_NAME";
            String databaseConstVersion = "DatabaseManager." + JavaUtil.nameToJavaConst(databaseName + "TablesVersion");
            String databaseViewsConstVersion = "DatabaseManager." + JavaUtil.nameToJavaConst(databaseName + "ViewsVersion");

            content.append("databaseManager.addDatabase(application, " + dbConstClassName + "." + databaseConstName + ", " + databaseConstVersion + ", " + databaseViewsConstVersion + ");\n");
        }

        List<JavaVariable> params = Arrays.asList(new JavaVariable("AndroidDatabaseBaseManager", "databaseManager"));
        myClass.addMethod(Access.PUBLIC, "void", "identifyDatabases", params, content.toString());
    }

    private void createCreateNewDatabaseWrapper() {
        myClass.addImport("org.dbtools.android.domain.database.DatabaseWrapper");
        myClass.addImport("org.dbtools.android.domain.database.AndroidDatabaseWrapper");

        List<JavaVariable> params = Arrays.asList(new JavaVariable("AndroidDatabase", "androidDatabase"));
        myClass.addMethod(Access.PUBLIC, "DatabaseWrapper", "createNewDatabaseWrapper", params, "return new AndroidDatabaseWrapper(androidDatabase.getPath());");
    }

    private void createNewDBToolsLogger() {
        myClass.addImport("org.dbtools.android.domain.log.DBToolsAndroidLogger");
        myClass.addImport("org.dbtools.android.domain.log.DBToolsLogger");

        myClass.addMethod(Access.PUBLIC, "DBToolsLogger", "createNewDBToolsLogger", null, "return new DBToolsAndroidLogger();");
    }

    private void createNewDBToolsContentValues() {
        myClass.addImport("org.dbtools.android.domain.database.contentvalues.AndroidDBToolsContentValues");
        myClass.addImport("org.dbtools.android.domain.database.contentvalues.DBToolsContentValues");

        myClass.addMethod(Access.PUBLIC, "DBToolsContentValues", "createNewDBToolsContentValues", null, "return new AndroidDBToolsContentValues();");
    }

    private void addImports() {
        myClass.addImport("android.app.Application");
        myClass.addImport("org.dbtools.android.domain.config.DatabaseConfig");
        myClass.addImport("org.dbtools.android.domain.AndroidDatabase");
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
