/*
 * BaseRecordClassRenderer.java
 *
 * Created on November  8, 2002
 *
 * Copyright 2007 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */

package org.dbtools.gen.jpa;

import org.dbtools.codegen.java.*;
import org.dbtools.gen.DateType;
import org.dbtools.gen.GenConfig;
import org.dbtools.schema.ClassInfo;
import org.dbtools.schema.schemafile.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author Jeff
 */
public class JPABaseRecordRenderer {

    public static final String CLEANUP_ORPHANS_METHOD_NAME = "cleanupOrphans";
    public static final String PRIMARY_KEY_COLUMN = "PRIMARY_KEY_COLUMN";
    public static final String PRIMARY_KEY_PROPERTY_COLUMN = "PRIMARY_KEY_PROPERTY_COLUMN";

    private JavaClass myClass;

    private List<JavaEnum> enumerationClasses = new ArrayList<>();

    private StringBuilder toStringContent;
    private StringBuilder cleanupOrphansContent;

    private boolean useInnerEnums = true;

    private boolean useBeanValidators = false;
    private GenConfig genConfig;

    /**
     * Creates a new instance of JPABaseRecordClassRenderer
     */
    public JPABaseRecordRenderer() {
    }

    public void generate(SchemaDatabase schemaDatabase, SchemaEntity entity, String packageName) {
        String className = createClassName(entity);
        SchemaEntityType entityType = entity.getType();

        if (entity.isEnumerationTable() && entityType == SchemaEntityType.TABLE) {
            SchemaTable table = (SchemaTable) entity;

            String enumClassName = createClassName(entity);
            myClass = new JavaEnum(packageName, enumClassName, table.getTableEnumsText());
            myClass.setCreateDefaultConstructor(false);

            List<TableEnum> tableEnums = table.getTableEnums();
            if (tableEnums.size() > 0) {
                // private static Map<ScheduleType, String> enumStringMap = new HashMap<ScheduleType, String>();
                myClass.addImport("java.util.Map");
                myClass.addImport("java.util.HashMap");
                JavaVariable enumStringMapVar = myClass.addVariable("Map<" + enumClassName + ", String>", "enumStringMap", "new HashMap<" + enumClassName + ", String>()");
                enumStringMapVar.setStatic(true);

                // private static List<String> stringList = new ArrayList<String>();
                myClass.addImport("java.util.List");
                myClass.addImport("java.util.ArrayList");
                JavaVariable stringListVar = myClass.addVariable("List<String>", "stringList", "new ArrayList<String>()");
                stringListVar.setStatic(true);

                for (TableEnum enumItem : tableEnums) {
                    //enumStringMap.put(DRINK, "Drink");
                    //stringList.add("Breakfast");

                    myClass.appendStaticInitializer("enumStringMap.put(" + enumItem.getName() + ", \"" + enumItem.getValue() + "\");");
                    myClass.appendStaticInitializer("stringList.add(\"" + enumItem.getValue() + "\");");
                    myClass.appendStaticInitializer("");
                }

                List<JavaVariable> getStringMParam = new ArrayList<>();
                getStringMParam.add(new JavaVariable(enumClassName, "key"));
                JavaMethod getStringM = myClass.addMethod(Access.PUBLIC, "String", "getString", getStringMParam, "return enumStringMap.get(key);");
                getStringM.setStatic(true);

                JavaMethod getListM = myClass.addMethod(Access.PUBLIC, "List<String>", "getList", "return stringList;");
                getListM.setStatic(true);
            }
        } else {
            myClass = new JavaClass(packageName, className);
            myClass.addImport("org.dbtools.jpa.domain.JPABaseRecord");
            myClass.setExtends("JPABaseRecord");
        }

        // prep
        toStringContent = new StringBuilder();
        toStringContent.append("String text = \"\\n\";\n");
        cleanupOrphansContent = new StringBuilder();

        // header comment
        // Do not place date in file because it will cause a new check-in to scm        
        String fileHeaderComment;
        fileHeaderComment = "/*\n";
        fileHeaderComment += " * " + className + ".java\n";
        fileHeaderComment += " *\n";
        fileHeaderComment += " * GENERATED FILE - DO NOT EDIT\n";
        fileHeaderComment += " * CHECKSTYLE:OFF\n";
        fileHeaderComment += " * \n";
        fileHeaderComment += " */\n";
        myClass.setFileHeaderComment(fileHeaderComment);

        // Since this is generated code.... suppress all warnings
        myClass.addAnnotation("@SuppressWarnings(\"all\")");

        // JPA
        if (!myClass.isEnum()) {
            myClass.addImplements("java.io.Serializable");
            myClass.addAnnotation("@javax.persistence.MappedSuperclass()");
        }

        StringBuilder constructorElement = new StringBuilder();
        constructorElement.append("try {\n");
        boolean primaryKeyAdded = false;

        // constants and variables
        String tableName = entity.getName();
        myClass.addConstant("String", "TABLE", tableName);
        myClass.addConstant("String", "TABLE_CLASSNAME", JPARecordClassRenderer.createClassName(entity));

        List<? extends SchemaField> fields = entity.getFields();
        for (SchemaField field : fields) {
            boolean primaryKey = field.isPrimaryKey();

            String fieldName = field.getName();

            String fieldNameJavaStyle = field.getName(true);

            // check for second primary key
            if (primaryKey) {
                if (primaryKeyAdded) {
                    throw new IllegalStateException("Cannot have more than 1 Primary Key [" + fieldNameJavaStyle + "]");
                } else {
                    primaryKeyAdded = true;
                }
            }

            // constants
            String constName = JavaClass.formatConstant(fieldNameJavaStyle);
            String fieldKey = "C_" + constName;

            if (primaryKey) {
                myClass.addConstant("String", PRIMARY_KEY_COLUMN, fieldName); // add a reference to this column
                myClass.addConstant("String", PRIMARY_KEY_PROPERTY_COLUMN, fieldNameJavaStyle); // add a reference to this column
            }

            myClass.addConstant("String", fieldKey, fieldName);
            myClass.addConstant("String", "FULL_C_" + constName, tableName + "." + fieldName);

            String propertyName = "P_" + constName;
            // P_ is set in the following switch statement

            // skip some types of variables at this point (so that we still get the column name and the property name)
            switch (field.getForeignKeyType()) {
                case MANYTOONE:
                    generateManyToOne(schemaDatabase, packageName, field);
                    continue;
                case ONETOMANY:
                    generateOneToMany(schemaDatabase, packageName, field);
                    continue;
                case ONETOONE:
                    generateOneToOne(schemaDatabase, packageName, field);
                    continue;
                default:
                    myClass.addConstant("String", propertyName, fieldNameJavaStyle);
            }

            createToStringMethodContent(field, fieldNameJavaStyle);

            // creates the variable OR changes the var to an enum
            JavaVariable newVariable = null;
            if (field.isEnumeration()) {
                newVariable = generateEnumeration(field, fieldNameJavaStyle, packageName, newVariable, schemaDatabase);
            } else {
                newVariable = generateFieldVariable(fieldNameJavaStyle, field);
            }

            // add @Id field for pk
            if (primaryKey) {
                addPkFieldVariableAnnotations(field, fieldNameJavaStyle, newVariable);
//                newVariable.addAnnotation("javax.persistence.Id");
            }

            // add primary key JPA annotations and default functions
            if (primaryKey && !myClass.isEnum()) {
                myClass.addMethod(Access.PUBLIC, "String", "getIdColumnName", "return " + fieldKey + ";").addAnnotation("Override");

                // add vanilla getPrimaryKeyId() / setPrimaryKeyId(...) for the primary key
                myClass.addMethod(Access.PUBLIC, field.getJavaTypeText(), "getPrimaryKeyId", "return " + fieldNameJavaStyle + ";").addAnnotation("Override");

                List<JavaVariable> setIdParams = new ArrayList<>();
                setIdParams.add(new JavaVariable(newVariable.getDataType(), "id"));
                myClass.addMethod(Access.PUBLIC, "void", "setPrimaryKeyId", setIdParams, "this." + fieldNameJavaStyle + " = id;").addAnnotation("Override");
            }

            // JPA stuff
            // JPA @Column
            SchemaFieldType fieldType = field.getJdbcDataType();
            if (!myClass.isEnum()) {
                myClass.addImport("javax.persistence.Column");
                String columnAnnotation = "@Column(name=\"" + fieldName + "\"";

                if (fieldType == SchemaFieldType.BLOB || fieldType == SchemaFieldType.CLOB) {
                    myClass.addImport("javax.persistence.Basic");
                    myClass.addImport("javax.persistence.FetchType");
                    newVariable.addAnnotation("Basic(fetch=FetchType.LAZY)");

                    myClass.addImport("javax.persistence.Lob");
                    newVariable.addAnnotation("Lob");
                }

                if (field.getSize() > 0) {
                    columnAnnotation += ", length=" + field.getSize();
                }
                if (field.getDecimals() > 0) {
                    columnAnnotation += ", precision=" + field.getDecimals();
                }

                if (field.isNotNull()) {
                    columnAnnotation += ", nullable=false"; // defaults to true
                    if (useBeanValidators) {
                        myClass.addImport("org.hibernate.validator.NotNull");
                        newVariable.addAnnotation("NotNull");
                    }
                }
                if (field.isUnique()) {
                    columnAnnotation += ", unique=true"; // defaults to false
                }
                columnAnnotation += ")";
                newVariable.addAnnotation(columnAnnotation);

                // JPA @Temporal (if applicable)
                String temporalImport = "javax.persistence.Temporal";
                String temporalTypeImport = "javax.persistence.TemporalType";
                String temporalAnnotation = "@Temporal(value = TemporalType.{0})";
                if (fieldType == SchemaFieldType.TIMESTAMP) {
                    myClass.addImport(temporalImport);
                    myClass.addImport(temporalTypeImport);

                    newVariable.addAnnotation(MessageFormat.format(temporalAnnotation, "TIMESTAMP"));
                } else if (fieldType == SchemaFieldType.DATETIME) {
                    myClass.addImport(temporalImport);
                    myClass.addImport(temporalTypeImport);

                    newVariable.addAnnotation(MessageFormat.format(temporalAnnotation, "DATETIME"));
                } else if (fieldType == SchemaFieldType.DATE) {
                    myClass.addImport(temporalImport);
                    myClass.addImport(temporalTypeImport);

                    newVariable.addAnnotation(MessageFormat.format(temporalAnnotation, "DATE"));
                } else if (fieldType == SchemaFieldType.TIME) {
                    myClass.addImport(temporalImport);
                    myClass.addImport(temporalTypeImport);

                    newVariable.addAnnotation(MessageFormat.format(temporalAnnotation, "TIME"));
                }

                if (newVariable.getDataType().endsWith("Date")) {
                    if (genConfig.getDateType() == DateType.JODA) {
                        newVariable.setGenerateSetterGetter(false);

                        String type = "org.joda.time.DateTime";

                        // create setter / getter
                        String getterContent = "return new org.joda.time.DateTime(" + newVariable.getName() + ");";
                        myClass.addMethod(newVariable.getGenerateGetterAccess(), type, newVariable.getGetterMethodName(), getterContent);

                        String newVarName = newVariable.getName();
                        String setterContent = "this." + newVarName + " = " + newVarName + ".toDate();";
                        JavaMethod setterMethod = new JavaMethod(newVariable.getGenerateSetterAccess(), "void", newVariable.getSetterMethodName());
                        setterMethod.addParameter(new JavaVariable(type, newVarName));
                        setterMethod.setContent(setterContent);
                        myClass.addMethod(setterMethod);
                    } else if (genConfig.getDateType() == DateType.JSR_310) {
                        newVariable.setGenerateSetterGetter(false);

                        String type = "java.time.LocalDateTime";

                        // create setter / getter
                        String getterContent = "return new java.time.LocalDateTime(" + newVariable.getName() + ");";
                        myClass.addMethod(newVariable.getGenerateGetterAccess(), type, newVariable.getGetterMethodName(), getterContent);

                        String newVarName = newVariable.getName();
                        String setterContent = "this." + newVarName + " = " + newVarName + ".toDate();";
                        JavaMethod setterMethod = new JavaMethod(newVariable.getGenerateSetterAccess(), "void", newVariable.getSetterMethodName());
                        setterMethod.addParameter(new JavaVariable(type, newVarName));
                        setterMethod.setContent(setterContent);
                        myClass.addMethod(setterMethod);
                    }
                }
            } // end of JPA stuff

            if (!myClass.isEnum()) {
                myClass.addVariable(newVariable);
            }
        }

//        System.out.println(myClass.getName() + " TYPE: " + entityType + " primaryKeyAdded: " + primaryKeyAdded);
        if (!primaryKeyAdded && (entityType == SchemaEntityType.VIEW || entityType == SchemaEntityType.QUERY)) {
            myClass.addMethod(Access.PUBLIC, "String", "getIdColumnName", "return null;").addAnnotation("Override");

            // add vanilla getPrimaryKeyId() / setPrimaryKeyId() for the primary key
            myClass.addMethod(Access.PUBLIC, "long", "getPrimaryKeyId", "return 0;").addAnnotation("Override");
            myClass.addMethod(Access.PUBLIC, "void", "setPrimaryKeyId", Arrays.asList(new JavaVariable("long", "id")), "").addAnnotation("Override");
        }

        // constructors

        // methods
        addForeignKeyData(schemaDatabase, entity, packageName);

        // add method to cleanup many-to-one left-overs (till support CascadeType.DELETE-ORPHAN is supported in JPA)
        if (!myClass.isEnum()) {
            List<JavaVariable> orphanParams = new ArrayList<>();
            orphanParams.add(new JavaVariable("javax.persistence.EntityManager", "em"));
            myClass.addMethod(Access.PUBLIC, "void", CLEANUP_ORPHANS_METHOD_NAME, orphanParams, cleanupOrphansContent.toString());
        }

        if (!myClass.isEnum()) {
            // to String method
            toStringContent.append("return text;\n");
            JavaMethod toStringMethod = myClass.addMethod(Access.PUBLIC, "String", "toString", toStringContent.toString());
            toStringMethod.addAnnotation("Override");

            // new record check
            myClass.addMethod(Access.PUBLIC, "boolean", "isNewRecord", "return getPrimaryKeyId() <= 0;");
        }
    }

