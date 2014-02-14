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
import org.dbtools.schema.schemafile.SchemaDatabase;
import org.dbtools.schema.schemafile.SchemaTable;
import org.dbtools.schema.schemafile.SchemaView;
import org.dbtools.util.PackageUtil;

import java.io.File;
import java.util.List;

/**
 * @author jeff
 */
public abstract class DBObjectBuilder {

    private String xmlFilename;
    private String schemaDatabaseName;
    private List<SchemaTable> tables;
    private List<SchemaView> views;
    private String outputBaseDir;
    private String packageBase;
    // used internally
    private SchemaRenderer schemaRenderer;
    private boolean injectionSupport;
    private boolean dateTimeSupport;
    private boolean springSupport;

    public abstract DBTableObjectBuilder getTableObjectBuilder();
    public abstract DBViewObjectBuilder getViewObjectBuilder();

    int numberFilesGenerated;

    /**
     * Creates a new instance of DBObjectBuilder
     */
    public DBObjectBuilder() {
        schemaRenderer = new SchemaRenderer();
    }

    public DBObjectBuilder(String xmlFilename, String packageBase, String outputBaseDir) {
        setXmlFilename(xmlFilename);
        setPackageBase(packageBase);
        setOutputBaseDir(outputBaseDir);
    }

    private void resetData() {
        tables = null;
        views = null;
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
        for (SchemaDatabase database : schemaRenderer.getDbSchema().getDatabases()) {
            resetData(); // prepare for new database
            if (!build(database)) {
                return false;
            }
        }

        return true;
    }

    private boolean build(SchemaDatabase database) {
        boolean success = true;

        // if tables is null... assume all tables
        if (tables == null) {
            tables = schemaRenderer.getTablesToGenerate(database, null);
        }

        if (views == null) {
            views = schemaRenderer.getViewsToGenerate(database, null);
        }

        if (validate()) {
            DBTableObjectBuilder tableObjectBuilder = getTableObjectBuilder();
            DBViewObjectBuilder viewObjectBuilder = getViewObjectBuilder();

            if (tableObjectBuilder == null) {
                throw new IllegalStateException("No Table Renderer specified");
            }

//            if (viewObjectBuilder == null) { // TODO Fix!
//                throw new IllegalStateException("No View Renderer specified");
//            }

            for (SchemaTable table : tables) {
                // crete the directory
                String outDir = createOutputDir(table.getClassName().toLowerCase());

                // package
                String packageName = packageBase + "." + table.getClassName().toLowerCase();

                tableObjectBuilder.setDatabase(database);
                tableObjectBuilder.setTable(table);
                tableObjectBuilder.setPackageName(packageName);
                tableObjectBuilder.setSourceOutputDir(outDir);
                tableObjectBuilder.setInjectionSupport(hasInjectionSupport());
                tableObjectBuilder.setDateTimeSupport(hasDateTimeSupport());
                tableObjectBuilder.setSpringSupport(hasSpringSupport());

                success = tableObjectBuilder.build();
                numberFilesGenerated += tableObjectBuilder.getNumberFilesGenerated();
            }

            for (SchemaView view : views) {
                // crete the directory
                String outDir = createOutputDir(view.getClassName().toLowerCase());

                // package
                String packageName = packageBase + "." + view.getClassName().toLowerCase();

                viewObjectBuilder.setDatabase(database);
                viewObjectBuilder.setView(view);
                viewObjectBuilder.setPackageName(packageName);
                viewObjectBuilder.setSourceOutputDir(outDir);
                viewObjectBuilder.setInjectionSupport(hasInjectionSupport());
                viewObjectBuilder.setDateTimeSupport(hasDateTimeSupport());
                viewObjectBuilder.setSpringSupport(hasSpringSupport());

                success = viewObjectBuilder.build();
                numberFilesGenerated += viewObjectBuilder.getNumberFilesGenerated();
            }
        }

        return success;
    }

    private String createOutputDir(String name) {
        String outDir = outputBaseDir + "/" + name;
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
        // read the schemaRenderer
        File xmlFile = null;
        String filename = xmlFilename;
        if (!filename.equals("")) {
            xmlFile = new File(filename);
            schemaRenderer.readXMLSchema(xmlFile.getPath());
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

    public void setDateTimeSupport(boolean dateTimeSupport) {
        this.dateTimeSupport = dateTimeSupport;
    }

    public boolean hasDateTimeSupport() {
        return dateTimeSupport;
    }

    public boolean hasSpringSupport() {
        return springSupport;
    }

    public void setSpringSupport(boolean springSupport) {
        this.springSupport = springSupport;
    }

    public int getNumberFilesGenerated() {
        return numberFilesGenerated;
    }
}
