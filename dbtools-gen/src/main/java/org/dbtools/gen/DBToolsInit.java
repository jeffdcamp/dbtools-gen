package org.dbtools.gen;

public class DBToolsInit {
    public void initDBTools(String schemaDir) {
        DBToolsFiles.copyXsdFileToSchemaDir(schemaDir);
        DBToolsFiles.copySampleSchemaFileToSchemaDir(schemaDir);
    }
}