    private void addPkFieldVariableAnnotations(SchemaField field, String fieldNameJavaStyle, JavaVariable newVariable) {
        myClass.addImport("javax.persistence.Id");
        newVariable.addAnnotation("@Id");

        String sequencerName = field.getSequencerName();
        boolean hasSequencer = sequencerName != null && sequencerName.length() > 0;
        if (field.isIncrement() && !hasSequencer) {
            myClass.addImport("javax.persistence.GeneratedValue");
            myClass.addImport("javax.persistence.GenerationType");
            newVariable.addAnnotation("@GeneratedValue(strategy=GenerationType.IDENTITY)");
        } else if (hasSequencer) {
            myClass.addImport("javax.persistence.GeneratedValue");
            myClass.addImport("javax.persistence.SequenceGenerator");
            newVariable.addAnnotation("@GeneratedValue(generator=\"" + sequencerName + "\")");
            newVariable.addAnnotation("@SequenceGenerator(name=\"" + sequencerName + "\", sequenceName=\"" + sequencerName + "\", allocationSize=1)");
        }
    }

    private void createToStringMethodContent(final SchemaField field, final String fieldNameJavaStyle) {

        SchemaFieldType fieldType = field.getJdbcDataType();
        if (fieldType != SchemaFieldType.BLOB && fieldType != SchemaFieldType.CLOB) {
            // toString
            toStringContent.append("text += \"").append(fieldNameJavaStyle).append(" = \"+ ").append(fieldNameJavaStyle).append(" +\"\\n\";\n");
        }
    }

