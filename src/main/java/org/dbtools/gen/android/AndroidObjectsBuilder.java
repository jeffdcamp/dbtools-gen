package org.dbtools.gen.android;

import org.dbtools.gen.DBObjectBuilder;
import org.dbtools.gen.DBObjectsBuilder;
import org.dbtools.gen.GenConfig;
import org.dbtools.schema.schemafile.DatabaseSchema;

/**
 * User: jcampbell
 * Date: 2/12/14
 */
public class AndroidObjectsBuilder extends DBObjectsBuilder {
    private AndroidDBObjectBuilder objectBuilder;

    public AndroidObjectsBuilder(GenConfig genConfig) {
        super(genConfig);
        objectBuilder = new AndroidDBObjectBuilder(genConfig);
    }

    @Override
    public DBObjectBuilder getObjectBuilder() {
        return objectBuilder;
    }

    public void buildAll(String schemaFilename, String baseOutputDir, String basePackageName) {
        setXmlFilename(schemaFilename);
        setOutputBaseDir(baseOutputDir);
        setPackageBase(basePackageName);

        build();
        System.out.println("Generated [" + objectBuilder.getNumberFilesGenerated() + "] files.");
    }

    @Override
    public void onPostBuild(DatabaseSchema databaseSchema, String packageBase, String outputBaseDir, GenConfig genConfig) {
        DatabaseBaseManagerRenderer databaseBaseManager = new DatabaseBaseManagerRenderer();
        databaseBaseManager.setPackageBase(packageBase);
        databaseBaseManager.setOutDir(outputBaseDir);
        databaseBaseManager.setGenConfig(genConfig);
        databaseBaseManager.generate(databaseSchema);

        DatabaseManagerRenderer databaseManager = new DatabaseManagerRenderer();
        databaseManager.setPackageBase(packageBase);
        databaseManager.setOutDir(outputBaseDir);
        databaseManager.setGenConfig(genConfig);
        databaseManager.generate(databaseSchema); // this file will only be created if it does not already exist
    }
}
