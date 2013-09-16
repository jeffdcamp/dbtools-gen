package org.dbtools.schema;

import org.dom4j.Element;

import java.io.File;
import java.util.*;

public class SchemaDatabase {

    private String name;

    //    private List<RefDatabaseSchema> referenceSchemas = new ArrayList<RefDatabaseSchema>();
    private List<SchemaTable> tables = new ArrayList<SchemaTable>();
    private Map<String, SchemaTable> tablesByName = new TreeMap<String, SchemaTable>();
    private List<SchemaView> views = new ArrayList<SchemaView>();
    private Map<String, SchemaView> viewsByName = new TreeMap<String, SchemaView>();
    private Map<String, String> tableClassNames = new HashMap<String, String>();  // key:
    private List<String> postSQLScriptFiles = null;

    // error checking
    private Set<String> sequenceNameSet = new HashSet<String>();


    public SchemaDatabase(String schemaFilename, String dbVendorName, boolean schemaXMLFilenameIsAResource, Element dbElement) {
        // read reference schemas (if any)
        //readReferences(dbElement, schemaFilename);

        name = dbElement.attribute("name").getValue();

        // get the document root
        Iterator tablesItr = dbElement.elementIterator("table");
        Element tableElement;

        // remove all existing tables
        removeAllTables();

        // add tables...
        while (tablesItr.hasNext()) {
            tableElement = (Element) tablesItr.next();
            addTable(dbVendorName, tableElement);
        }

        // get views...
        Iterator viewsItr = dbElement.elementIterator("view");
        Element viewElement;

        // remove all existing views
        removeAllViews();

        // add views...
        while (viewsItr.hasNext()) {
            viewElement = (Element) viewsItr.next();
            addView(viewElement);
        }


        // get all inserts
        // remove any existing
        postSQLScriptFiles = new ArrayList<String>();

        // add inserts...
        for (Element postSQLScriptElement : (List<Element>) dbElement.elements("postSQLScriptFile")) {

            String insertsPathname = postSQLScriptElement.attribute("pathname").getValue();
            boolean relativePath = true; // inserts are relative to where the schema is?
            try {
                relativePath = Boolean.parseBoolean(postSQLScriptElement.attribute("relativePath").getValue());
            } catch (Exception e) {
                e.printStackTrace();
            }

            insertsPathname = prepareFilepath(schemaFilename, insertsPathname, schemaXMLFilenameIsAResource, relativePath);

            // put pathname into inserts array
            //                System.out.println("Adding inserts: " + insertsPathname);
            postSQLScriptFiles.add(insertsPathname);
        }
    }