    private JavaVariable generateEnumeration(SchemaField field, String fieldNameJavaStyle, String packageName, JavaVariable newVariable, SchemaDatabase dbSchema) {
        myClass.addImport("javax.persistence.Enumerated");
        myClass.addImport("javax.persistence.EnumType");
        if (field.getJdbcDataType().isNumberDataType()) {
            if (!field.getForeignKeyTable().isEmpty()) {
                // define name of enum
                ClassInfo enumClassInfo = dbSchema.getTableClassInfo(field.getForeignKeyTable());
                String enumName = enumClassInfo.getClassName();

                // local definition of enumeration?
                List<String> localEnumerations = field.getEnumValues();
                if (localEnumerations != null && localEnumerations.size() > 0) {
                    myClass.addEnum(enumName, field.getEnumValues());
                } else {
                    // we must import the enum
                    String enumPackage = enumClassInfo.getPackageName(packageName) + "." + enumName;

                    // build foreign key packagename
//                    String[] packageElements = packageName.split("\\.");
//                    for (int i = 0; i < packageElements.length - 1; i++) {
//                        enumPackage += packageElements[i] + ".";
//                    }
//                    enumPackage += enumName.toLowerCase() + "." + enumName;


                    myClass.addImport(enumPackage);
                }

                newVariable = new JavaVariable(enumName, fieldNameJavaStyle);
                newVariable.setGenerateSetterGetter(true);
                newVariable.setDefaultValue(enumName + "." + field.getEnumerationDefault(), false);

                // JPA Stuff for enumeration
                newVariable.addAnnotation("Enumerated(EnumType.ORDINAL)");
            } else if (!field.getEnumerationClass().isEmpty()) {
                // use user defined class
                String enumClassName = field.getEnumerationClass();

                newVariable = new JavaVariable(enumClassName, fieldNameJavaStyle);
                newVariable.setGenerateSetterGetter(true);
                newVariable.setDefaultValue(enumClassName + "." + field.getEnumerationDefault(), false);
            } else {
                // ENUM without a foreign key table
                String javaStyleFieldName = field.getName(true);
                String firstChar = javaStyleFieldName.substring(0, 1).toUpperCase();
                String enumName = firstChar + javaStyleFieldName.substring(1);

                if (useInnerEnums) {
                    myClass.addEnum(enumName, field.getEnumValues());
                } else {
                    enumerationClasses.add(new JavaEnum(enumName, field.getEnumValues()));
                }

                newVariable = new JavaVariable(enumName, fieldNameJavaStyle);
                newVariable.setGenerateSetterGetter(true);
                newVariable.setDefaultValue(enumName + "." + field.getEnumerationDefault(), false);

                // JPA Stuff for enumeration
                newVariable.addAnnotation("Enumerated(EnumType.ORDINAL)");
            }
        } else {
            newVariable = new JavaVariable(field.getJavaTypeText(), fieldNameJavaStyle);
        }

        return newVariable;
    }

