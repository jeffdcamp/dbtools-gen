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
import org.dbtools.gen.GenConfig;
import org.dbtools.renderer.SchemaRenderer;
import org.dbtools.schema.dbmappings.DatabaseMapping;
import org.dbtools.schema.schemafile.*;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Jeff
 */
public class AndroidDBObjectBuilder implements DBObjectBuilder {

    private AndroidBaseRecordRenderer baseRecordClass = new AndroidBaseRecordRenderer();
    private AndroidRecordRenderer recordClass = new AndroidRecordRenderer();
    private AndroidBaseManagerRenderer baseManagerClass = new AndroidBaseManagerRenderer();
    private AndroidManagerRenderer managerClass = new AndroidManagerRenderer();

    private int filesGeneratedCount = 0;
    private List<String> filesGenerated = new ArrayList<>();

    private PrintStream psLog;

    public AndroidDBObjectBuilder(GenConfig genConfig) {
        recordClass.setGenConfig(genConfig);
        baseRecordClass.setGenConfig(genConfig);
        managerClass.setGenConfig(genConfig);
        baseManagerClass.setGenConfig(genConfig);
    }

    @Override
    public String getName() {
        return "Android Object Builder";
    }

    @Override
    public boolean build(SchemaDatabase database, SchemaEntity entity, String packageName, String outDir, GenConfig genConfig) {
        if (psLog == null) {
            psLog = System.out;
        }

        if (entity != null) {
            psLog.println(entity.getType() + ": " + entity.getName());
        } else {
            psLog.println("ERROR: SchemaTable is null");
            return false;
        }

        char lastDirChar = outDir.charAt(outDir.length() - 1);
        if (lastDirChar != File.separatorChar) {
            outDir += File.separatorChar;
        }

        DatabaseMapping databaseMapping = SchemaRenderer.readXMLTypes(this.getClass(), SchemaRenderer.DEFAULT_TYPE_MAPPING_FILENAME, "sqlite");

        // Entities
        String baseRecordFileName = outDir + AndroidRecordRenderer.createClassName(entity) + ".java";
        String recordFileName = outDir + AndroidRecordRenderer.createClassName(entity) + ".java";
        File baseRecordFile = new File(baseRecordFileName);
        File recordFile = new File(recordFileName);

        // BaseRecord
        AndroidGeneratedEntityInfo generatedEntityInfo = baseRecordClass.generate(database, entity, packageName, databaseMapping);
        baseRecordClass.writeToFile(outDir);

        filesGenerated.add(baseRecordFile.getPath());
        filesGeneratedCount++;

        // Record
        if (!entity.isEnumerationTable()) {
            if (!recordFile.exists()) {
                recordClass.generate(entity, packageName);
                recordClass.writeToFile(outDir);

                filesGenerated.add(recordFile.getPath());
                filesGeneratedCount++;
            }
        }

        // Managers
        if (!entity.isEnumerationTable()) {
            String managerFileName = outDir + AndroidManagerRenderer.getClassName(entity) + ".java";
            File managerFile = new File(managerFileName);

            // Base Manager
            baseManagerClass.generate(entity, packageName, generatedEntityInfo);
            baseManagerClass.writeToFile(outDir);
            filesGeneratedCount++;

            // Manager
            if (!managerFile.exists()) {
                managerClass.generate(entity, packageName);
                managerClass.writeToFile(outDir);
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
    public void buildDatabaseManagersHolder(SchemaDatabase database, String packageBase, String packageName, List<SchemaTable> tables, List<SchemaView> views, List<SchemaQuery> queries, String outDir) {
        DatabaseManagersHolderRenderer renderer = new DatabaseManagersHolderRenderer();
        renderer.generate(database, packageBase, packageName, tables, views, queries, outDir);
    }
}
