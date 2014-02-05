/*
 * AndroidDBObjectBuilder.java
 *
 * Created on Sep 9, 2010
 *
 * Copyright 2010 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.gen.android;


import org.dbtools.gen.DBObjectBuilder;
import org.dbtools.gen.GroupObjectBuilder;
import org.dbtools.schema.schemafile.SchemaDatabase;
import org.dbtools.schema.schemafile.SchemaTable;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Jeff
 */
public class AndroidDBObjectBuilder implements DBObjectBuilder {

    private AndroidBaseRecordClassRenderer baseRecordClass = new AndroidBaseRecordClassRenderer();
    private AndroidRecordClassRenderer recordClass = new AndroidRecordClassRenderer();
    private AndroidBaseRecordManager baseManagerClass;
    private AndroidRecordManager managerClass;

    private int filesGeneratedCount = 0;
    private List<String> filesGenerated = new ArrayList<String>();

    private SchemaDatabase database;
    private SchemaTable table;
    private String packageName;
    private String outDir;
    private String testOutDir;
    private PrintStream psLog;
    private String author;
    private String version;
    private boolean injectionSupport;

    /**
     * Creates a new instance of AndroidDBObjectBuilder.
     */
    public AndroidDBObjectBuilder() {
        this(false);
    }

    public AndroidDBObjectBuilder(boolean injectionSupport) {
        this.injectionSupport = injectionSupport;
        baseManagerClass = new AndroidBaseRecordManager(injectionSupport);
        managerClass = new AndroidRecordManager(injectionSupport);
    }

    public static void buildAll(String schemaFilename, String baseOutputDir, String basePackageName, boolean injectionSupport, boolean dateTimeSupport) {
        AndroidDBObjectBuilder androidBuilder = new AndroidDBObjectBuilder(injectionSupport);
        androidBuilder.setUseDateTime(dateTimeSupport);
        GroupObjectBuilder groupObjectBuilder = new GroupObjectBuilder();
        groupObjectBuilder.setXmlFilename(schemaFilename);
        groupObjectBuilder.setTables(null); // null means all
        groupObjectBuilder.setOutputBaseDir(baseOutputDir);
        groupObjectBuilder.setPackageBase(basePackageName);
        groupObjectBuilder.setObjectBuilder(androidBuilder);
        groupObjectBuilder.build();
        System.out.println("Generated [" + groupObjectBuilder.getObjectBuilder().getNumberFilesGenerated() + "] files.");
    }

    @Override
    public String getName() {
        return "Android Object Builder";
    }

    @Override
    public boolean build() {
        if (psLog == null) {
            psLog = System.out;
        }

//        psLog.println("Injection Support: " + injectionSupport);

        if (table != null) {
            psLog.println("SchemaTable: " + table.getName());
        } else {
            psLog.println("ERROR: SchemaTable is null");
            return false;
        }

//        System.out.println("Pre outdir: " + outDir);
        char lastDirChar = outDir.charAt(outDir.length() - 1);
        if (lastDirChar != File.separatorChar) {
            outDir += File.separatorChar;
        }

        // Managers
        if (!table.isEnumerationTable()) {
            String managerFileName = outDir + AndroidRecordManager.getClassName(table) + ".java";
//            System.out.println("ManagerFilename: " + managerFileName);

            //File baseManagerFile = new File(baseSEManagerFileName);
            File managerFile = new File(managerFileName);

            // Base Manager
            baseManagerClass.generateObjectCode(table, packageName, author, version, psLog);
            baseManagerClass.writeToFile(outDir);

            filesGeneratedCount++;

            // Manager
            if (!managerFile.exists()) {
                managerClass.generateObjectCode(table, packageName, author, version, psLog);
                managerClass.writeToFile(outDir);

                filesGeneratedCount++;
            }
        }

        // Entities
        String baseRecordFileName = outDir + AndroidRecordClassRenderer.createClassName(table) + ".java";
        String recordFileName = outDir + AndroidRecordClassRenderer.createClassName(table) + ".java";
        File baseRecordFile = new File(baseRecordFileName);
        File recordFile = new File(recordFileName);
        File recordTestFile = new File(recordFileName + "Test");


        // BaseRecord
        baseRecordClass.generateObjectCode(database, table, packageName, psLog);
        baseRecordClass.writeToFile(outDir);

        if (testOutDir != null && testOutDir.length() > 0) {
            baseRecordClass.writeTestsToFile(testOutDir);
        }

        filesGenerated.add(baseRecordFile.getPath());
        filesGeneratedCount++;

        // Record
        if (!table.isEnumerationTable()) {
            if (!recordFile.exists()) {
                recordClass.generateObjectCode(database, table, packageName, author, version, psLog);
                recordClass.writeToFile(outDir);

                filesGenerated.add(recordFile.getPath());
                filesGeneratedCount++;
            }

            // if this test does not exist then write it
            if (testOutDir != null && testOutDir.length() > 0 && !recordTestFile.exists()) {
                recordClass.writeTestsToFile(testOutDir, table, packageName);
            }

        }
        return true;
    }

    @Override
    public int getNumberFilesGenerated() {
        return filesGeneratedCount;
    }

    @Override
    public List<String> getFilesGenerated() {
        return Collections.unmodifiableList(filesGenerated);
    }

    @Override
    public void setIncludeXMLSupport(boolean b) {
        baseRecordClass.setIncludeXML(b);
    }

    @Override
    public void setUseDateTime(boolean b) {
        baseRecordClass.setUseDateTime(b);
    }

    @Override
    public void setTable(SchemaTable table) {
        this.table = table;
    }

    @Override
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public void setSourceOutputDir(String outDir) {
        this.outDir = outDir;
    }

    @Override
    public void setTestOutputDir(String outDir) {
        this.testOutDir = outDir;
    }

    @Override
    public void setLogPrintStream(PrintStream psLog) {
        this.psLog = psLog;
    }

    @Override
    public void setAuthor(String author) {
        this.author = author;
    }

    @Override
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public void setDatabase(SchemaDatabase dbSchema) {
        this.database = dbSchema;
    }

    @Override
    public void setProperty(String key, Object value) {
    }

    @Override
    public void setSpringSupport(boolean b) {
    }
}
