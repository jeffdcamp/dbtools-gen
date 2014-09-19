package org.dbtools.schema.schemafile;

import org.dbtools.schema.ClassInfo;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

@Root
public class SchemaView extends SchemaEntity {
    @Attribute
    private String name;

    @Attribute(required = false)
    private String className;

    @Attribute(required = false)
    private Boolean fieldsDefaultNotNull = null;

    @ElementList(entry = "field", inline = true)
    List<SchemaViewField> fields = new ArrayList<>();

    @Override
    public SchemaEntityType getType() {
        return SchemaEntityType.VIEW;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassName() {
        if (className == null || className.isEmpty()) {
            return ClassInfo.createJavaStyleName(name);
        }
        return className;
    }

    @Override
    public boolean isEnumerationTable() {
        return false;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<? extends SchemaField> getFields() {
        setFieldDefaults();
        return fields;
    }

    public void setFields(List<SchemaViewField> fields) {
        this.fields = fields;
    }

    public Boolean isFieldsDefaultNotNull() {
        return fieldsDefaultNotNull;
    }

    public void setFieldsDefaultNotNull(Boolean fieldsDefaultNotNull) {
        this.fieldsDefaultNotNull = fieldsDefaultNotNull;
    }

    private void setFieldDefaults() {
        if (fieldsDefaultNotNull != null) {
            for (SchemaField field : fields) {
                field.setNotNullDefaultValue(fieldsDefaultNotNull);
            }
        }
    }
}
