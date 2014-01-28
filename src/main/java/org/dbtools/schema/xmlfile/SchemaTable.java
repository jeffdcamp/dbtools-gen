package org.dbtools.schema.xmlfile;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

@Root
public class SchemaTable {
    @Attribute
    private String name;

    @Attribute(required = false)
    private String className;

    @Attribute(required = false)
    private String enumerations;

    @Element(required = false)
    private SchemaTableUnique unique;

    @ElementList(entry = "tableparameter", inline = true, required = false)
    private List<SchemaTableParameter> tableParameters;

    @ElementList(entry = "field", inline = true)
    List<SchemaField> fields = new ArrayList<SchemaField>();

    public SchemaTable() {
    }

    public SchemaTable(String name) {
        this.name = name;
    }

    public List<String> getSequenceNames() {
        List<String> names = new ArrayList<>();
        for (SchemaField field : fields) {
            String seqName = field.getSequencerName();
            if (seqName != null && !seqName.isEmpty()) {
                names.add(seqName);
            }
        }

        return names;
    }

    public boolean validate() {
        int primaryKeyCount = 0;
        for (SchemaField field : fields) {
            if (field.isPrimaryKey()) {
                primaryKeyCount++;

                if (primaryKeyCount > 1) {
                    throw new IllegalStateException("Cannot have 2 primary key fields for table [" + getName() + "].[" + field.getName() + "]");
                }
            }
        }


        return true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getEnumerations() {
        return enumerations;
    }

    public void setEnumerations(String enumerations) {
        this.enumerations = enumerations;
    }

    public SchemaTableUnique getUnique() {
        return unique;
    }

    public void setUnique(SchemaTableUnique unique) {
        this.unique = unique;
    }

    public List<SchemaTableParameter> getTableParameters() {
        return tableParameters;
    }

    public void setTableParameters(List<SchemaTableParameter> tableParameters) {
        this.tableParameters = tableParameters;
    }

    public List<SchemaField> getFields() {
        return fields;
    }

    public void setFields(List<SchemaField> fields) {
        this.fields = fields;
    }
}
