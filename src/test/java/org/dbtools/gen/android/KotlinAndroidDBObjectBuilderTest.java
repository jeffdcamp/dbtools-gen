package org.dbtools.gen.android;

import org.dbtools.gen.DateType;
import org.dbtools.gen.GenConfig;
import org.dbtools.gen.android.kotlin.KotlinAndroidObjectsBuilder;
import org.junit.Test;

public class KotlinAndroidDBObjectBuilderTest {
    @Test
    public void testBasicGen() {
        String userDir = System.getProperty("user.dir");

        String schemaFilename = userDir + "/src/main/resources/org/dbtools/xml/schema.xml";
        boolean injectionSupport = true; // support for CDI (Dagger, Guice, etc)
        boolean dateTimeSupport = true; // support for jsr DateTime (Joda Time)
        String baseOutputDir = userDir + "/target/test-src/src/main/kotlin/org/mycompany/domain";
        String basePackageName = "org.mycompany.domain";

        GenConfig genConfig = new GenConfig();
        genConfig.setInjectionSupport(injectionSupport);
        genConfig.setDateType(DateType.JODA);

        KotlinAndroidObjectsBuilder objectsBuilder = new KotlinAndroidObjectsBuilder(genConfig);
        objectsBuilder.buildAll(schemaFilename, baseOutputDir, basePackageName);
    }
}