    private JavaVariable generateFieldVariable(String fieldNameJavaStyle, SchemaField field) {
        JavaVariable newVariable;

        String typeText = field.getJavaTypeText();
        String defaultValue = field.getFormattedClassDefaultValue();

//        boolean fractionType = typeText.endsWith("Fraction");
//        boolean moneyType = typeText.endsWith("Money");
        boolean dateType = typeText.endsWith("Date");

        // Special handling for Fraction and Money
//        if (!field.isJavaTypePrimitive()) { // && (fractionType || moneyType)) {
//            // both Money and Fraction are both float at the core of JPA
//            String dataType = "float";
//            newVariable = new JavaVariable(dataType, fieldNameJavaStyle);
//
//            // custom setters and getters to change primative to Fraction or Money
//            JavaMethod setterMethod = new JavaMethod(Access.PUBLIC, "void", newVariable.getSetterMethodName());
//            setterMethod.addParameter(new JavaVariable(typeText, newVariable.getName()));
//            setterMethod.setContent("this." + newVariable.getName() + " = " + newVariable.getName() + ".floatValue();");
//            myClass.addMethod(setterMethod);
//
//            JavaMethod getterMethod = new JavaMethod(Access.PUBLIC, typeText, newVariable.getGetterMethodName());
//            getterMethod.setContent("return new " + typeText + "(" + newVariable.getName() + ");");
//            myClass.addMethod(getterMethod);
//        } else {
//        if (dateType && dateTimeSupport) {
//            newVariable = new JavaVariable("org.joda.time.DateTime", fieldNameJavaStyle);
//        } else {
            newVariable = new JavaVariable(typeText, fieldNameJavaStyle);
//        }

        SchemaFieldType fieldType = field.getJdbcDataType();
        boolean immutableDate = field.getJavaClassType() == Date.class; // && dateTimeSupport; // org.joda.time.DateTime IS immutable
        if (!fieldType.isJavaTypePrimitive() && !fieldType.isJavaTypeImmutable() && !immutableDate) {
            newVariable.setCloneSetterGetterVar(true);
        }

        newVariable.setGenerateSetterGetter(true);
        newVariable.setDefaultValue(defaultValue);

        return newVariable;
    }