    public Element toXML(Element parent) {
        Element element = parent.addElement("database");
        element.addAttribute("name", name);

        for (SchemaTable table : getTables()) {
            table.toXML(element);
        }

        return element;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private boolean addTable(String dbVendorName, Element tableElement) {
        //showProgress("Adding table " + tableElement.getAttribute("name").getValue() + "...", true);
        SchemaTable table = new SchemaTable(dbVendorName, tableElement);

        duplicateTableViewCheck(table.getName());

        // check for duplicate sequence name
        SchemaField primaryKey = table.getPrimaryKey();
        if (primaryKey != null) {
            String seqName = primaryKey.getSequencerName();
            if (seqName != null && seqName.length() > 0) {
                if (!sequenceNameSet.contains(seqName)) {
                    sequenceNameSet.add(seqName);
                } else {
                    throw new IllegalStateException("Sequence [" + seqName + "] from table [" + table.getName() + "] ALREADY exists!");
                }
            }
        }

        tables.add(table);
        tablesByName.put(table.getName(), table);
        tableClassNames.put(table.getName(), table.getClassName());

        return true;
    }

    private void removeAllTables() {
        tables = new ArrayList<SchemaTable>();
        tablesByName = new HashMap<String, SchemaTable>();
    }

    private boolean addView(Element viewElement) {
        SchemaView view = new SchemaView(viewElement);
        views.add(view);
        viewsByName.put(view.getName(), view);

        return true;
    }

    private void duplicateTableViewCheck(String name) {
        if (tableViewNameExistsInSchema(name)) {
            throw new IllegalStateException("SchemaTable/SchemaView [" + name + "] ALREADY exists in schema");
        }
    }

    private void removeAllViews() {
        views = new ArrayList<SchemaView>();
        viewsByName = new HashMap<String, SchemaView>();
    }

    /**
     * Getter for property tables.
     *
     * @return Value of property tables.
     */
    public List<SchemaTable> getTables() {
        return tables;
    }

    public List<String> getTableNames() {
        List<String> tableNames = new ArrayList<String>(tables.size());

        for (SchemaTable table : tables) {
            tableNames.add(table.getName());
        }

        Collections.sort(tableNames, String.CASE_INSENSITIVE_ORDER);

        return tableNames;
    }

    /**
     * Case insensitive search for table
     */
    public SchemaTable getTable(String tableName) {
        for (SchemaTable table : tables) {
            //System.out.println("Found table: " + table.getName() + " =? " + tableName);
            if (table.getName().equalsIgnoreCase(tableName)) {
                return table;
            }
        }

        return null;
    }

    /**
     * Case insensitive search for views
     */
    public SchemaView getView(String viewName) {
        for (SchemaView view : views) {
            if (view.getName().equalsIgnoreCase(viewName)) {
                return view;
            }
        }

        return null;
    }

    /**
     * Getter for property views.
     *
     * @return Value of property views.
     */
    public java.util.List<SchemaView> getViews() {
        return views;
    }

    public boolean tableViewNameExistsInSchema(String name) {
        boolean exists = tablesByName.containsKey(name) || viewsByName.containsKey(name);

//        // if it does not exist, check reference Schemas of THIS schema
//        if (!exists) {
//            for (RefDatabaseSchema refDbSchema : referenceSchemas) {
//                DatabaseSchema dbSchema = refDbSchema.getDbSchema();
//                if (dbSchema.tableViewNameExistsInSchema(name)) {
//                    exists = true;
//                    break;
//                }
//            }
//        }

        return exists;
    }

    public ClassInfo getTableClassInfo(String tableName) {
        return getTableClassInfo(tableName, true);
    }

    public ClassInfo getTableClassInfo(String tableName, boolean failOnNotFound) {
        ClassInfo classInfo = null;
        String className = tableClassNames.get(tableName);

        if (className != null) {
            classInfo = new ClassInfo(className, null);
        } else {
//            // try reference Schemas
//            for (RefDatabaseSchema refDBSchema : referenceSchemas) {
//                DatabaseSchema dbSchemaInRef = refDBSchema.getDbSchema();
//                ClassInfo refClassInfo = dbSchemaInRef.getTableClassInfo(tableName);
//                if (refClassInfo != null) {
//                    classInfo = refClassInfo;
//                    classInfo.setPackageName(refDBSchema.getBaseJavaPackage());
//                    break;
//                }
//            }

            if (classInfo == null && failOnNotFound) {
                throw new IllegalArgumentException("Cannot find table named [" + tableName + "].  Be sure that the name of the table name is correct (Including case sensitive, check foreign key table references for errors)");
            }
        }

        return classInfo;
    }

    private void reset() {
        sequenceNameSet = new HashSet<String>();
    }

    public List<String> getPostSQLScriptFiles() {
        return postSQLScriptFiles;
    }

    public void setPostSQLScriptFiles(List<String> postSQLScriptFiles) {
        this.postSQLScriptFiles = postSQLScriptFiles;
    }


    //    private void readReferences(Element root, String parentSchemaFilename) {
//        Element referenceElement = root.element("references");
//
//        if (referenceElement != null) {
//            Iterator schemaItr = referenceElement.elementIterator("schema");
//            while (schemaItr.hasNext()) {
//                Element schemaElement = (Element) schemaItr.next();
//                readSchemaReference(schemaElement, parentSchemaFilename);
//            }
//        }
//    }
//
//    private void readSchemaReference(Element schemaElement, String parentSchemaFilename) {
//        String path = XMLUtil.getAttribute(schemaElement, "path", true);
//        String baseJavaPackage = XMLUtil.getAttribute(schemaElement, "baseJavaPackage", true);
//        boolean isInClassPath = XMLUtil.getAttributeBoolean(schemaElement, "readFromClasspath", false, false);
//
//        DatabaseSchema refSchema = new DatabaseSchema();
//
//        // try to find the file
//        if (!isInClassPath) {
//            path = findReferenceFile(path, parentSchemaFilename);
//        }
//
//        refSchema.readXMLSchema(path, baseJavaPackage, isInClassPath);
//        referenceSchemas.add(new RefDatabaseSchema(path, refSchema, baseJavaPackage));
//    }
//
//    private String findReferenceFile(String path, String parentSchemaFilename) {
//        String pathToUse;
//
//        // check to see if raw path is available
//        File rawFile = new File(path);
//        if (rawFile.exists()) {
//            pathToUse = path;
//        } else {
//            // try as a relative path
//            pathToUse = prepareFilepath(parentSchemaFilename, path, false, true);
//        }
//
//        return pathToUse;
//    }
//
    private String prepareFilepath(String schemaFilename, String filePathToPrepare, boolean schemaXMLFilenameIsAResource, boolean relativePath) {
        String preparedFilepath;

        // append current directory to path if this is a relative path
        if (relativePath) {
            if (schemaXMLFilenameIsAResource) {
                // inserts are also a resource
                // strip off the filename from the classpath of the
                // xmlschema filename
                String resourcePath = "";
                char SEPERATOR = '/';

                String pathSegment = "";
                for (int i = 0; i < schemaFilename.length(); i++) {
                    char nextChar = schemaFilename.charAt(i);

                    if (nextChar == SEPERATOR) {
                        resourcePath += pathSegment + SEPERATOR;

                        // reset
                        pathSegment = "";
                    } else {
                        pathSegment += nextChar;
                    }
                }

                preparedFilepath = resourcePath + filePathToPrepare;
            } else {
                // inserts are on the file system
                File schemaFile = new File(schemaFilename);
                int filenameSize = schemaFile.getName().length();
                String path = schemaFile.getPath();
                preparedFilepath = path.substring(0, path.length() - filenameSize) + filePathToPrepare;
            }
        } else {
            preparedFilepath = filePathToPrepare;
        }

        return preparedFilepath;
    }
}
