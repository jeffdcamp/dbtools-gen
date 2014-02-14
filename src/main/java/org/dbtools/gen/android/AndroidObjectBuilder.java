package org.dbtools.gen.android;

import org.dbtools.gen.DBObjectBuilder;
import org.dbtools.gen.DBTableObjectBuilder;
import org.dbtools.gen.DBViewObjectBuilder;

/**
 * User: jcampbell
 * Date: 2/12/14
 */
public class AndroidObjectBuilder extends DBObjectBuilder {
    @Override
    public DBTableObjectBuilder getTableObjectBuilder() {
        return new AndroidDBTableObjectBuilder();
    }

    @Override
    public DBViewObjectBuilder getViewObjectBuilder() {
        return null;
    }

    public static void buildAll(String schemaFilename, String baseOutputDir, String basePackageName, boolean injectionSupport, boolean dateTimeSupport) {
        DBObjectBuilder builder = new AndroidObjectBuilder();
        builder.setXmlFilename(schemaFilename);
        builder.setOutputBaseDir(baseOutputDir);
        builder.setPackageBase(basePackageName);
        builder.setInjectionSupport(injectionSupport);
        builder.setDateTimeSupport(dateTimeSupport);

        builder.build();
        System.out.println("Generated [" + builder.getTableObjectBuilder().getNumberFilesGenerated() + "] files.");
    }

}
