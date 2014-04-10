package org.dbtools.gen.android;

import org.dbtools.gen.DBObjectBuilder;
import org.dbtools.gen.DBObjectsBuilder;
import org.dbtools.schema.schemafile.DatabaseSchema;

/**
 * User: jcampbell
 * Date: 2/12/14
 */
public class AndroidObjectsBuilder extends DBObjectsBuilder {
    @Override
    public DBObjectBuilder getObjectBuilder() {
        return new AndroidDBObjectBuilder();
    }

    public static void buildAll(String schemaFilename, String baseOutputDir, String basePackageName, boolean injectionSupport, boolean dateTimeSupport) {
        buildAll(schemaFilename, baseOutputDir, basePackageName, injectionSupport, dateTimeSupport, false);
    }

    public static void buildAll(String schemaFilename, String baseOutputDir, String basePackageName, boolean injectionSupport, boolean dateTimeSupport, boolean encryptionSupport) {
        DBObjectsBuilder builder = new AndroidObjectsBuilder();
        builder.setXmlFilename(schemaFilename);
        builder.setOutputBaseDir(baseOutputDir);
        builder.setPackageBase(basePackageName);
        builder.setInjectionSupport(injectionSupport);
        builder.setDateTimeSupport(dateTimeSupport);
        builder.setEncryptionSupport(encryptionSupport);

        builder.build();
        System.out.println("Generated [" + builder.getObjectBuilder().getNumberFilesGenerated() + "] files.");
    }

    @Override
    public void onPostBuild(DatabaseSchema databaseSchema, String packageBase, String outputBaseDir, boolean injectionSupport, boolean encryptionSupport) {
        DatabaseBaseManagerRenderer databaseBaseManager = new DatabaseBaseManagerRenderer();
        databaseBaseManager.setPackageBase(packageBase);
        databaseBaseManager.setOutDir(outputBaseDir);
        databaseBaseManager.setEncryptionSupport(encryptionSupport);
        databaseBaseManager.generate(databaseSchema);

        DatabaseManagerRenderer databaseManager = new DatabaseManagerRenderer();
        databaseManager.setPackageBase(packageBase);
        databaseManager.setOutDir(outputBaseDir);
        databaseManager.setInjectionSupport(injectionSupport);
        databaseManager.generate(databaseSchema); // this file will only be created if it does not already exist
    }
}
