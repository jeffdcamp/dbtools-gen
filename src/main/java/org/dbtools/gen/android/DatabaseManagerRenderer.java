package org.dbtools.gen.android;

import org.dbtools.codegen.java.Access;
import org.dbtools.codegen.java.JavaClass;
import org.dbtools.codegen.java.JavaMethod;
import org.dbtools.codegen.java.JavaVariable;
import org.dbtools.gen.GenConfig;
import org.dbtools.schema.schemafile.DatabaseSchema;
import org.dbtools.schema.schemafile.SchemaDatabase;
import org.dbtools.util.JavaUtil;

import java.util.Arrays;
import java.util.List;

/**
 * User: jcampbell
 * Date: 1/18/14
 */
public class DatabaseManagerRenderer {

    private JavaClass myClass;
    public final String dbConstClassName = "DatabaseManagerConst";

    private String packageBase;
    private String outDir;
    private GenConfig genConfig;

    public void generate(DatabaseSchema databaseSchema) {
        System.out.println("Generating DatabaseManager...");

        String className = "DatabaseManager";
        myClass = new JavaClass(packageBase, className);


        myClass.setExtends("DatabaseBaseManager"); // extend the generated base class
        myClass.setCreateDefaultConstructor(false);
        if (genConfig.isInjectionSupport()) {
            myClass.addAnnotation("Singleton");

            List<JavaVariable> params = Arrays.asList(new JavaVariable("Application", "application"));
            JavaMethod defaultConstructor = myClass.addConstructor(Access.PUBLIC, params, "this.application = application;");
            defaultConstructor.addAnnotation("javax.inject.Inject");
        }
        addImports();

        JavaVariable contextVariable = myClass.addVariable("Application", "application");
        if (genConfig.isInjectionSupport()) {
            contextVariable.setAccess(Access.DEFAULT_NONE);
            contextVariable.addAnnotation("Inject");
        } else {
            contextVariable.setGenerateSetter(true);
        }

        createIdentifyDatabases(databaseSchema);
        createCreateNewDatabaseWrapper();
        createOnUpgrade();
        createOnUpgradeViews();

        myClass.writeToDisk(outDir, false);
    }

    private void createIdentifyDatabases(DatabaseSchema databaseSchema) {
        StringBuilder content = new StringBuilder();

        for (SchemaDatabase database : databaseSchema.getDatabases()) {
            String databaseConstName = JavaUtil.nameToJavaConst(database.getName()) + "_DATABASE_NAME";
            String databaseConstVersion = database.getName() + "TablesVersion";
            String databaseViewsConstVersion = database.getName() + "ViewsVersion";

            content.append("addDatabase(application, " + dbConstClassName + "." + databaseConstName + ", " + databaseConstVersion + ", " + databaseViewsConstVersion + ");\n");

            myClass.addVariable("int", databaseConstVersion, "1").setFinal(true);
            myClass.addVariable("int", databaseViewsConstVersion, "1").setFinal(true);
        }

        myClass.addMethod(Access.PUBLIC, "void", "identifyDatabases", content.toString());
    }

    private void createCreateNewDatabaseWrapper() {
        myClass.addImport("org.dbtools.android.domain.database.DatabaseWrapper");
        myClass.addImport("org.dbtools.android.domain.database.AndroidDatabaseWrapper");

        List<JavaVariable> params = Arrays.asList(new JavaVariable("AndroidDatabase", "androidDatabase"));
        myClass.addMethod(Access.PUBLIC, "DatabaseWrapper", "createNewDatabaseWrapper", params, "return new AndroidDatabaseWrapper(androidDatabase.getPath());");
    }

    private void createOnUpgrade() {
        StringBuilder content = new StringBuilder();

        content.append("String databaseName = androidDatabase.getName();\n");
        content.append("Log.i(TAG, \"Upgrading database [\" + databaseName + \"] from version \" + oldVersion + \" to \" + newVersion);\n");


        List<JavaVariable> params = Arrays.asList(new JavaVariable("AndroidDatabase", "androidDatabase"),
                new JavaVariable("int", "oldVersion"),
                new JavaVariable("int", "newVersion"));
        myClass.addMethod(Access.PUBLIC, "void", "onUpgrade", params, content.toString());
    }

    private void createOnUpgradeViews() {
        StringBuilder content = new StringBuilder();

        content.append("String databaseName = androidDatabase.getName();\n");
        content.append("Log.i(TAG, \"Upgrading database [\" + databaseName + \"] VIEWS from version \" + oldVersion + \" to \" + newVersion);\n");


        List<JavaVariable> params = Arrays.asList(new JavaVariable("AndroidDatabase", "androidDatabase"),
                new JavaVariable("int", "oldVersion"),
                new JavaVariable("int", "newVersion"));

        content.append("// automatically drop/create views\n");
        content.append("super.onUpgradeViews(androidDatabase, oldVersion, newVersion);\n");

        myClass.addMethod(Access.PUBLIC, "void", "onUpgradeViews", params, content.toString());
    }

    private void addImports() {
        myClass.addImport("android.util.Log");
        myClass.addImport("android.app.Application");
        myClass.addImport("org.dbtools.android.domain.AndroidDatabase");

        if (genConfig.isInjectionSupport()) {
            myClass.addImport("javax.inject.Inject");
            myClass.addImport("javax.inject.Singleton");
        }
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
