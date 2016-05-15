package org.dbtools.gen;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

public class DBToolsInitTest {

    @Test
    public void testInitDBTools() throws Exception {
        String userDir = System.getProperty("user.dir");
        String databaseSchemaDir = userDir + "/target/test-src/src/main/database";

        DBToolsInit init = new DBToolsInit();
        init.initDBTools(databaseSchemaDir);

        assertTrue("schema.xml exists", new File(databaseSchemaDir, "schema.xml").exists());
        assertTrue("dbschema.xsd exists", new File(databaseSchemaDir, "dbschema.xsd").exists());
    }

}