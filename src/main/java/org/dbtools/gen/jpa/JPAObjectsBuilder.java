package org.dbtools.gen.jpa;

import org.dbtools.gen.DBObjectsBuilder;
import org.dbtools.gen.DBObjectBuilder;

/**
 * User: jcampbell
 * Date: 2/12/14
 */
public class JPAObjectsBuilder extends DBObjectsBuilder {
    @Override
    public DBObjectBuilder getObjectBuilder() {
        return new JPADBObjectBuilder();
    }

    public static void buildAll(String schemaFilename, String baseOutputDir, String basePackageName, boolean injectionSupport, boolean dateTimeSupport) {
        DBObjectsBuilder builder = new JPAObjectsBuilder();
        builder.setXmlFilename(schemaFilename);
        builder.setOutputBaseDir(baseOutputDir);
        builder.setPackageBase(basePackageName);
        builder.setInjectionSupport(injectionSupport);
        builder.setDateTimeSupport(dateTimeSupport);

        builder.build();
        System.out.println("Generated [" + builder.getObjectBuilder().getNumberFilesGenerated() + "] files.");
    }

}
