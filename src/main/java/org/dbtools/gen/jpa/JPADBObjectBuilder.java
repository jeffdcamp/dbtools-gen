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
import org.dbtools.schema.schemafile.SchemaEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jeff
 */
public class JPADBObjectBuilder implements DBObjectBuilder {

    private JPABaseRecordManagerRenderer baseManagerClass = new JPABaseRecordManagerRenderer();
    private JPARecordManagerRenderer managerClass = new JPARecordManagerRenderer();
    private JPABaseRecordClassRenderer baseRecordClass = new JPABaseRecordClassRenderer();
    private JPARecordClassRenderer recordClass = new JPARecordClassRenderer();
    private int filesGeneratedCount = 0;
    private List<String> filesGenerated = new ArrayList<>();

    private SchemaDatabase database;
    private SchemaEntity table;
    private String packageName;
    private String outDir;

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
            String managerFileName = outDir + JPARecordManagerRenderer.getClassName(table) + ".java";

            //File baseManagerFile = new File(baseSEManagerFileName);
            File managerFile = new File(managerFileName);

            // Base Manager
            baseManagerClass.generateObjectCode(table, packageName);
            baseManagerClass.writeToFile(outDir);

            filesGeneratedCount++;

            // Manager
            if (!managerFile.exists()) {
                managerClass.generateObjectCode(table, packageName);
                managerClass.writeToFile(outDir);

                filesGeneratedCount++;
            }
        }

        // Entities
        String baseRecordFileName = outDir + JPABaseRecordClassRenderer.createClassName(table) + ".java";
        String recordFileName = outDir + JPARecordClassRenderer.createClassName(table) + ".java";
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
        return filesGenerated;
    }

    @Override
    public void setDateTimeSupport(boolean b) {
        baseRecordClass.setDateTimeSupport(b);
    }

    @Override
    public void setInjectionSupport(boolean b) {
        baseRecordClass.setInjectionSupport(b);
        managerClass.setInjectionSupport(b);
        baseManagerClass.setInjectionSupport(b);
    }

    @Override
    public void setJsr305Support(boolean b) {
    }

    @Override
    public void setEncryptionSupport(boolean b) {
        // do nothing
    }

    public void setJavaeeSupport(boolean javaeeSupport) {
        baseManagerClass.setJavaeeSupport(javaeeSupport);
        managerClass.setJavaeeSupport(javaeeSupport);
    }

    @Override
    public void setEntity(SchemaEntity table) {
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
}
