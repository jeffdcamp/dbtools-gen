package org.dbtools.gen.android;

import org.junit.Test;

public class AndroidDBTableObjectBuilderTest {
    @Test
    public void testBasicGen() {
        String userDir = System.getProperty("user.dir");

        String schemaFilename = userDir + "/src/main/resources/org/dbtools/xml/schema.xml";
        boolean injectionSupport = true; // support for CDI (Guice)
        boolean dateTimeSupport = true; // support for jsr DateTime (Joda Time)
        String baseOutputDir = userDir + "/target/test-src/src/main/java/org/mycompany/domain";
        String basePackageName = "org.mycompany.domain";
        AndroidObjectBuilder.buildAll(schemaFilename, baseOutputDir, basePackageName, injectionSupport, dateTimeSupport);
    }

}