    private void generateManyToOne(SchemaDatabase schemaDatabase, String packageName, SchemaField field) {
        String fkTableName = field.getForeignKeyTable();
        ClassInfo fkTableClassInfo = schemaDatabase.getTableClassInfo(fkTableName);
        String fkTableClassName = fkTableClassInfo.getClassName();
        String varName = field.getVarName();
        if (varName.equals("")) {
            varName = JavaClass.formatToJavaVariable(fkTableClassName);
        }

        myClass.addConstant("String", "P_" + JavaClass.formatConstant(varName), varName);

        String newImport = fkTableClassInfo.getPackageName(packageName) + ".*";
        myClass.addImport(newImport);
        JavaVariable manyToOneVar = new JavaVariable(fkTableClassName, varName);

        myClass.addImport("javax.persistence.ManyToOne");
        myClass.addImport("javax.persistence.FetchType");
        manyToOneVar.addAnnotation("@ManyToOne(fetch=FetchType." + field.getForeignKeyFetchType() + ")");

        myClass.addImport("javax.persistence.JoinColumn");
        if (field.isNotNull()) {
            manyToOneVar.addAnnotation("@JoinColumn(name=\"" + field.getName() + "\", nullable=false)");
        } else {
            manyToOneVar.addAnnotation("@JoinColumn(name=\"" + field.getName() + "\")");
        }

        myClass.addVariable(manyToOneVar, true);
    }

