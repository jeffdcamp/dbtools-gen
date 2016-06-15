package org.dbtools.renderer;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

public class SchemaRendererTest {

    private String userDir = System.getProperty("user.dir");
    private String schemaFilename = userDir + "/src/test/resources/org/dbtools/xml/schema.xml";
    private String outputDir = userDir + "/target/test";
    private String outputFilenameTemplate = "schema-%s.sql";

    @Test
    public void executeRendererDerby() {
        assertTrue(testGenSql(DerbyRenderer.RENDERER_NAME));
    }

    @Test
    public void executeRendererFireBird() {
        assertTrue(testGenSql(FireBirdRenderer.RENDERER_NAME));
    }

    @Test
    public void executeRendererHSQLDBBird() {
        assertTrue(testGenSql(HSQLDBRenderer.RENDERER_NAME));
    }

    @Test
    public void executeRendererIAnywhere() {
        assertTrue(testGenSql(IAnywhereRenderer.RENDERER_NAME));
    }

    @Test
    public void executeRendererMySQL() {
        assertTrue(testGenSql(MySQLRenderer.RENDERER_NAME));
    }

    @Test
    public void executeRendererOracle() {
        assertTrue(testGenSql(Oracle9Renderer.RENDERER_NAME));
    }

    @Test
    public void executeRendererPostgreSQL() {
        assertTrue(testGenSql(PostgreSQLRenderer.RENDERER_NAME));
    }

    @Test
    public void executeRendererSqlite() {
        assertTrue(testGenSql(SqliteRenderer.RENDERER_NAME));
    }

    private boolean testGenSql(String dbVendor) {
        String outputFilename = String.format(outputFilenameTemplate, dbVendor);
        SchemaRenderer sr = SchemaRenderer.getRenderer(dbVendor);
        sr.setShowConsoleProgress(false);
        sr.setDbVendorName(dbVendor);
        sr.setSchemaXMLFilename(schemaFilename, false);
        sr.setOutputFile(outputDir + File.separator + outputFilename);
        sr.setExecuteSQLScriptFiles(false);
        sr.setCreateSchema(true);
        sr.setCreatePostSchema(true);
        sr.setTablesToGenerate(null); // if null... all tables
        sr.setViewsToGenerate(null); // if null... all views
        sr.setDropTables(false);

        return sr.executeRenderer();
    }
}