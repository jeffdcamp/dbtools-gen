package org.dbtools.gen.android;

import org.dbtools.gen.DateType;
import org.dbtools.gen.GenConfig;
import org.junit.Test;

public class AndroidDBObjectBuilderTest {
    @Test
    public void testBasicGen() {
        String userDir = System.getProperty("user.dir");

        String schemaFilename = userDir + "/src/main/resources/org/dbtools/xml/schema.xml";
        boolean injectionSupport = true; // support for CDI (Dagger, Guice, etc)
        boolean dateTimeSupport = true; // support for jsr DateTime (Joda Time)
        String baseOutputDir = userDir + "/target/test-src/src/main/java/org/mycompany/domain";
        String basePackageName = "org.mycompany.domain";

        GenConfig genConfig = new GenConfig();
        genConfig.setInjectionSupport(injectionSupport);
        genConfig.setDateType(DateType.JODA);

        AndroidObjectsBuilder objectsBuilder = new AndroidObjectsBuilder(genConfig);
        objectsBuilder.buildAll(schemaFilename, baseOutputDir, basePackageName, genConfig);
    }
}