    private void generateOneToMany(SchemaDatabase schemaDatabase, String packageName, SchemaField field) {
        String fkTableName = field.getForeignKeyTable();
        ClassInfo fkTableClassInfo = schemaDatabase.getTableClassInfo(fkTableName);
        String fkTableClassName = fkTableClassInfo.getClassName();
        String varName = field.getVarName();
        if (varName.equals("")) {
            varName = JavaClass.formatToJavaVariable(fkTableClassName);
        }

        myClass.addConstant("String", "P_" + JavaClass.formatConstant(varName), varName);

        String newImport = fkTableClassInfo.getPackageName(packageName) + ".*";
        myClass.addImport(newImport);
        JavaVariable manyToOneVar = new JavaVariable(fkTableClassName, varName);

        myClass.addImport("javax.persistence.ManyToOne");
        myClass.addImport("javax.persistence.FetchType");
        manyToOneVar.addAnnotation("@ManyToOne(fetch=FetchType." + field.getForeignKeyFetchType() + ")");

        myClass.addImport("javax.persistence.JoinColumn");
        if (field.isNotNull()) {
            manyToOneVar.addAnnotation("@JoinColumn(name=\"" + field.getName() + "\", nullable=false)");
        } else {
            manyToOneVar.addAnnotation("@JoinColumn(name=\"" + field.getName() + "\")");
        }

        myClass.addVariable(manyToOneVar, true);
    }

    private void generateOneToOne(SchemaDatabase schemaDatabase, String packageName, SchemaField field) {
        String fkTableName = field.getForeignKeyTable();
        ClassInfo fkTableClassInfo = schemaDatabase.getTableClassInfo(fkTableName);
        String fkTableClassName = fkTableClassInfo.getClassName();
        String varName = field.getVarName();
        if (varName.equals("")) {
            varName = JavaClass.formatToJavaVariable(fkTableClassName);
        }

        myClass.addConstant("String", "P_" + JavaClass.formatConstant(varName), varName);

        String newImport = fkTableClassInfo.getPackageName(packageName) + ".*";
        myClass.addImport(newImport);
        JavaVariable oneToOneVar = new JavaVariable(fkTableClassName, varName);

        myClass.addImport("javax.persistence.OneToOne");
        myClass.addImport("javax.persistence.FetchType");

        // determine the cascade type
        String cascadeType = field.getForeignKeyCascadeType();
        String cascadeTypeAnnotation = "";
        if (cascadeType != null && cascadeType.length() > 0) {
            myClass.addImport("javax.persistence.CascadeType");
            cascadeTypeAnnotation = "cascade=CascadeType." + cascadeType + ", ";
        }

        oneToOneVar.addAnnotation("@OneToOne(" + cascadeTypeAnnotation + "fetch=FetchType." + field.getForeignKeyFetchType() + ")");

        myClass.addImport("javax.persistence.JoinColumn");
        if (field.isNotNull()) {
            oneToOneVar.addAnnotation("@JoinColumn(name=\"" + field.getName() + "\", nullable=false)");
        } else {
            oneToOneVar.addAnnotation("@JoinColumn(name=\"" + field.getName() + "\")");
        }

        myClass.addVariable(oneToOneVar, true);
    }

