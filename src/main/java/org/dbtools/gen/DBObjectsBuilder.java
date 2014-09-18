/*
 * GroupObjectBuilder.java
 *
 * Created on March 15, 2003
 *
 * Copyright 2006 Jeff Campbell. All rights reserved. Unauthorized reproduction 
 * is a violation of applicable law. This material contains certain 
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.gen;


import org.dbtools.renderer.SchemaRenderer;
import org.dbtools.schema.schemafile.*;
import org.dbtools.util.JavaUtil;
import org.dbtools.util.PackageUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

/**
 * @author jeff
 */
public abstract class DBObjectsBuilder {

    private String xmlFilename;
    private String schemaDatabaseName;
    private List<SchemaTable> tables;
    private List<SchemaView> views;
    private List<SchemaQuery> queries;
    private String outputBaseDir;
    private String packageBase;
    // used internally
    private SchemaRenderer schemaRenderer;
    private boolean injectionSupport;
    private boolean jsr305Support;
    private boolean encryptionSupport;
    private boolean dateTimeSupport;
    private boolean javaeeSupport;
    private boolean includeDatabaseNameInPackage;

    public abstract DBObjectBuilder getObjectBuilder();

    int numberFilesGenerated;

    /**
     * Creates a new instance of DBObjectsBuilder
     */
    public DBObjectsBuilder() {
        schemaRenderer = new SchemaRenderer();
    }

    public DBObjectsBuilder(String xmlFilename, String packageBase, String outputBaseDir) {
        setXmlFilename(xmlFilename);
        setPackageBase(packageBase);
        setOutputBaseDir(outputBaseDir);
    }

    private void resetData() {
        tables = null;
        views = null;
        queries = null;
        numberFilesGenerated = 0;
    }

    private boolean validate() {
        if (tables.size() == 0) {
            throw new IllegalStateException("No tables specified");
        }
        if (outputBaseDir == null || outputBaseDir.equals("")) {
            throw new IllegalStateException("No baseDir specified");
        }

        return true;
    }

    public boolean build() {
        if (schemaDatabaseName != null && !schemaDatabaseName.isEmpty()) {
            return build(schemaRenderer.getDbSchema().getDatabase(schemaDatabaseName));
        } else {
            return buildAllDatabases();
        }
    }

    private boolean buildAllDatabases() {
        DatabaseSchema databaseSchema = schemaRenderer.getDbSchema();
        for (SchemaDatabase database : databaseSchema.getDatabases()) {
            resetData(); // prepare for new database
            if (!build(database)) {
                return false;
            }
        }

        onPostBuild(databaseSchema, packageBase, outputBaseDir, injectionSupport, encryptionSupport, jsr305Support, includeDatabaseNameInPackage);

        return true;
    }

    public void onPostBuild(DatabaseSchema databaseSchema, String packageBase, String outputBaseDir,
                            boolean injectionSupport,
                            boolean encryptionSupport,
                            boolean jsr305Support,
                            boolean includeDatabaseNameInPackage) {
    }

    private boolean build(SchemaDatabase database) {
        boolean success = true;

        System.out.println("DATABASE: " + database.getName());

        // if tables is null... assume all tables
        if (tables == null) {
            tables = schemaRenderer.getTablesToGenerate(database, null);
        }

        if (views == null) {
            views = schemaRenderer.getViewsToGenerate(database, null);
        }

        if (queries == null) {
            queries = schemaRenderer.getQueriesToGenerate(database, null);
        }

        if (validate()) {
            DBObjectBuilder objectBuilder = getObjectBuilder();

            if (objectBuilder == null) {
                throw new IllegalStateException("No Table Renderer specified");
            }

            for (SchemaTable table : tables) {
                // crete the directory
                String packageDir = table.getClassName().toLowerCase();

                if (includeDatabaseNameInPackage) {
                    packageDir = database.getName().toLowerCase() + "/" + packageDir;
                }

                String outDir = createOutputDir(outputBaseDir, packageDir);

                // package
                String packageName = packageBase + (includeDatabaseNameInPackage ? '.' + database.getName().toLowerCase() : "");
                packageName = JavaUtil.createTablePackageName(packageName, table.getClassName());

                objectBuilder.setDatabase(database);
                objectBuilder.setEntity(table);
                objectBuilder.setPackageName(packageName);
                objectBuilder.setSourceOutputDir(outDir);
                objectBuilder.setInjectionSupport(hasInjectionSupport());
                objectBuilder.setDateTimeSupport(hasDateTimeSupport());
                objectBuilder.setJavaeeSupport(hasJavaEESupport());
                objectBuilder.setEncryptionSupport(hasEncryptionSupport());
                objectBuilder.setIncludeDatabaseNameInPackage(includeDatabaseNameInPackage);

                success = objectBuilder.build();
                numberFilesGenerated += objectBuilder.getNumberFilesGenerated();
            }

            for (SchemaView view : views) {
                // crete the directory
                String packageDir = view.getClassName().toLowerCase();

                if (includeDatabaseNameInPackage) {
                    packageDir = database.getName().toLowerCase() + "/" + packageDir;
                }

                String outDir = createOutputDir(outputBaseDir, packageDir);

                // package
                String packageName = packageBase + (includeDatabaseNameInPackage ? '.' + database.getName().toLowerCase() : "");
                packageName = packageName + "." + view.getClassName().toLowerCase();

                objectBuilder.setDatabase(database);
                objectBuilder.setEntity(view);
                objectBuilder.setPackageName(packageName);
                objectBuilder.setSourceOutputDir(outDir);
                objectBuilder.setInjectionSupport(hasInjectionSupport());
                objectBuilder.setDateTimeSupport(hasDateTimeSupport());
                objectBuilder.setJavaeeSupport(hasJavaEESupport());
                objectBuilder.setEncryptionSupport(hasEncryptionSupport());
                objectBuilder.setIncludeDatabaseNameInPackage(includeDatabaseNameInPackage);

                success = objectBuilder.build();
                numberFilesGenerated += objectBuilder.getNumberFilesGenerated();
            }

            for (SchemaQuery query : queries) {
                // crete the directory
                String packageDir = query.getClassName().toLowerCase();

                if (includeDatabaseNameInPackage) {
                    packageDir = database.getName().toLowerCase() + "/" + packageDir;
                }

                String outDir = createOutputDir(outputBaseDir, packageDir);

                // package
                String packageName = packageBase + (includeDatabaseNameInPackage ? '.' + database.getName().toLowerCase() : "");
                packageName = packageName + "." + query.getClassName().toLowerCase();

                objectBuilder.setDatabase(database);
                objectBuilder.setEntity(query);
                objectBuilder.setPackageName(packageName);
                objectBuilder.setSourceOutputDir(outDir);
                objectBuilder.setInjectionSupport(hasInjectionSupport());
                objectBuilder.setDateTimeSupport(hasDateTimeSupport());
                objectBuilder.setJavaeeSupport(hasJavaEESupport());
                objectBuilder.setEncryptionSupport(hasEncryptionSupport());
                objectBuilder.setIncludeDatabaseNameInPackage(includeDatabaseNameInPackage);

                success = objectBuilder.build();
                numberFilesGenerated += objectBuilder.getNumberFilesGenerated();
            }
        }

        return success;
    }

