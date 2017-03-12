package org.dbtools.schema.schemafile;

import org.dbtools.schema.ClassInfo;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

@Root
public class SchemaTable extends SchemaEntity {
    @Attribute
    private String name;

    @Attribute(required = false)
    private String className;

    @Attribute(required = false)
    private String enumerations = "";

    @Attribute(required = false)
    private Boolean fieldsDefaultNotNull = null;

    @Attribute(required = false)
    private Boolean readOnly = false;

    @ElementList(entry = "index", inline = true, required = false)
    private List<SchemaTableIndex> indexDeclarations = new ArrayList<>();

    @ElementList(entry = "unique", inline = true, required = false)
    private List<SchemaTableUnique> uniqueDeclarations = new ArrayList<>();

    @ElementList(entry = "tableparameter", inline = true, required = false)
    private List<SchemaTableParameter> tableParameters = new ArrayList<>();

    @ElementList(entry = "field", inline = true)
    private List<SchemaTableField> fields = new ArrayList<>();

    public SchemaTable() {
    }

    public SchemaTable(String name) {
        this.name = name;
    }

    public boolean validate() {
        int primaryKeyCount = 0;
        for (SchemaTableField field : fields) {
            if (field.isPrimaryKey()) {
                primaryKeyCount++;

                if (primaryKeyCount > 1) {
                    throw new IllegalStateException("Cannot have 2 primary key fields for table [" + getName() + "].[" + field.getName() + "]");
                }
            }
            field.validate();
        }
        return true;
    }


    @Override
    public SchemaEntityType getType() {
        return SchemaEntityType.TABLE;
    }

    public String getParameter(String tableType) {
        for (SchemaTableParameter tableParameter : tableParameters) {
            if (tableParameter.getName().equalsIgnoreCase(tableType)) {
                return tableParameter.getValue();
            }
        }

        return null;
    }

    public boolean isEnumerationTable() {
        return !enumerations.isEmpty();
    }

    public List<String> getSequenceNames() {
        List<String> names = new ArrayList<>();
        for (SchemaTableField field : fields) {
            String seqName = field.getSequencerName();
            if (seqName != null && !seqName.isEmpty()) {
                names.add(seqName);
            }
        }

        return names;
    }

    public List<String> getTableEnumsText() {
        List<String> enumTextItems = new ArrayList<>();
        for (TableEnum enumItem : getTableEnums()) {
            enumTextItems.add(enumItem.getName());
        }

        return enumTextItems;
    }

    public List<TableEnum> getTableEnums() {
        List<TableEnum> enums = new ArrayList<>();

        String enumArrayStr = getEnumerations();
        if (enumArrayStr != null && enumArrayStr.length() > 0) {
            // cleanup the enumerations by doing:
            // 1. removing extra spaces in enum area
            StringBuilder cleanEnumArrayStr = new StringBuilder();
            boolean afterEqual = false;
            for (char c : enumArrayStr.toCharArray()) {
                if (c == '=') {
                    afterEqual = true;
                }

                if (c == ' ' && !afterEqual) {
                    //skip
                } else {
                    cleanEnumArrayStr.append(c);
                }
            }

            //  VALUE1=Jeff,VALUE2=Ricky
            String enumItem = "";
            String enumValue = "";
            afterEqual = false;
            int charCount = 0;
            for (char c : enumArrayStr.toCharArray()) {
                charCount++;

                if (c == ',') {
                    enums.add(createTableEnum(enumItem.trim(), enumValue.trim()));

                    enumItem = "";
                    enumValue = "";
                    afterEqual = false;
                } else {
                    if (c == '=' && !afterEqual) {
                        afterEqual = true;
                        continue;
                    }

                    if (!afterEqual) {
                        enumItem += c;
                    } else {
                        enumValue += c;
                    }

                    // if this is the last character.... store
                    if (charCount == enumArrayStr.length()) {
                        enums.add(createTableEnum(enumItem.trim(), enumValue.trim()));
                    }
                }

            }
        }

        return enums;
    }

    /**
     * Returns a list of Fields that reference a specified SchemaTable
     *
     * @param tableName Name of table that fields reference
     * @return List of Fields (foreign key)
     */
    public List<SchemaTableField> getForeignKeyFields(String tableName) {
        List<SchemaTableField> fkFields = new ArrayList<SchemaTableField>();

        for (SchemaTableField field : fields) {
            String fkTable = field.getForeignKeyTable();
            if (fkTable != null && fkTable.equalsIgnoreCase(tableName)) {
                fkFields.add(field);
            }
        }

        return fkFields;
    }

    /**
     * Returns a list of Fields that reference another table.
     *
     * @return List of Fields (foreign key)
     */
    public List<SchemaTableField> getForeignKeyFields() {
        List<SchemaTableField> fkFields = new ArrayList<SchemaTableField>();

        for (SchemaTableField field : fields) {
            String fkTable = field.getForeignKeyTable();
            if (fkTable != null && fkTable.length() > 0) {
                fkFields.add(field);
            }
        }

        return fkFields;
    }

    private TableEnum createTableEnum(String enumItem, String enumValue) {
        String value;

        if (enumValue.length() != 0) {
            value = null;
        } else {
            // make the enumItem into a enumValue
            char prevEnumChar = ' ';
            String newEnumValue = "";
            int enumItemItr = 0;
            for (char enumChar : enumItem.toCharArray()) {
                if (enumItemItr == 0 || prevEnumChar == '_') {
                    newEnumValue += Character.toUpperCase(enumChar);
                } else if (enumChar == '_') {
                    // replace _ with space
                    newEnumValue += " ";
                } else {
                    newEnumValue += Character.toLowerCase(enumChar);
                }

                prevEnumChar = enumChar;
                enumItemItr++;
            }

            value = newEnumValue;
        }

        return new TableEnum(enumItem, value);
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

    public void setClassName(String className) {
        this.className = className;
    }

    public String getEnumerations() {
        return enumerations;
    }

    public void setEnumerations(String enumerations) {
        this.enumerations = enumerations;
    }

    public List<SchemaTableIndex> getIndexDeclarations() {
        return indexDeclarations;
    }

    public void setIndexDeclarations(List<SchemaTableIndex> indexDeclarations) {
        this.indexDeclarations = indexDeclarations;
    }

    public List<SchemaTableUnique> getUniqueDeclarations() {
        return uniqueDeclarations;
    }

    public void setUniqueDeclarations(List<SchemaTableUnique> uniqueDeclarations) {
        this.uniqueDeclarations = uniqueDeclarations;
    }

    public List<SchemaTableParameter> getTableParameters() {
        return tableParameters;
    }

    public void setTableParameters(List<SchemaTableParameter> tableParameters) {
        this.tableParameters = tableParameters;
    }

    public List<SchemaTableField> getFields() {
        setFieldDefaults();
        return fields;
    }

    public void setFields(List<SchemaTableField> fields) {
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

    public boolean isReadonly() {
        return readOnly != null ? readOnly : false;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }
}
