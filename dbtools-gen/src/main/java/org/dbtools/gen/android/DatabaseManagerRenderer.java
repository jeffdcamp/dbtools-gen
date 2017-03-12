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
    private String packageBase;
    private String outDir;
    private GenConfig genConfig;

    public void generate(DatabaseSchema databaseSchema) {
        System.out.println("Generating DatabaseManager...");

        String className = "DatabaseManager";
        myClass = new JavaClass(packageBase, className);

        myClass.setExtends("DatabaseBaseManager"); // extend the generated base class
        myClass.setCreateDefaultConstructor(false);

        List<JavaVariable> params = Arrays.asList(new JavaVariable("DatabaseConfig", "databaseConfig"));
        JavaMethod defaultConstructor = myClass.addConstructor(Access.PUBLIC, params, "super(databaseConfig);");

        if (genConfig.isInjectionSupport()) {
            myClass.addAnnotation("Singleton");
            defaultConstructor.addAnnotation("javax.inject.Inject");
        }
        addImports();

        createDatabaseVersions(databaseSchema);
        createOnUpgrade();
        createOnUpgradeViews();

        myClass.writeToDisk(outDir, false);
    }

    private void createDatabaseVersions(DatabaseSchema databaseSchema) {
        for (SchemaDatabase database : databaseSchema.getDatabases()) {
            String databaseConstVersion = JavaUtil.nameToJavaConst(database.getName(true) + "TablesVersion");
            String databaseViewsConstVersion = JavaUtil.nameToJavaConst(database.getName(true) + "ViewsVersion");

            myClass.addConstant("int", databaseConstVersion, "1").setFinal(true);
            myClass.addConstant("int", databaseViewsConstVersion, "1").setFinal(true);
        }
    }

    private void createOnUpgrade() {
        StringBuilder content = new StringBuilder();

        content.append("String databaseName = androidDatabase.getName();\n");
        content.append("getLogger().i(TAG, \"Upgrading database [\" + databaseName + \"] from version \" + oldVersion + \" to \" + newVersion);\n");


        List<JavaVariable> params = Arrays.asList(new JavaVariable("AndroidDatabase", "androidDatabase"),
                new JavaVariable("int", "oldVersion"),
                new JavaVariable("int", "newVersion"));
        myClass.addMethod(Access.PUBLIC, "void", "onUpgrade", params, content.toString());
    }

    private void createOnUpgradeViews() {
        StringBuilder content = new StringBuilder();

        content.append("String databaseName = androidDatabase.getName();\n");
        content.append("getLogger().i(TAG, \"Upgrading database [\" + databaseName + \"] VIEWS from version \" + oldVersion + \" to \" + newVersion);\n");


        List<JavaVariable> params = Arrays.asList(new JavaVariable("AndroidDatabase", "androidDatabase"),
                new JavaVariable("int", "oldVersion"),
                new JavaVariable("int", "newVersion"));

        content.append("// automatically drop/create views\n");
        content.append("super.onUpgradeViews(androidDatabase, oldVersion, newVersion);\n");

        myClass.addMethod(Access.PUBLIC, "void", "onUpgradeViews", params, content.toString());
    }

    private void addImports() {
        myClass.addImport("org.dbtools.android.domain.AndroidDatabase");
        myClass.addImport("org.dbtools.android.domain.config.DatabaseConfig");

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
