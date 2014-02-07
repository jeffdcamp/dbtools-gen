/*
 * SchemaRenderer.java
 *
 * Created on February 23, 2002
 *
 * Copyright 2006 Jeff Campbell. All rights reserved. Unauthorized reproduction 
 * is a violation of applicable law. This material contains certain 
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.renderer;

import org.dbtools.schema.SQLStatement;
import org.dbtools.schema.dbmappings.DatabaseMapping;
import org.dbtools.schema.dbmappings.DatabaseMappings;
import org.dbtools.schema.schemafile.*;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * @author Jeff Campbell
 */
public class SchemaRenderer implements Runnable {

    public static final String DEFAULT_TYPE_MAPPING_FILENAME = "dbmappings.xml";
    public static final String DEFAULT_TYPE_MAPPING_FILE = "/org/dbtools/xml/" + DEFAULT_TYPE_MAPPING_FILENAME;
    private DatabaseSchema dbSchema;
    private String otherInsertsFilename = "";
    private DatabaseMapping databaseMapping;
    private PrintStream ps = null;
    private static Map<String, String> dbRenderers = new TreeMap<>();
    private Component parentComponent; // for progress (if needed)
    private String dbVendorName;
    private String databaseName;
    private String schemaXMLFilename;
    private boolean schemaXMLFilenameIsAResource = false; // for use when XML Schema is integrated into jar file
    private String outputFile;
    private String[] tablesToGenerate;
    private boolean createSchema = true; // includes tables & views
    private String[] viewsToGenerate;
    private boolean dropTables = false;
    private boolean executeSQLScriptFiles;
    private boolean createONLYOtherInserts = false;
    private boolean createPostSchema = true;
    private boolean createEnumInserts = true;
    private Map<String, SchemaTable> alreadyCreatedEnum = new HashMap<>();
    private boolean showConsoleProgress = false;
    private ProgressMonitor pm = null;
    private int currProgress = 0;
    private int maxProgress = 0;
    private String mappingFilename = DEFAULT_TYPE_MAPPING_FILENAME;

    static {
        dbRenderers.put(DerbyRenderer.RENDERER_NAME, "com.jdc.db.schema.DerbyRenderer");
        dbRenderers.put(SqliteRenderer.RENDERER_NAME, "com.jdc.db.schema.SqliteRenderer");
        dbRenderers.put(FireBirdRenderer.RENDERER_NAME, "com.jdc.db.schema.FireBirdRenderer");
        dbRenderers.put(HSQLDBRenderer.RENDERER_NAME, "com.jdc.db.schema.HSQLDBRenderer");
        dbRenderers.put(IAnywhereRenderer.RENDERER_NAME, "com.jdc.db.schema.IAnywhereRenderer");
        dbRenderers.put(MySQLRenderer.RENDERER_NAME, "com.jdc.db.schema.MySQLRenderer");
        dbRenderers.put(PostgreSQLRenderer.RENDERER_NAME, "com.jdc.db.schema.PostgreSQLRenderer");
        dbRenderers.put(Oracle9Renderer.RENDERER_NAME, "com.jdc.db.schema.Oracle9Renderer");
    }

    /**
     * Creates a new instance of SchemaRenderer
     */
    public SchemaRenderer() {
        setDefaults();
    }

    /**
     * Creates a new instance of SchemaRenderer
     */
    public SchemaRenderer(PrintStream ps) {
        this.ps = ps;
        setDefaults();
    }

    /**
     * Creates a new instance of SchemaRenderer
     */
    public SchemaRenderer(Component parentComponent, String dbVendorName, String databaseName, String schemaXMLFilename, String outputFile, String[] tablesToGenerate, boolean dropTables, boolean createInserts, PrintStream ps) {
        init(parentComponent, dbVendorName, schemaXMLFilename, outputFile, tablesToGenerate, dropTables, createInserts, ps);
    }

