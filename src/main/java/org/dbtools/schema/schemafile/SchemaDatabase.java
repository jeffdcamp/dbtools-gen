package org.dbtools.schema.schemafile;

import org.dbtools.schema.ClassInfo;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Root
public class SchemaDatabase {
    @Attribute
    private String name;

    @Attribute(required = false)
    private Boolean fieldsDefaultNotNull = null;

    @ElementList(entry = "table", inline = true, required = false)
    private List<SchemaTable> tables = new ArrayList<>();

    @ElementList(entry = "view", inline = true, required = false)
    private List<SchemaView> views = new ArrayList<>();

    @ElementList(entry = "query", inline = true, required = false)
    private List<SchemaQuery> queries = new ArrayList<>();

    @ElementList(entry = "postSQLScriptFile", inline = true, required = false)
    private List<PostSQLScriptFile> postSQLScriptFiles;

    public SchemaDatabase() {
    }

    public SchemaDatabase(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SchemaTable> getTables() {
        if (fieldsDefaultNotNull != null) {
            for (SchemaTable entity : tables) {
                entity.setFieldsDefaultNotNull(fieldsDefaultNotNull);
            }
        }

        return tables;
    }

    public void setTables(List<SchemaTable> tables) {
        this.tables = tables;
    }

    public List<SchemaView> getViews() {
        if (fieldsDefaultNotNull != null) {
            for (SchemaView entity : views) {
                entity.setFieldsDefaultNotNull(fieldsDefaultNotNull);
            }
        }

        return views;
    }

    public void setViews(List<SchemaView> views) {
        this.views = views;
    }

    public List<SchemaQuery> getQueries() {
        if (fieldsDefaultNotNull != null) {
            for (SchemaQuery entity : queries) {
                entity.setFieldsDefaultNotNull(fieldsDefaultNotNull);
            }
        }

        return queries;
    }

    public void setQueries(List<SchemaQuery> queries) {
        this.queries = queries;
    }

    public List<PostSQLScriptFile> getPostSQLScriptFiles() {
        return postSQLScriptFiles;
    }

    public void setPostSQLScriptFiles(List<PostSQLScriptFile> postSQLScriptFiles) {
        this.postSQLScriptFiles = postSQLScriptFiles;
    }

    public List<String> getTableNames() {
        List<String> names = new ArrayList<>();
        for (SchemaTable table : tables) {
            names.add(table.getName());
        }

        return names;
    }

    public List<String> getViewNames() {
        List<String> names = new ArrayList<>();
        for (SchemaView view : views) {
            names.add(view.getName());
        }

        return names;
    }

    /**
     * Case insensitive search for table
     */
    public SchemaTable getTable(String tableName) {
        for (SchemaTable table : tables) {
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
     * Case insensitive search for queries
     */
    public SchemaQuery getQuery(String queryName) {
        for (SchemaQuery query : queries) {
            if (query.getName().equalsIgnoreCase(queryName)) {
                return query;
            }
        }

        return null;
    }

    public ClassInfo getTableClassInfo(String tableName) {
        String className = ClassInfo.createJavaStyleName(tableName);
        return new ClassInfo(className, null);
    }

    public boolean validate() {
        // Check for duplicate table names
        Set<String> existingTableViewNames = new HashSet<>();
        Set<String> existingSequences = new HashSet<>();
        for (SchemaTable table : tables) {
            // table self validation
            table.validate();

            // Check for duplicate table names
            String tableName = table.getName();
            if (existingTableViewNames.contains(tableName)) {
                throw new IllegalStateException("Table named [" + tableName + "] already exists in database [" + getName() + "]");
            }
            existingTableViewNames.add(tableName);

            // Check for duplicate sequence name
            for (String seqName : table.getSequenceNames()) {
                if (existingSequences.contains(seqName)) {
                    throw new IllegalStateException("Sequencer named [" + seqName + "] already exists in database [" + getName() + "]");
                }
                existingSequences.add(seqName);
            }
        }

        // Check for duplicate view names
        for (String viewName : getViewNames()) {
            if (existingTableViewNames.contains(viewName)) {
                throw new IllegalStateException("View named [" + viewName + "] already exists in database [" + getName() + "]");
            }
            existingTableViewNames.add(viewName);
        }

        return true;
    }
}
