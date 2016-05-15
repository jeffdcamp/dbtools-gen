package org.dbtools.gen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class DBToolsFiles {

    public static void copyXsdFileToSchemaDir(String schemaDirname) {
        saveFile("/org/dbtools/xml/", "dbschema.xsd", schemaDirname);
    }

    public static void copySampleSchemaFileToSchemaDir(String schemaDirname) {
        File outFile = new File(schemaDirname, "schema.xml");

        // skip if already exists
        if (outFile.exists()) {
            System.out.println(outFile.getAbsoluteFile() + " already exists... skipping");
            return;
        }

        saveFile("/org/dbtools/xml/", "schema.xml", schemaDirname);
    }

    public static void saveFile(String sourceClasspathDir, String filename, String schemaDirname) {
        try {
            File schemaDir = new File(schemaDirname);
            schemaDir.mkdirs();

            InputStream in = DBToolsFiles.class.getResourceAsStream(sourceClasspathDir + filename);

            File outFile = new File(schemaDirname, filename);

            FileOutputStream fos = new FileOutputStream(outFile);
            int read;
            byte[] bytes = new byte[1024];

            while ((read = in.read(bytes)) != -1) {
                fos.write(bytes, 0, read);
            }
        } catch (Exception e) {
            System.out.println("Failed to write: " + filename + " Error: [" + e.getMessage() + "]");
        }
    }



}
