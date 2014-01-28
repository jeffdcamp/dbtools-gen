package org.dbtools.schema.xmlfile;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Root
public class DBSchema {
    @Attribute(required = false)
    private String schemaLocation;

    @ElementList(entry = "database", inline = true)
    private List<SchemaDatabase> databases = new ArrayList<>();

    public List<SchemaDatabase> getDatabases() {
        return databases;
    }

    public void setDatabases(List<SchemaDatabase> databases) {
        this.databases = databases;
    }


    public static void main(String[] args) {
//        javaToXml();
        xmlToJava();
    }

    public static void javaToXml() {
        try {
            DBSchema schema = new DBSchema();

            List<SchemaDatabase> databases = new ArrayList<SchemaDatabase>();

            SchemaDatabase db1 = new SchemaDatabase("main");
            SchemaTable t1 = new SchemaTable("table1");

            SchemaField f1 = new SchemaField("ID", SchemaFieldType.BIGINT);
            f1.setIncrement(true);

            t1.getFields().add(f1);


            List<SchemaTable> db1Tables = new ArrayList<>();
            db1Tables.add(t1);
            db1Tables.add(new SchemaTable("table2"));
            db1.setTables(db1Tables);
            databases.add(db1);
            schema.setDatabases(databases);


            Serializer serializer = new Persister();
            File result = new File("/home/jcampbell/Desktop/example.xml");

            serializer.write(schema, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void xmlToJava() {
        try {
            Serializer serializer = new Persister();
            File source = new File("/home/jcampbell/src/ldschurch/android/ldstools/LDSToolsAndroid/src/main/database/schema.xml");
            DBSchema schema = serializer.read(DBSchema.class, source);

            if (schema != null) {
                System.out.println("SUCCESS");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
