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


import org.dbtools.gen.DBTableObjectBuilder;
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
public class AndroidDBTableObjectBuilder implements DBTableObjectBuilder {

    private AndroidBaseRecordClassRenderer baseRecordClass = new AndroidBaseRecordClassRenderer();
    private AndroidRecordClassRenderer recordClass = new AndroidRecordClassRenderer();
    private AndroidBaseRecordManager baseManagerClass = new AndroidBaseRecordManager();
    private AndroidRecordManager managerClass = new AndroidRecordManager();

    private int filesGeneratedCount = 0;
    private List<String> filesGenerated = new ArrayList<>();

    private SchemaDatabase database;
    private SchemaTable table;
    private String packageName;
    private String outDir;
    private PrintStream psLog;

    @Override
    public String getName() {
        return "Android Object Builder";
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
        if (lastDirChar != File.separatorChar) {
            outDir += File.separatorChar;
        }

        // Managers
        if (!table.isEnumerationTable()) {
            String managerFileName = outDir + AndroidRecordManager.getClassName(table) + ".java";
            File managerFile = new File(managerFileName);

            // Base Manager
            baseManagerClass.generate(table, packageName);
            baseManagerClass.writeToFile(outDir);

            filesGeneratedCount++;

            // Manager
            if (!managerFile.exists()) {
                managerClass.generate(table, packageName);
                managerClass.writeToFile(outDir);

                filesGeneratedCount++;
            }
        }

        // Entities
        String baseRecordFileName = outDir + AndroidRecordClassRenderer.createClassName(table) + ".java";
        String recordFileName = outDir + AndroidRecordClassRenderer.createClassName(table) + ".java";
        File baseRecordFile = new File(baseRecordFileName);
        File recordFile = new File(recordFileName);


        // BaseRecord
        baseRecordClass.generate(database, table, packageName);
        baseRecordClass.writeToFile(outDir);

        filesGenerated.add(baseRecordFile.getPath());
        filesGeneratedCount++;

        // Record
        if (!table.isEnumerationTable()) {
            if (!recordFile.exists()) {
                recordClass.generate(table, packageName);
                recordClass.writeToFile(outDir);

                filesGenerated.add(recordFile.getPath());
                filesGeneratedCount++;
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
    public void setDateTimeSupport(boolean b) {
        baseRecordClass.setDateTimeSupport(b);
    }

    @Override
    public void setInjectionSupport(boolean b) {
        baseManagerClass.setInjectionSupport(b);
        baseRecordClass.setInjectionSupport(b);
        managerClass.setInjectionSupport(b);
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
    public void setDatabase(SchemaDatabase dbSchema) {
        this.database = dbSchema;
    }

    @Override
    public void setSpringSupport(boolean b) {
    }
}
