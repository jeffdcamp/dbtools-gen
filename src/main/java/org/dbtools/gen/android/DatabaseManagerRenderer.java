package org.dbtools.gen.android;

import org.dbtools.codegen.Access;
import org.dbtools.codegen.JavaClass;
import org.dbtools.codegen.JavaVariable;
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
    private static final String TAB = JavaClass.getTab();

    private String packageBase;
    private String outDir;
    private boolean injectionSupport = false;
    private boolean encryptionSupport = false; // use SQLCipher

    public void generate(DatabaseSchema databaseSchema) {
        System.out.println("Generating DatabaseManager...");

        String className = "DatabaseManager";
        myClass = new JavaClass(packageBase, className);
        myClass.setExtends("DatabaseBaseManager"); // extend the generated base class
        myClass.setCreateDefaultConstructor(false);
        if (injectionSupport) {
            myClass.addAnnotation("Singleton");
        }
        addImports();

        JavaVariable contextVariable = myClass.addVariable("Context", "context");
        contextVariable.setGenerateSetter(true);
        if (injectionSupport) {
            contextVariable.setAccess(Access.DEFAULT_NONE);
            contextVariable.addAnnotation("Inject");
        }

        createIdentifyDatabases(databaseSchema);
        createOnUpgrade();
        createOnUpgradeViews();

        myClass.writeToDisk(outDir, false);
    }

    private void createIdentifyDatabases(DatabaseSchema databaseSchema) {
        StringBuilder content = new StringBuilder();

        for (SchemaDatabase database : databaseSchema.getDatabases()) {
            String databaseConstName = JavaUtil.nameToJavaConst(database.getName()) + "_DATABASE_NAME";
            String databaseConstVersion = JavaUtil.nameToJavaConst(database.getName()) + "_VERSION";

            content.append("addDatabase(context, " + databaseConstName + ", " + databaseConstVersion + ");\n");

            myClass.addConstant("int", databaseConstVersion, "1");
        }

        myClass.addMethod(Access.PUBLIC, "void", "identifyDatabases", content.toString());
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
        myClass.addImport("android.content.Context");
        myClass.addImport("org.dbtools.android.domain.AndroidDatabase");

        if (injectionSupport) {
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

    public void setInjectionSupport(boolean injectionSupport) {
        this.injectionSupport = injectionSupport;
    }

    public void setEncryptionSupport(boolean encryptionSupport) {
        this.encryptionSupport = encryptionSupport;
    }
}
