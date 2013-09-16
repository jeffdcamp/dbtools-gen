/*
 * DatabaseSchema.java
 *
 * Created on August 24, 2003
 *
 * Copyright 2006 Jeff Campbell. All rights reserved. Unauthorized reproduction 
 * is a violation of applicable law. This material contains certain 
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.schema;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Jeff
 */
public class DatabaseSchema {

    private List<SchemaDatabase> schemaDatabases;

    /**
     * Creates a new instance of DatabaseSchema
     */
    public DatabaseSchema() {
    }

    /**
     * Reads in the XML Schema from specified file.
     * If schemaXMLFilenameIsAResource is set to true, then the schema is loaded
     * from the classpath as a resource
     */
    public boolean readXMLSchema(String schemaFilename, String dbVendorName, boolean schemaXMLFilenameIsAResource) {
        return readXMLSchema(schemaFilename, dbVendorName, schemaXMLFilenameIsAResource, null);
    }

    @SuppressWarnings("unchecked")
    public boolean readXMLSchema(String schemaFilename, String dbVendorName, boolean schemaXMLFilenameIsAResource, PrintStream logPrintStream) {
        if (logPrintStream == null) {
            logPrintStream = System.out;
        }

        try {
            // prepare the xml file for reading...
            SAXReader xmlReader = new SAXReader(false);  // use a DTD
            Document doc;

            // check to see if this is a file (from the filesystem) or
            // if it is a resource from the classpath
            if (schemaXMLFilenameIsAResource) {
                InputStream xmlSchemaInputStream = null;
                logPrintStream.print("Loading XML schema from classpath [" + schemaFilename + "]...");
                xmlSchemaInputStream = this.getClass().getResourceAsStream(schemaFilename);

                // check to see if we successfully loaded the schema from resource
                if (xmlSchemaInputStream == null) {
                    logPrintStream.println("[Failed] cannot find file resource in classpath!");
                    return false;
                }

                // Build Document
                doc = xmlReader.read(xmlSchemaInputStream);
            } else {
                // load the schema from local filesystem
                File schemaFile = new File(schemaFilename);
                doc = xmlReader.read(schemaFile);//inputFile, url);
            }

            Element root = doc.getRootElement();

            schemaDatabases = new ArrayList<SchemaDatabase>();
            Iterator dbItr = root.elementIterator("database");
            while (dbItr.hasNext()) {
                Element dbElement = (Element) dbItr.next();
                schemaDatabases.add(new SchemaDatabase(schemaFilename, dbVendorName, schemaXMLFilenameIsAResource, dbElement));
            }

        } catch (DocumentException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public List<SchemaDatabase> getSchemaDatabases() {
        return schemaDatabases;
    }

    public SchemaDatabase getDefaultDatabase() {
        if (schemaDatabases.size() > 0) {
            return schemaDatabases.get(0);
        }

        return null;
    }

    public SchemaDatabase getDatabase(String name) {
        for (SchemaDatabase schemaDatabase : schemaDatabases) {
            if (name.equals(schemaDatabase.getName())) {
                return schemaDatabase;
            }
        }

        return null;
    }

    public List<String> getTableNames() {
        List<String> names = new ArrayList<String>();
        for (SchemaDatabase schemaDatabase : schemaDatabases) {
            names.addAll(schemaDatabase.getTableNames());
        }

        return names;
    }
}
