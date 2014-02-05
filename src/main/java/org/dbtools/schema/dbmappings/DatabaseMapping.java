package org.dbtools.schema.dbmappings;

import org.dbtools.schema.schemafile.SchemaFieldType;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

/**
 * User: jcampbell
 * Date: 2/4/14
 */
@Root
public class DatabaseMapping {
    @Element(name = "name")
    private String databaseName;

    @ElementList(entry = "mapping", inline = true)
    private List<TypeMapping> mappings = new ArrayList<>();

    public String getSqlType(SchemaFieldType type) {
        for (TypeMapping mapType : mappings) {
            if (mapType.getJdbcType() == type) {
                return mapType.getSqlType();
            }
        }

        return null;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public List<TypeMapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<TypeMapping> mappings) {
        this.mappings = mappings;
    }
}
