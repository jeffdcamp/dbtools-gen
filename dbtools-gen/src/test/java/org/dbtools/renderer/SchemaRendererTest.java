package org.dbtools.renderer;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

public class SchemaRendererTest {

    @Test
    public void executeRenderer() {

        String userDir = System.getProperty("user.dir");
        String schemaFilename = userDir + "/src/test/resources/org/dbtools/xml/schema.xml";
        String outputDir = userDir + "/target/test";
        String outputFilename = "schema.sql";

        String dbVendor = "sqlite";
        SchemaRenderer sr = SchemaRenderer.getRenderer(dbVendor);
        sr.setShowConsoleProgress(false);
        sr.setDbVendorName(dbVendor);
        sr.setSchemaXMLFilename(schemaFilename, false);
        sr.setOutputFile(outputDir + File.separator + outputFilename);
        sr.setExecuteSQLScriptFiles(!true);
        sr.setCreateSchema(true);
        sr.setCreatePostSchema(true);
        sr.setTablesToGenerate(null); // if null... all tables
        sr.setViewsToGenerate(null); // if null... all views
        sr.setDropTables(false);

        boolean success = sr.executeRenderer();
        assertTrue(success);
    }
}