    public void readXMLSchema(String path) {
        dbSchema = DatabaseSchema.readXMLSchema(path);
    }

    private void setDefaults() {
        if (ps == null) {
            ps = System.out;
        }
    }

    // remember to modify executeRenderer(...) if the parameters change
    public void init(Component parentComponent, String dbVendorName, String schemaXMLFilename, String outputFile, String[] tablesToGenerate, boolean dropTables, boolean executeSQLScriptFiles, PrintStream ps) {
        this.parentComponent = parentComponent;
        this.dbVendorName = dbVendorName;
        this.schemaXMLFilename = schemaXMLFilename;
        this.outputFile = outputFile;
        this.tablesToGenerate = tablesToGenerate;
        this.dropTables = dropTables;
        this.executeSQLScriptFiles = executeSQLScriptFiles;
        this.ps = ps;
        setDefaults();
    }

    private void addProgress(int value) {
        currProgress += value;
        updateProgress();
    }

    private void updateProgress() {
        if (pm != null) {
            pm.setProgress(currProgress);
        }
    }

    private void calculateProgress(SchemaDatabase database) {
        // determine size of inserts
        if (executeSQLScriptFiles && database.getPostSQLScriptFiles() != null && !createONLYOtherInserts) {
            for (PostSQLScriptFile postSqlScriptFile : database.getPostSQLScriptFiles()) {
                try {
                    File insertsFile = new File(postSqlScriptFile.getPreparedFilepath());
                    maxProgress += (int) insertsFile.length();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        if (executeSQLScriptFiles && otherInsertsFilename.length() > 0) {
            File insertsFile = new File(otherInsertsFilename);
            maxProgress += (int) insertsFile.length();
        }
    }

    public static synchronized DatabaseMapping readXMLTypes(Class<?> classLoaderClass, String typesFilename, String dbVendorName) {
        File xmlMappingFile = new File(typesFilename);
        if (!xmlMappingFile.exists()) {
            xmlMappingFile = new File("xml/" + typesFilename);
            if (!xmlMappingFile.exists()) {
                xmlMappingFile = new File("../xml/" + typesFilename);
                if (!xmlMappingFile.exists()) {
                    xmlMappingFile = null;
                }
            }
        }

        // may need to try to load from the jar
        InputStream xmlMappingInputStream = null;
        if (xmlMappingFile == null) {
//            System.out.println("Failed to find mapping file: [" + typesFilename + "]... loading default one from classpath...");
            xmlMappingInputStream = classLoaderClass.getResourceAsStream(DEFAULT_TYPE_MAPPING_FILE);
        }

        if (xmlMappingFile == null && xmlMappingInputStream == null) {
            throw new IllegalStateException("Failed to find mapping file: [" + typesFilename + "] in classpath.");
            //return false;
        }

        try {
            Serializer serializer = new Persister();
            DatabaseMappings mappings;
            if (xmlMappingFile != null) {
                mappings = serializer.read(DatabaseMappings.class, xmlMappingFile);
            } else {
                mappings = serializer.read(DatabaseMappings.class, xmlMappingInputStream);
            }

            for (DatabaseMapping databaseMapping : mappings.getDatabaseMappings()) {
                if (databaseMapping.getDatabaseName().equalsIgnoreCase(dbVendorName)) {
                    return databaseMapping;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * represents a generic way of writing a schema (no made for any specific database
     */
    private boolean writeSchema(SchemaDatabase database, String pathname, String schema, boolean addInserts) {
        showProgress("Writing file [" + pathname + "]...", true);

        File outFile = new File(pathname);

        try {
            PrintStream fps = new PrintStream(new FileOutputStream(outFile));

            fps.print(schema);

            // concatenate inserts
            if (addInserts) {
                // Add all inserts as specified in the xml file
                if (database.getPostSQLScriptFiles() != null && !createONLYOtherInserts) {
                    for (PostSQLScriptFile postSQLScriptFile : database.getPostSQLScriptFiles()) {
                        try {
                            addInsertsToSQL(fps, postSQLScriptFile.getPreparedFilepath());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }

                // Add other inserts (if any)
                if (otherInsertsFilename.length() > 0) {
                    addInsertsToSQL(fps, otherInsertsFilename);
                }
            }

            if (createPostSchema) {
                fps.print(generatePostSchema(database, tablesToGenerate));
            }

            fps.close();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private void addInsertsToSQL(PrintStream fps, String insertsFilename) throws IOException {
        BufferedReader insertsReader;
        // determine how to read inserts file
        if (schemaXMLFilenameIsAResource) {
            InputStream insertsInputStream = this.getClass().getResourceAsStream(insertsFilename);

            if (insertsInputStream == null) {
                showProgress("Failed to read 'inserts' resource [" + insertsFilename + "]", true);
            }

            insertsReader = new BufferedReader(new InputStreamReader(insertsInputStream));
        } else {
            insertsReader = new BufferedReader(new FileReader(insertsFilename));
        }

        // read though all lines...
        while (insertsReader.ready()) {
            String line = insertsReader.readLine();
            addProgress(line.length());

            boolean skip = false;
            if (line.startsWith("//")) {
                skip = true;
            }

            // avoid having duplicate inserts for enum tables
            if (createEnumInserts) {
                SQLStatement sqlStmnt = new SQLStatement(line);
                if (sqlStmnt.isInsert() && alreadyCreatedEnum.containsKey(sqlStmnt.getTable())) {
                    skip = true;
                }
            }

            if (!skip) {
                fps.println(line);
            }
        }
    }

    public boolean executeRenderer() {
        showProgress("Reading Database Mappings...", true);
        databaseMapping = readXMLTypes(this.getClass(), mappingFilename, dbVendorName);

        showProgress("Reading XML Schema...", true);
        dbSchema.readXMLSchema(schemaXMLFilename);

        if (databaseName != null && databaseName.isEmpty()) {
            return renderDatabase(dbSchema.getDatabase(databaseName));
        } else {
            for (SchemaDatabase database : dbSchema.getDatabases()) {
                if (!renderDatabase(database)) {
                    return false;
                }
            }
        }

        return true;
    }

    public String getSqlType(SchemaFieldType type) {
        return databaseMapping.getSqlType(type);
    }

    public DatabaseMapping getDatabaseMapping() {
        return databaseMapping;
    }

    private boolean renderDatabase(SchemaDatabase database) {
        calculateProgress(database);
        pm = null;
        if (parentComponent != null && maxProgress > 0) {
            showConsoleProgress = false;
            pm = new ProgressMonitor(parentComponent, "Creating Schema", "", 0, maxProgress);
            pm.setMillisToPopup(0);
        } else {
            showConsoleProgress = true;
        }

        String newSchema = "";
        if (createSchema) {
            newSchema = generateSchema(database, tablesToGenerate, viewsToGenerate, dropTables, executeSQLScriptFiles);
        }

        boolean success = writeSchema(database, outputFile, newSchema, executeSQLScriptFiles);
        showProgress("Schema Rendering Completed.", true);

        if (pm != null) {
            pm.close();
        }

        return success;
    }

    public String generateSchema(SchemaDatabase database, String[] tablesToGenerate, String[] viewsToGenerate, boolean dropTables, boolean createInserts) {
        showProgress("Generating SQL schema using default renderer ...", false);
        StringBuilder schema = new StringBuilder();

        List<SchemaTable> requestedTables = getTablesToGenerate(database, tablesToGenerate);
        List<SchemaView> requestedViews = getViewsToGenerate(database, viewsToGenerate);

        // drop schema
        if (dropTables) {
            generateDropSchema(false, true, schema, requestedTables, requestedViews);
        }

        // create tables
        for (SchemaTable table : requestedTables) {
            // add table header
            schema.append("CREATE TABLE ");
            schema.append(table.getName());
            schema.append(" (\n");

            // add fields
            List<SchemaField> fields = table.getFields();

            SchemaField enumPKField = null;
            SchemaField enumValueField = null;

            for (int j = 0; j < fields.size(); j++) {
                SchemaField field = fields.get(j);

                // add field
                schema.append("\t");
                schema.append(field.getName());
                schema.append(" ");
                schema.append(getSqlType(field.getJdbcDataType()));

                //check for size
                if (field.getSize() > 0) {
                    schema.append("(");
                    schema.append(field.getSize());
                    schema.append(")");
                }


                schema.append("");

                // if this is the last one, then don't put a ','
                if (j == fields.size() - 1) {
                    schema.append("\n");
                } else {
                    schema.append(",\n");
                }

                // check for enumFields
                if (enumPKField == null && field.isPrimaryKey()) {
                    enumPKField = field;
                }
                if (enumValueField == null && field.getJdbcDataType() == SchemaFieldType.VARCHAR) {
                    enumValueField = field;
                }
            }

            // add table footer
            schema.append(");\n\n");

            generateEnumSchema(schema, table, alreadyCreatedEnum, enumPKField, enumValueField, createEnumInserts);
        }

        //ps.println("\n\nSchema: \n\n" + schema.toString());
        showProgress("complete", true);
        return schema.toString();
    }

    public static boolean enumInsertsWillBeCreated(SchemaTable table, SchemaField enumPKField, SchemaField enumValueField, boolean createInserts) {
        return table.isEnumerationTable() && createInserts && enumPKField != null && enumValueField != null;
    }

    public static int generateEnumSchema(StringBuilder schema, SchemaTable table, Map<String, SchemaTable> alreadyCreatedEnum,
                                         SchemaField enumPKField, SchemaField enumValueField, boolean createInserts) {
        return generateEnumSchema(schema, table, alreadyCreatedEnum, enumPKField, enumValueField, createInserts, 0);
    }

    public static int generateEnumSchema(StringBuilder schema, SchemaTable table, Map<String, SchemaTable> alreadyCreatedEnum,
                                         SchemaField enumPKField, SchemaField enumValueField, boolean createInserts, int ordinalStartValue) {
        int enumPKID = ordinalStartValue;

        // if table is enum, add enum inserts
        if (enumInsertsWillBeCreated(table, enumPKField, enumValueField, createInserts)) {
            // store which inserts happened for later
            alreadyCreatedEnum.put(table.getName().toLowerCase(), table);

            schema.append("\n");
            for (TableEnum enumItem : table.getTableEnums()) {
                schema.append("INSERT INTO ").append(table.getName());
                schema.append(" (").append(enumPKField.getName()).append(", ").append(enumValueField.getName()).append(")");
                schema.append(" VALUES (").append(enumPKID).append(", \'").append(enumItem.getValue()).append("\');\n");
                enumPKID++;
            }
            schema.append("\n");
        }

        return enumPKID;
    }

    public Map<String, SchemaTable> getAlreadyCreatedEnum() {
        return alreadyCreatedEnum;
    }

    public static String generateDropSchema(boolean addIfExists, SchemaTable table) {
        if (addIfExists) {
            return "DROP TABLE IF EXISTS " + table.getName() + ";";
        } else {
            return "DROP TABLE " + table.getName() + ";";
        }
    }

    public void generateDropSchema(boolean addIfExists, boolean ifExistsAtEndOfStmnt, StringBuilder schema,
                                   List<SchemaTable> tablesToGenerate, List<SchemaView> viewsToGenerate) {
        // reverse order
        List<String> inverseViews = new ArrayList<>();
        List<String> inverseTables = new ArrayList<>();

        String ifExists = "";
        if (addIfExists) {
            ifExists = " IF EXISTS ";
        }

        for (SchemaView view : viewsToGenerate) {
            inverseViews.add(0, view.getName());
        }

        for (SchemaTable table : tablesToGenerate) {
            inverseTables.add(0, table.getName());
        }

        // create drop schema
        for (String view : inverseViews) {
            if (ifExistsAtEndOfStmnt) {
                schema.append("DROP VIEW ").append(view).append(ifExists).append(";\n");
            } else {
                schema.append("DROP VIEW ").append(ifExists).append(view).append(";\n");
            }
        }
        schema.append("\n");

        for (String table : inverseTables) {
            if (ifExistsAtEndOfStmnt) {
                schema.append("DROP TABLE ").append(table).append(ifExists).append(";\n");
            } else {
                schema.append("DROP TABLE ").append(ifExists).append(table).append(";\n");
            }
        }
        schema.append("\n");
    }

    /**
     * If your database has sequences, or anything else that needs to
     * occur after inserts happen.
     */
    public String generatePostSchema(SchemaDatabase database, String[] tablesToGenerate) {
        showProgress("Generating Post SQL schema using default renderer...", false);
        showProgress("complete", true);
        return "";
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        //SchemaRenderer sr = new SchemaRenderer();
        //System.out.println("test= " + sr.dropTables);
        String line = "    INSERT INTO    Customer";
        StringTokenizer st = new StringTokenizer(line, " ", false);
        while (st.hasMoreTokens()) {
            System.out.println("Token: " + st.nextToken());
        }
    }

    public static List<String> getRendererNames() {
        List<String> renderers = new ArrayList<>();

        for (Map.Entry<String, String> stringStringEntry : dbRenderers.entrySet()) {
            Map.Entry e = (Map.Entry) stringStringEntry;
            renderers.add((String) e.getKey());
        }

        return renderers;
    }

    public static SchemaRenderer getRenderer(String name) {
        SchemaRenderer renderer = null;

        String className = dbRenderers.get(name.toLowerCase().trim());
        if (className != null) {
            try {
                renderer = (SchemaRenderer) Class.forName(className).newInstance();
            } catch (Exception e) {
                System.out.println("Could not find specified renderer [" + name + "] from known list, trying to load by given name...");
                try {
                    renderer = (SchemaRenderer) Class.forName(className).newInstance();
                } catch (Exception ex) {
                    System.out.println("Could load specified renderer [" + name + "]... using default");
                    renderer = new SchemaRenderer();
                }
            }
        }

        return renderer;
    }

    public List<SchemaTable> getTablesToGenerate(SchemaDatabase database, String[] tablesToGenerate) {
        // determine which tables to generate
        List<SchemaTable> requestedTables = new ArrayList<>();
        if (tablesToGenerate == null || (tablesToGenerate.length > 0 && tablesToGenerate[0] == null)) {
            requestedTables.addAll(database.getTables());
        } else {
            for (String tableToGenerate : tablesToGenerate) {
                SchemaTable table = database.getTable(tableToGenerate);
                if (table != null) {
                    requestedTables.add(table);
                } else {
                    showProgress("WARNING: Could not find requested table [" + tableToGenerate + "].", true);
                }
            }
        }

        return getTablesInCreateOrder(requestedTables);
    }

    /**
     * Returns a list of tables and the order in which they should be created.
     *
     * @param requestedTables List of Strings of tablesNames to be ordered.  Use null to
     *                        specify all tables in this database
     * @return List of Strings of tableNames (Ordered for creation)
     */
    public static List<SchemaTable> getTablesInCreateOrder(List<SchemaTable> requestedTables) {
        // order the tables in correct create order so that there are no db errors
        List<SchemaTable> orderedTables = new ArrayList<>();
        int lastOrderedTablesSize = 0;

        List<SchemaTable> tablesNotYetAdded = new ArrayList<>(requestedTables);
        List<SchemaTable> tablesCouldNotBeAdded;
        while (tablesNotYetAdded.size() > 0) {
            tablesCouldNotBeAdded = new ArrayList<>();
            for (SchemaTable table : tablesNotYetAdded) {
                if (tableListContainsAllFKFields(orderedTables, table)) {
                    orderedTables.add(table);
                } else {
                    tablesCouldNotBeAdded.add(table);
                }
            }

            // keep us from an infinite loop
            // if nothing was added to orderedTables.... then throw exception
            if (lastOrderedTablesSize == orderedTables.size()) {
                //int remainingTablesCount = tablesNotYetAdded.size() - orderedTables.size();

                for (SchemaTable missingDepTable : tablesNotYetAdded) {
                    System.out.println("WARNING: Could not find dependency table for table: [" + missingDepTable.getName() + "]");

                    // just add it anyway
                    orderedTables.add(missingDepTable);
                }
                break;
            }

            lastOrderedTablesSize = orderedTables.size();

            // this must be last
            tablesNotYetAdded = new ArrayList<>(tablesCouldNotBeAdded);
        }

        return orderedTables;
    }

    private static boolean tableListContainsAllFKFields(List<SchemaTable> orderedTables, SchemaTable table) {
        for (SchemaField field : table.getFields()) {
            String fkTable = field.getForeignKeyTable();

            // if
            // 1. fktable is not null
            // 2. fktable name IS something more than empty
            // 3. fktable name does NOT exist int the already added tables
            // 4. fktable is NOT this table
            if (fkTable != null && fkTable.length() > 0 && !tableListContainsTable(orderedTables, fkTable) && !fkTable.equalsIgnoreCase(table.getName())) {
                return false;
            }
        }
        return true;
    }

    private static boolean tableListContainsTable(List<SchemaTable> tables, String tableToMatch) {
        if (tableToMatch == null) {
            return false;
        }

        for (SchemaTable table : tables) {
            if (table.getName().equals(tableToMatch)) {
                return true;
            }
        }

        return false;
    }

    public List<SchemaView> getViewsToGenerate(SchemaDatabase database, String[] viewsToGenerate) {
        // determine which views to generate
        List<SchemaView> requestedViews = new ArrayList<>();
        if (viewsToGenerate == null || (viewsToGenerate.length > 0 && viewsToGenerate[0] == null)) {
            requestedViews.addAll(database.getViews());
        } else {
            for (String viewToGenerate : viewsToGenerate) {
                SchemaView view = database.getView(viewToGenerate);//(SchemaView) viewsByName.get(viewsToGenerate[i]);
                if (view != null) {
                    requestedViews.add(view);
                } else {
                    showProgress("WARNING: Could not find requested view [" + viewToGenerate + "].", true);
                }
            }
        }

        return requestedViews;
    }

    public String formatDefaultValue(SchemaField field) {
        return formatBaseDefaultValue(field);
    }

    public static String formatBaseDefaultValue(SchemaField field) {
        String defaultValue = field.getDefaultValue();
        String newDefaultValue;

        Class<?> javaType = field.getJavaClassType();
        if (defaultValue.equalsIgnoreCase("NULL")) {
            return defaultValue;
        }

        if (javaType == String.class) {
            newDefaultValue = "'" + defaultValue + "'";
        } else if (javaType == boolean.class) {
            if (defaultValue.equalsIgnoreCase("true") || defaultValue.equals("1")) {
                newDefaultValue = "1";
            } else {
                newDefaultValue = "0";
            }
        } else if (javaType == Date.class) {
            newDefaultValue = "'" + defaultValue + "'";
        } else if (javaType == Calendar.class) {
            newDefaultValue = "'" + defaultValue + "'";
        } else {
            newDefaultValue = defaultValue;
        }

        return newDefaultValue;
    }

    protected void showProgress(String text, boolean newLine) {
        if (showConsoleProgress) {//pm == null) {
            if (newLine) {
                ps.println(text);
            } else {
                ps.print(text);
            }
        }
    }

    public void run() {
        executeRenderer();
    }

    public String getSchemaXMLFilename() {
        return schemaXMLFilename;
    }

    /**
     * Setter for property schemaXMLFilename.
     * NOTE: schemaXMLFilename will be assumed to be a file on the filesystem (not a classpath resource)
     *
     * @param schemaXMLFilename New value of property schemaXMLFilename.
     */
    public void setSchemaXMLFilename(String schemaXMLFilename) {
        this.schemaXMLFilename = schemaXMLFilename;
        this.schemaXMLFilenameIsAResource = false;
    }

    /**
     * Setter for property schemaXMLFilename.
     *
     * @param schemaXMLFilename New value of property schemaXMLFilename.
     * @param isResource        file will be read out of classpath using schemaXMLFilename as the classpath
     */
    public void setSchemaXMLFilename(String schemaXMLFilename, boolean isResource) {
        this.schemaXMLFilename = schemaXMLFilename;
        this.schemaXMLFilenameIsAResource = isResource;
    }

    public String[] getTablesToGenerate() {
        return this.tablesToGenerate;
    }

    public void setTablesToGenerate(String[] tablesToGenerate) {
        this.tablesToGenerate = tablesToGenerate;
    }

    public boolean isDropTables() {
        return dropTables;
    }

    public void setDropTables(boolean dropTables) {
        this.dropTables = dropTables;
    }

    public boolean isExecuteSQLScriptFiles() {
        return executeSQLScriptFiles;
    }

    public void setExecuteSQLScriptFiles(boolean executeSQLScriptFiles) {
        this.executeSQLScriptFiles = executeSQLScriptFiles;
    }

    public boolean isShowConsoleProgress() {
        return showConsoleProgress;
    }

    public void setShowConsoleProgress(boolean showConsoleProgress) {
        this.showConsoleProgress = showConsoleProgress;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getDbVendorName() {
        return dbVendorName;
    }

    public void setDbVendorName(String dbVendorName) {
        // check to make sure the vendor name is valid
        if (!dbRenderers.containsKey(dbVendorName.toLowerCase())) {
            throw new IllegalArgumentException("Cannot find a renderer for Vendor [" + dbVendorName.toLowerCase() + "]");
        }

        this.dbVendorName = dbVendorName;
    }

    public String[] getViewsToGenerate() {
        return this.viewsToGenerate;
    }

    public void setViewsToGenerate(String[] viewsToGenerate) {
        this.viewsToGenerate = viewsToGenerate;
    }

    public boolean isCreateSchema() {
        return createSchema;
    }

    public void setCreateSchema(boolean createSchema) {
        this.createSchema = createSchema;
    }

    public boolean isCreatePostSchema() {
        return createPostSchema;
    }

    public void setCreatePostSchema(boolean createPostSchema) {
        this.createPostSchema = createPostSchema;
    }

    public String getMappingFilename() {
        return mappingFilename;
    }

    public void setMappingFilename(String mappingFilename) {
        this.mappingFilename = mappingFilename;
    }

    public DatabaseSchema getDbSchema() {
        return dbSchema;
    }

    public String getOtherInsertsFilename() {
        return otherInsertsFilename;
    }

    public void setOtherInsertsFilename(String otherInsertsFilename) {
        this.otherInsertsFilename = otherInsertsFilename;
    }

    public boolean isCreateONLYOtherInserts() {
        return createONLYOtherInserts;
    }

    public void setCreateONLYOtherInserts(boolean createONLYOtherInserts) {
        this.createONLYOtherInserts = createONLYOtherInserts;
    }

    public boolean isCreateEnumInserts() {
        return createEnumInserts;
    }

    public void setCreateEnumInserts(boolean createEnumInserts) {
        this.createEnumInserts = createEnumInserts;
    }

    public SchemaDatabase getDatabase() {
        return getDbSchema().getDatabase(databaseName);
    }
}
