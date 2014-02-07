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
import org.dbtools.util.PackageUtil;

import java.io.File;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

/**
 * @author jeff
 */
public class GroupObjectBuilder {

    // used for preparation
    // used for building objects
    private String xmlFilename;
    private String schemaDatabaseName;
    private List<SchemaTable> tables;
    private String outputBaseDir;
    private String testOutputBaseDir;
    private String packageBase;
    private String author;
    private String version;
    private PrintStream psLog;
    // used internally
    private DBObjectBuilder objectBuilder;
    private SchemaRenderer schemaRenderer;

    /**
     * Creates a new instance of GroupObjectBuilder
     */
    public GroupObjectBuilder() {
        author = "";
        version = "";

        schemaRenderer = new SchemaRenderer();
    }

    public GroupObjectBuilder(String xmlFilename, String packageBase, String outputBaseDir) {
        setXmlFilename(xmlFilename);
        setPackageBase(packageBase);
        setOutputBaseDir(outputBaseDir);
    }

    private void resetData() {
        tables = null;
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

        if (validate()) {
            if (getObjectBuilder() == null) {
                throw new IllegalStateException("No Renderer specified");
            }


            Iterator<SchemaTable> itr = tables.iterator();
            while (itr.hasNext() && success) {
                SchemaTable table = itr.next();

                // crete the directory
                String outDir = outputBaseDir + "/" + table.getClassName().toLowerCase();
                File newDir = new File(outDir);
                if (!newDir.exists()) {
                    newDir.mkdirs();
                }

                String testOutDir = "";
                if (createTests()) {
                    testOutDir = testOutputBaseDir + "/" + table.getClassName().toLowerCase();
                    File newTestDir = new File(testOutDir);
                    if (!newTestDir.exists()) {
                        newTestDir.mkdirs();
                    }
                }


                // package
                String packageName = packageBase + "." + table.getClassName().toLowerCase();

                DBObjectBuilder objBuilder = getObjectBuilder();
                objBuilder.setDatabase(database);
                objBuilder.setTable(table);
                objBuilder.setPackageName(packageName);
                objBuilder.setSourceOutputDir(outDir);
                objBuilder.setTestOutputDir(testOutDir);
                objBuilder.setAuthor(author);
                objBuilder.setVersion(version);
                objBuilder.setLogPrintStream(psLog);

                success = objBuilder.build();
            }
        }

        return success;
    }

    public boolean createTests() {
        return (testOutputBaseDir != null && testOutputBaseDir.length() > 0);
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

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public PrintStream getPsLog() {
        return psLog;
    }

    public void setPsLog(PrintStream psLog) {
        this.psLog = psLog;
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

    public DBObjectBuilder getObjectBuilder() {
        return objectBuilder;
    }

    public void setObjectBuilder(DBObjectBuilder objectBuilder) {
        this.objectBuilder = objectBuilder;
    }

    public String getTestOutputBaseDir() {
        return testOutputBaseDir;
    }

    public void setTestOutputBaseDir(String testOutputBaseDir) {
        this.testOutputBaseDir = testOutputBaseDir;
    }
}
