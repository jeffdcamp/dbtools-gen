package org.dbtools.schema.dbmappings;

import org.dbtools.schema.schemafile.SchemaFieldType;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * User: jcampbell
 * Date: 2/4/14
 */
@Root
public class TypeMapping {
    @Element(name = "jdbc-type")
    private SchemaFieldType jdbcType;

    @Element(name = "java-type")
    private String javaType;

    @Element(name = "sql-type")
    private String sqlType;

    public SchemaFieldType getJdbcType() {
        return jdbcType;
    }

    public void setJdbcType(SchemaFieldType jdbcType) {
        this.jdbcType = jdbcType;
    }

    public String getJavaType() {
        return javaType;
    }

    public void setJavaType(String javaType) {
        this.javaType = javaType;
    }

    public String getSqlType() {
        return sqlType;
    }

    public void setSqlType(String sqlType) {
        this.sqlType = sqlType;
    }
}
