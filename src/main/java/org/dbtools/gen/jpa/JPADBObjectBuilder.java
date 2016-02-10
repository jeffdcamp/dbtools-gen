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
import org.dbtools.gen.GenConfig;
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
    private JPABaseRecordRenderer baseRecordClass = new JPABaseRecordRenderer();
    private JPARecordClassRenderer recordClass = new JPARecordClassRenderer();
    private int filesGeneratedCount = 0;
    private List<String> filesGenerated = new ArrayList<>();

    public JPADBObjectBuilder(GenConfig genConfig) {
        baseRecordClass.setGenConfig(genConfig);
        managerClass.setGenConfig(genConfig);
        baseManagerClass.setGenConfig(genConfig);
    }

    @Override
    public String getName() {
        return "JPA Object Builder";
    }

    @Override
    public boolean build(SchemaDatabase database, SchemaEntity entity, String packageName, String outDir, GenConfig genConfig) {
        char lastDirChar = outDir.charAt(outDir.length() - 1);
        if (lastDirChar != '\\' || lastDirChar != '/') {
            if (outDir.charAt(0) == '/') {
                outDir += "/";
            } else {
                outDir += "\\";
            }
        }

        // Managers
        if (!entity.isEnumerationTable()) {
            String managerFileName = outDir + JPARecordManagerRenderer.getClassName(entity) + ".java";

            //File baseManagerFile = new File(baseSEManagerFileName);
            File managerFile = new File(managerFileName);

            // Base Manager
            baseManagerClass.generateObjectCode(entity, packageName);
            baseManagerClass.writeToFile(outDir);

            filesGeneratedCount++;

            // Manager
            if (!managerFile.exists()) {
                managerClass.generateObjectCode(entity, packageName);
                managerClass.writeToFile(outDir);

                filesGeneratedCount++;
            }
        }

        // Entities
        String baseRecordFileName = outDir + JPABaseRecordRenderer.createClassName(entity) + ".java";
        String recordFileName = outDir + JPARecordClassRenderer.createClassName(entity) + ".java";
        File baseRecordFile = new File(baseRecordFileName);
        File recordFile = new File(recordFileName);


        // BaseRecord
        baseRecordClass.generate(database, entity, packageName);
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
}
