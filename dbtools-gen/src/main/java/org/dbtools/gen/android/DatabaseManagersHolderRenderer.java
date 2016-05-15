package org.dbtools.gen.android;

import org.dbtools.codegen.java.Access;
import org.dbtools.codegen.java.JavaClass;
import org.dbtools.codegen.java.JavaMethod;
import org.dbtools.codegen.java.JavaVariable;
import org.dbtools.schema.schemafile.*;
import org.dbtools.util.JavaUtil;

import java.util.Arrays;
import java.util.List;

public class DatabaseManagersHolderRenderer {
    private String packageName;
    private JavaClass myClass;

    public void generate(SchemaDatabase database, String packageBase, String packageName, List<SchemaTable> tables, List<SchemaView> views, List<SchemaQuery> queries, String outDir) {
        System.out.println("Generating DatabaseManagersHolder...");

        this.packageName = packageName;

        String preName = database.getName().toLowerCase();

        // uppercase the first letter
        preName = Character.toString(preName.charAt(0)).toUpperCase() + preName.substring(1);

        String className = preName + "DatabaseManagers";
        myClass = new JavaClass(packageName, className);
        myClass.setCreateDefaultConstructor(false);
        myClass.setFileHeaderComment("/*\n * GENERATED FILE - DO NOT EDIT\n */\n");
        myClass.addAnnotation("@SuppressWarnings(\"all\")");

        StringBuilder initContent = new StringBuilder();

        for (SchemaTable table : tables) {
            if (!table.isEnumerationTable()) {
                addSchemaEntityToInit(initContent, table);
            }
        }

        for (SchemaView view : views) {
            addSchemaEntityToInit(initContent, view);
        }

        for (SchemaQuery query : queries) {
            addSchemaEntityToInit(initContent, query);
        }

        myClass.addImport(packageBase + ".DatabaseManager");

        List<JavaVariable> params = Arrays.asList(new JavaVariable("DatabaseManager", "databaseManager"));
        JavaMethod initMethod = myClass.addMethod(Access.PUBLIC, "void", "init", params, initContent.toString());
        initMethod.setStatic(true);

        myClass.writeToDisk(outDir, true);
    }

    private void addSchemaEntityToInit(StringBuilder initContent, SchemaEntity entity) {
        String managerClassName = AndroidManagerRenderer.getClassName(entity);
        String managerVarName = Character.toString(managerClassName.charAt(0)).toLowerCase() + managerClassName.substring(1);

        JavaVariable javaVariable = myClass.addVariable(managerClassName, managerVarName);
        javaVariable.setStatic(true);
        javaVariable.setGenerateGetter(true);

        initContent.append(managerVarName).append(" = new ").append(AndroidManagerRenderer.getClassName(entity)).append("(databaseManager);\n");

        myClass.addImport(JavaUtil.createTablePackageName(packageName, entity.getClassName()) + "." + managerClassName);
    }
}
