package org.dbtools.plugin.extensions;


/**
 * Goal which creates JPA entities based on an DBTools schema.xml file
 *
 * @author <a href="mailto:jeff@soupbowl.net">Jeff Campbell</a>
 * @version $Id$
 *
 */
public abstract class BaseDBToolsExtension {

    /**
     * Name of the directory where the schema file is located.
     */
    private String schemaDir = "src/main/database";

    /**
     * Name of the schema file to do the generation from.
     */
    private String schemaXMLFilename = "schema.xml";

    public String getSchemaFullFilename() {
        if (schemaDir.endsWith("\\") || schemaDir.endsWith("/")) {
            return schemaDir + schemaXMLFilename;
        } else {
            return schemaDir + "/" + schemaXMLFilename;
        }
    }

    public String getSchemaDir() {
        return schemaDir;
    }

    public void schemaDir(String schemaDir) {
        this.schemaDir = schemaDir;
    }

    public String getSchemaXMLFilename() {
        return schemaXMLFilename;
    }

    public void schemaXMLFilename(String schemaXMLFilename) {
        this.schemaXMLFilename = schemaXMLFilename;
    }
}