    private String createOutputDir(String baseDir, String name) {
        String outDir = baseDir + "/" + name;
        File newDir = new File(outDir);
        if (!newDir.exists()) {
            newDir.mkdirs();
        }

        return outDir;
    }

    /**
     * Reread xml file
     */
    private void scanXMLFile() {
        File xmlFile = getXmlFile();
        if (xmlFile != null) {
            saveXsdFileToSchemaDir();
            schemaRenderer.readXMLSchema(xmlFile.getPath());
        }
    }

    private File getXmlFile() {
        String filename = xmlFilename;
        if (filename != null && !filename.isEmpty()) {
            return new File(filename);
        }

        return null;
    }

    private void saveXsdFileToSchemaDir() {
        String filename = "dbschema.xsd";
        try {
            InputStream in = this.getClass().getResourceAsStream("/org/dbtools/xml/" + filename);

            File outFile = new File(getXmlFile().getParent(), filename);
            FileOutputStream fos = new FileOutputStream(outFile);
            int read;
            byte[] bytes = new byte[1024];

            while ((read = in.read(bytes)) != -1) {
                fos.write(bytes, 0, read);
            }
        } catch (Exception e) {
            System.out.println("Failed to write: " + filename + " Error: [" + e.getMessage() + "]");
        }
    }

    public String getGeneratedPackageName() {
        return PackageUtil.getPackageFromFilePath(outputBaseDir);
    }

    public String getSchemaDatabaseName() {
        return schemaDatabaseName;
    }

    public void setSchemaDatabaseName(String schemaDatabaseName) {
        this.schemaDatabaseName = schemaDatabaseName;
    }

    public List getTables() {
        return tables;
    }

    public void setTables(List tables) {
        this.tables = tables;
    }

    public String getPackageBase() {
        return packageBase;
    }

    public void setPackageBase(String packageBase) {
        this.packageBase = packageBase;
    }

    public String getXmlFilename() {
        return xmlFilename;
    }

    public void setXmlFilename(String xmlFilename) {
        this.xmlFilename = xmlFilename;
        scanXMLFile();
    }

    public String getOutputBaseDir() {
        return outputBaseDir;
    }

    public void setOutputBaseDir(String outputBaseDir) {
        this.outputBaseDir = outputBaseDir;
    }

    public void setInjectionSupport(boolean injectionSupport) {
        this.injectionSupport = injectionSupport;
    }

    public boolean hasInjectionSupport() {
        return injectionSupport;
    }

    public void setJsr305Support(boolean jsr305Support) {
        this.jsr305Support = jsr305Support;
    }

    public boolean hasJsr305Support() {
        return jsr305Support;
    }

    public boolean hasEncryptionSupport() {
        return encryptionSupport;
    }

    public void setEncryptionSupport(boolean encryptionSupport) {
        this.encryptionSupport = encryptionSupport;
    }

    public void setDateTimeSupport(boolean dateTimeSupport) {
        this.dateTimeSupport = dateTimeSupport;
    }

    public boolean hasDateTimeSupport() {
        return dateTimeSupport;
    }

    public boolean hasJavaEESupport() {
        return javaeeSupport;
    }

    public void setJavaeeSupport(boolean javaeeSupport) {
        this.javaeeSupport = javaeeSupport;
    }

    public int getNumberFilesGenerated() {
        return numberFilesGenerated;
    }

    public boolean isIncludeDatabaseNameInPackage() {
        return includeDatabaseNameInPackage;
    }

    public void setIncludeDatabaseNameInPackage(boolean includeDatabaseNameInPackage) {
        this.includeDatabaseNameInPackage = includeDatabaseNameInPackage;
    }
}
