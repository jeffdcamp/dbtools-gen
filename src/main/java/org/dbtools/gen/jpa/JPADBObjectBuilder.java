/*
 * JPADBObjectBuilder.java
 *
 * Created on November 1, 2006, 7:30 PM
 *
 * Copyright 2007 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.gen.jpa;

import org.dbtools.gen.DBObjectBuilder;
import org.dbtools.schema.schemafile.SchemaDatabase;
import org.dbtools.schema.schemafile.SchemaTable;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jeff
 */
public class JPADBObjectBuilder implements DBObjectBuilder {

    private JPAJSEBaseRecordManager baseSEManagerClass = new JPAJSEBaseRecordManager();
    private JPAJSERecordManager seManagerClass = new JPAJSERecordManager();
    private JPAJEEBaseRecordManager baseEEManagerClass = new JPAJEEBaseRecordManager();
    private JPAJEERecordManager eeManagerClass = new JPAJEERecordManager();
    private JPABaseRecordClassRenderer baseRecordClass = new JPABaseRecordClassRenderer();
    private JPARecordClassRenderer recordClass = new JPARecordClassRenderer();
    private int filesGeneratedCount = 0;
    private List<String> filesGenerated = new ArrayList<String>();

    public static enum JPAManagerType {JAVAEE, JAVASE}

    private JPAManagerType managerType = JPAManagerType.JAVASE;
    private boolean jeeLocalInterface = false;
    private boolean jeeRemoteInterface = false;
    private boolean springSupport = false;
    private SchemaDatabase database;
    private SchemaTable table;
    private String packageName;
    private String outDir;
    private String testOutDir;
    private PrintStream psLog;
    private String author;
    private String version;

    /**
     * Creates a new instance of JPADBObjectBuilder
     */
    public JPADBObjectBuilder() {
    }

    @Override
    public String getName() {
        return "JPA Object Builder";
    }

    @Override
    public boolean build() {
        if (psLog == null) {
            psLog = System.out;
        }

        if (table != null) {
            psLog.println("SchemaTable: " + table.getName());
        } else {
            psLog.println("ERROR: SchemaTable is null");
            return false;
        }

        char lastDirChar = outDir.charAt(outDir.length() - 1);
        if (lastDirChar != '\\' || lastDirChar != '/') {
            if (outDir.charAt(0) == '/') {
                outDir += "/";
            } else {
                outDir += "\\";
            }
        }

        // Managers
        if (!table.isEnumerationTable()) {
            if (managerType == JPAManagerType.JAVASE) {
                //String baseSEManagerFileName = outDir + JPAJSEBaseRecordManager.getClassName(table)+".java";
                String managerFileName = outDir + JPAJSERecordManager.getClassName(table) + ".java";

                //File baseManagerFile = new File(baseSEManagerFileName);
                File managerFile = new File(managerFileName);

                // Base Manager
                baseSEManagerClass.generateObjectCode(table, packageName, author, version, psLog);
                baseSEManagerClass.writeToFile(outDir);

                filesGeneratedCount++;

                // Manager
                if (!managerFile.exists()) {
                    seManagerClass.generateObjectCode(table, packageName, author, version, psLog);
                    seManagerClass.writeToFile(outDir);

                    filesGeneratedCount++;
                }
            } else {
                //String baseEEManagerFileName = outDir + JPAJEEBaseRecordManager.getClassName(table)+".java";
                String managerFileName = outDir + JPAJEERecordManager.getClassName(table) + ".java";

                //File baseEEManagerFile = new File(baseEEManagerFileName);
                File managerFile = new File(managerFileName);

                // Base Manager
                baseEEManagerClass.generateObjectCode(table, packageName, author, version, psLog);
                baseEEManagerClass.writeToFile(outDir);

                filesGeneratedCount++;


                // Manager
                if (!managerFile.exists()) {
                    eeManagerClass.generateObjectCode(table, packageName, author, version, psLog);
                    eeManagerClass.writeToFile(outDir);

                    filesGeneratedCount++;
                }
            }
        }

        // Entities
        String baseRecordFileName = outDir + JPABaseRecordClassRenderer.createClassName(table) + ".java";
        String recordFileName = outDir + JPARecordClassRenderer.createClassName(table) + ".java";
        File baseRecordFile = new File(baseRecordFileName);
        File recordFile = new File(recordFileName);
        File recordTestFile = new File(recordFileName + "Test");


        // BaseRecord
        baseRecordClass.generateObjectCode(database, table, packageName, author, version, psLog);
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
        return filesGenerated;
    }

    @Override
    public void setIncludeXMLSupport(boolean b) {
        baseRecordClass.setIncludeXML(b);
    }

    @Override
    public void setUseDateTime(boolean b) {
        baseRecordClass.setUseDateTime(b);
    }

    public JPAManagerType getManagerType() {
        return managerType;
    }

    public void setManagerType(JPAManagerType managerType) {
        this.managerType = managerType;
    }

    public boolean isJeeLocalInterface() {
        return jeeLocalInterface;
    }

    public void setJeeLocalInterface(boolean jeeLocalInterface) {
        baseEEManagerClass.setLocalInterfaceRequired(jeeLocalInterface);
        eeManagerClass.setLocalInterfaceRequired(jeeLocalInterface);
        this.jeeLocalInterface = jeeLocalInterface;
    }

    public boolean isJeeRemoteInterface() {
        return jeeRemoteInterface;
    }

    public void setJeeRemoteInterface(boolean jeeRemoteInterface) {
        baseEEManagerClass.setRemoteInterfaceRequired(jeeRemoteInterface);
        eeManagerClass.setRemoteInterfaceRequired(jeeRemoteInterface);
        this.jeeRemoteInterface = jeeRemoteInterface;
    }

    public boolean isSpringSupport() {
        return springSupport;
    }

    public void setSpringSupport(boolean springSupport) {
        this.springSupport = springSupport;
        baseSEManagerClass.setSpringSupport(springSupport);
        seManagerClass.setSpringSupport(springSupport);
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
}