    private void addForeignKeyData(SchemaDatabase dbSchema, SchemaEntity entity, String packageName) {
        String TAB = JavaClass.getTab();

        // find any other tables that depend on this one (MANYTOONE) or other tables this table depends on (ONETOONE)
        for (SchemaTable tmpTable : dbSchema.getTables()) {
            List<SchemaTableField> fkFields = tmpTable.getForeignKeyFields(entity.getName());

            for (SchemaTableField fkField : fkFields) {
                switch (fkField.getForeignKeyType()) {
                    case ONETOMANY:
                        String fkTableName = tmpTable.getName();
                        ClassInfo fkTableClassInfo = dbSchema.getTableClassInfo(fkTableName);
                        String fkTableClassName = fkTableClassInfo.getClassName();
                        String newImport = fkTableClassInfo.getPackageName(packageName) + ".*";

                        String customVarName = fkField.getVarName();
                        String listVarName = customVarName != null && !customVarName.isEmpty() ?
                                JavaClass.formatToJavaVariable(customVarName, fkTableClassName) :
                                JavaClass.formatToJavaVariable(fkTableClassName);

                        String items = listVarName + "Items";
                        String itemsToDelete = listVarName + "ItemsToDelete";

                        myClass.addImport(newImport);
                        myClass.addImport("javax.persistence.OneToMany");

                        myClass.addImport("java.util.Set");
                        myClass.addImport("java.util.HashSet");
                        String listType = "Set<" + fkTableClassName + ">";
                        String defaultListTypeValue = "new HashSet<" + fkTableClassName + ">()";
                        JavaVariable itemsList = myClass.addVariable(listType, items);
                        itemsList.setDefaultValue(defaultListTypeValue);

                        myClass.addMethod(Access.PUBLIC, listType, JavaVariable.getGetterMethodName(listType, items), "return java.util.Collections.unmodifiableSet(" + items + ");");

                        ClassInfo mappedByClassInfo = dbSchema.getTableClassInfo(fkField.getForeignKeyTable());

                        String mappedByVarName = fkField.getVarName();
                        if (mappedByVarName == null || mappedByVarName.isEmpty()) {
                            mappedByVarName = JavaClass.formatToJavaVariable(mappedByClassInfo.getClassName());
                        }

                        myClass.addImport("javax.persistence.FetchType");

                        // determine the cascade type
                        String cascadeType = fkField.getForeignKeyCascadeType();
                        String cascadeTypeAnnotation = "";
                        if (cascadeType != null && cascadeType.length() > 0) {
                            myClass.addImport("javax.persistence.CascadeType");
                            cascadeTypeAnnotation = "cascade=CascadeType." + cascadeType + ", ";
                        }


                        itemsList.addAnnotation("@OneToMany(mappedBy=\"" + mappedByVarName + "\", " + cascadeTypeAnnotation + "fetch=FetchType." + fkField.getForeignKeyFetchType() + ")");

                        String orderByColumn = fkField.getForeignKeyOrderByColumn();
                        if (orderByColumn != null && orderByColumn.length() > 0) {
                            myClass.addImport("javax.persistence.OrderBy");
                            itemsList.addAnnotation("@OrderBy(\"" + orderByColumn + "\")");
                        }

                        // addItem method
                        JavaMethod addMethod = new JavaMethod(JavaClass.formatToJavaMethod("add", listVarName));
                        addMethod.setAccess(Access.PUBLIC);
                        addMethod.addParameter(new JavaVariable(fkTableClassName, listVarName));
                        String addMethodContent = "";

                        ClassInfo myTableClassInfo = dbSchema.getTableClassInfo(fkField.getForeignKeyTable());
                        String tableClassName = myTableClassInfo.getClassName();


                        String fieldName = fkField.getVarName();
                        if (fieldName == null || fieldName.length() == 0) {
                            fieldName = tableClassName;
                        }

                        String setterMethodName = "set" + fieldName.toUpperCase().charAt(0) + fieldName.substring(1, fieldName.length());

                        addMethodContent += listVarName + "." + setterMethodName + "((" + tableClassName + ")this);\n";
                        addMethodContent += items + ".add(" + listVarName + ");\n";
                        addMethod.setContent(addMethodContent);
                        myClass.addMethod(addMethod);

                        // deleteItem method
                        myClass.addImport("javax.persistence.Transient");
                        JavaVariable itemsToDeleteList = myClass.addVariable(listType, itemsToDelete);
                        itemsToDeleteList.setDefaultValue(defaultListTypeValue);
                        itemsToDeleteList.addAnnotation("@Transient");

                        //itemsToDeleteList.setGenerateGetter(true);
                        //itemsToDeleteList.setGenerateGetterAccess(Access.PROTECTED);

                        JavaMethod removeMethod = new JavaMethod(JavaClass.formatToJavaMethod("delete", listVarName));
                        removeMethod.setAccess(Access.PUBLIC);
                        removeMethod.addParameter(new JavaVariable(fkTableClassName, listVarName));

                        String removeMethodContent = "";
                        removeMethodContent += "if (" + listVarName + " == null) {\n";
                        removeMethodContent += TAB + "return;\n";
                        removeMethodContent += "}\n\n";
                        removeMethodContent += "java.util.Iterator<" + fkTableClassName + "> itr = " + items + ".iterator();\n";
                        removeMethodContent += "while (itr.hasNext()) {\n";
                        removeMethodContent += TAB + fkTableClassName + " item = itr.next();\n";
                        removeMethodContent += TAB + "if (item.equals(" + listVarName + ")) {\n";
                        removeMethodContent += TAB + TAB + "itr.remove();\n";
                        removeMethodContent += TAB + TAB + itemsToDelete + ".add(item);\n";
                        removeMethodContent += TAB + TAB + "break;\n";
                        removeMethodContent += TAB + "}\n";
                        removeMethodContent += TAB + "if (!itr.hasNext()) {\n";
                        removeMethodContent += TAB + TAB + "throw new IllegalStateException(\"deleteItem failed: Cannot find itemID \"+ " + listVarName + ".getId());\n";
                        removeMethodContent += TAB + "}\n";
                        removeMethodContent += "}";

                        removeMethod.setContent(removeMethodContent);
                        myClass.addMethod(removeMethod);

                        // add to cleanup orphans
                        cleanupOrphansContent.append("for (").append(fkTableClassName).append(" itemToDelete : ").append(itemsToDelete).append(") {\n");
                        cleanupOrphansContent.append(TAB).append("try {\n");
                        cleanupOrphansContent.append(TAB).append(TAB).append("em.remove(itemToDelete);\n");
                        cleanupOrphansContent.append(TAB).append("} catch(RuntimeException e) {// do nothing... it is ok if it does not exist\n");
                        cleanupOrphansContent.append(TAB).append("}\n");
                        cleanupOrphansContent.append("}\n\n");
                        break;
                    case ONETOONE:
                        // do nothing .... one to one stuff happens below
                        break;

                    case IGNORE:
                    default:
                }
            }
        }
    }

    public static String createClassName(SchemaEntity entity) {
        if (entity.isEnumerationTable()) {
            return entity.getClassName();
        } else {
            return entity.getClassName() + "BaseRecord";
        }
    }

    public void writeToFile(String directoryname) {
        myClass.writeToDisk(directoryname);

        for (JavaEnum enumClass : enumerationClasses) {
            enumClass.writeToDisk(directoryname);
        }
    }

    public void setGenConfig(GenConfig genConfig) {
        this.genConfig = genConfig;
    }
}
