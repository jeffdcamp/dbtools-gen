package org.dbtools.gen.android;

import org.dbtools.gen.DBObjectBuilder;
import org.dbtools.gen.DBObjectsBuilder;

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
        DBObjectsBuilder builder = new AndroidObjectsBuilder();
        builder.setXmlFilename(schemaFilename);
        builder.setOutputBaseDir(baseOutputDir);
        builder.setPackageBase(basePackageName);
        builder.setInjectionSupport(injectionSupport);
        builder.setDateTimeSupport(dateTimeSupport);

        builder.build();
        System.out.println("Generated [" + builder.getObjectBuilder().getNumberFilesGenerated() + "] files.");
    }

    // todo remove
    public static void main(String[] args) {
        String projectDir = "/home/jcampbell/src/ldschurch/android/ldstools/LDSToolsAndroid/";
        String schemaFilename = projectDir + "src/main/database/schema.xml";
        boolean injectionSupport = true; // support for CDI (Guice)
        boolean dateTimeSupport = true; // support for jsr DateTime (Joda Time)
        String baseOutputDir = projectDir + "src/main/java/org/lds/ldstools/domain";
        String basePackageName = "org.lds.ldstools.domain";
        AndroidObjectsBuilder.buildAll(schemaFilename, baseOutputDir, basePackageName, injectionSupport, dateTimeSupport);
    }

}
