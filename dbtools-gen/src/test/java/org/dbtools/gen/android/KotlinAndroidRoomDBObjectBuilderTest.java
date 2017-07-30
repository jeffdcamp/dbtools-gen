package org.dbtools.gen.android;

import org.dbtools.gen.DateType;
import org.dbtools.gen.GenConfig;
import org.dbtools.gen.android.kotlinroom.KotlinAndroidRoomObjectsBuilder;
import org.junit.Test;

public class KotlinAndroidRoomDBObjectBuilderTest {
    @Test
    public void testBasicGen() {
        String userDir = System.getProperty("user.dir");

        String schemaFilename = userDir + "/src/test/resources/org/dbtools/xml/schema.xml";
        boolean injectionSupport = true; // support for CDI (Dagger, Guice, etc)
        boolean dateTimeSupport = true; // support for jsr DateTime (Joda Time)
        String baseOutputDir = userDir + "/build/test-src/src/main/kotlin/org/mycompany/domain";
        String basePackageName = "org.mycompany.domain";

        GenConfig genConfig = new GenConfig();
        genConfig.setInjectionSupport(injectionSupport);
        genConfig.setDateType(DateType.JSR_310);

        KotlinAndroidRoomObjectsBuilder objectsBuilder = new KotlinAndroidRoomObjectsBuilder(genConfig);
        objectsBuilder.buildAll(schemaFilename, baseOutputDir, basePackageName);
    }
}
