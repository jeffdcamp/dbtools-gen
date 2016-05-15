package org.dbtools.schema.dbmappings;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * User: jcampbell
 * Date: 2/4/14
 */
@Root
public class DatabaseMappings {
    @ElementList(entry = "type-mapping", inline = true)
    private List<DatabaseMapping> databaseMappings = new ArrayList<>();

    public List<DatabaseMapping> getDatabaseMappings() {
        return databaseMappings;
    }

    public void setDatabaseMappings(List<DatabaseMapping> databaseMappings) {
        this.databaseMappings = databaseMappings;
    }

    public static void main(String[] args) {
//        javaToXml();
        xmlToJava();
    }

//    public static void javaToXml() {
//        try {
//            DatabaseSchema schema = new DatabaseSchema();
//
//            List<SchemaDatabase> databases = new ArrayList<SchemaDatabase>();
//
//            SchemaDatabase db1 = new SchemaDatabase("main");
//            SchemaTable t1 = new SchemaTable("table1");
//
//            SchemaField f1 = new SchemaField("ID", SchemaFieldType.BIGINT);
//            f1.setIncrement(true);
//
//            t1.getFields().add(f1);
//
//
//            List<SchemaTable> db1Tables = new ArrayList<>();
//            db1Tables.add(t1);
//            db1Tables.add(new SchemaTable("table2"));
//            db1.setTables(db1Tables);
//            databases.add(db1);
//            schema.setDatabases(databases);
//
//
//            Serializer serializer = new Persister();
//            File result = new File("/home/jcampbell/Desktop/example.xml");
//
//            serializer.write(schema, result);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public static void xmlToJava() {
        try {
            Serializer serializer = new Persister();
            File source = new File("/home/jcampbell/src/jdc/dbtools-gen/src/main/resources/org/dbtools/xml/dbmappings.xml");
            DatabaseMappings mappings = serializer.read(DatabaseMappings.class, source);

            if (mappings != null) {
                System.out.println("SUCCESS");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
