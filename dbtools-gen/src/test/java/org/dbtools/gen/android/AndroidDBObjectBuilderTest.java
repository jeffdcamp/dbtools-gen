package org.dbtools.gen.android;

import org.dbtools.gen.DateType;
import org.dbtools.gen.GenConfig;
import org.junit.Test;

public class AndroidDBObjectBuilderTest {
    @Test
    public void testBasicGen() {
        String userDir = System.getProperty("user.dir");

        String schemaFilename = userDir + "/src/test/resources/org/dbtools/xml/schema.xml";
        boolean injectionSupport = true; // support for CDI (Dagger, Guice, etc)
        String baseOutputDir = userDir + "/build/test-src/src/main/java/org/mycompany/domain";
        String basePackageName = "org.mycompany.domain";

        GenConfig genConfig = new GenConfig();
        genConfig.setInjectionSupport(injectionSupport);
        genConfig.setDateType(DateType.JSR_310);

        AndroidObjectsBuilder objectsBuilder = new AndroidObjectsBuilder(genConfig);
        objectsBuilder.buildAll(schemaFilename, baseOutputDir, basePackageName);
    }
}
