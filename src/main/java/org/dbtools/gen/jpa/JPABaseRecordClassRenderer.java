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

import org.dbtools.codegen.*;
import org.dbtools.schema.ClassInfo;
import org.dbtools.schema.SchemaDatabase;
import org.dbtools.schema.SchemaField;
import org.dbtools.schema.SchemaTable;

import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Jeff
 */
public class JPABaseRecordClassRenderer {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private JavaClass myClass;
    private JavaClass myTestClass;
    private boolean writeTestClass = true;

    private List<JavaEnum> enumerationClasses = new ArrayList<JavaEnum>();

    private StringBuilder toStringContent;
    private StringBuilder cleanupOrphansContent;

    private boolean includeXML = false;
    private boolean useDateTime = false; // use joda datetime or jsr 310
    private boolean useInnerEnums = true;

    public static final String CLEANUP_ORPHANS_METHODNAME = "cleanupOrphans";
    boolean uselegacyJUnit = false;
    boolean useBeanValidators = false;

    /**
     * Creates a new instance of JPABaseRecordClassRenderer
     */
    public JPABaseRecordClassRenderer() {
    }

    public void generateObjectCode(SchemaDatabase schemaDatabase, SchemaTable table, String packageName, String author, String version, PrintStream psLog) {
        String className = createClassName(table);

        if (table.isEnumerationTable()) {
            String enumClassname = createClassName(table);
            myClass = new JavaEnum(packageName, enumClassname, table.getEnumerations());
            myClass.setCreateDefaultConstructor(false);
            writeTestClass = false;

            if (table.getEnumValues().size() > 0) {
                // private static Map<ScheduleType, String> enumStringMap = new HashMap<ScheduleType, String>();
                myClass.addImport("java.util.Map");
                myClass.addImport("java.util.HashMap");
                JavaVariable enumStringMapVar = myClass.addVariable("Map<" + enumClassname + ", String>", "enumStringMap", "new HashMap<" + enumClassname + ", String>()");
                enumStringMapVar.setStatic(true);

                // private static List<String> stringList = new ArrayList<String>();
                myClass.addImport("java.util.List");
                myClass.addImport("java.util.ArrayList");
                JavaVariable stringListVar = myClass.addVariable("List<String>", "stringList", "new ArrayList<String>()");
                stringListVar.setStatic(true);

                Map<String, String> enumValues = table.getEnumValues();
                for (String enumItem : table.getEnumerations()) {
                    //enumStringMap.put(DRINK, "Drink");
                    //stringList.add("Breakfast");

                    String enumValue = enumValues.get(enumItem);
                    myClass.appendStaticInitializer("enumStringMap.put(" + enumItem + ", \"" + enumValue + "\");");
                    myClass.appendStaticInitializer("stringList.add(\"" + enumValue + "\");");
                    myClass.appendStaticInitializer("");
                }

                List<JavaVariable> getStringMParam = new ArrayList<JavaVariable>();
                getStringMParam.add(new JavaVariable(enumClassname, "key"));
                JavaMethod getStringM = myClass.addMethod(Access.PUBLIC, "String", "getString", getStringMParam, "return enumStringMap.get(key);");
                getStringM.setStatic(true);

                JavaMethod getListM = myClass.addMethod(Access.PUBLIC, "List<String>", "getList", "return stringList;");
                getListM.setStatic(true);
            }
        } else {
            myClass = new JavaClass(packageName, className);
            writeTestClass = true;
        }

        myTestClass = new JavaClass(packageName, className + "Test");
        initTestClass();

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
        String TAB = JavaClass.getTab();

        StringBuilder constructorElement = new StringBuilder();
        constructorElement.append("try {\n");
        StringBuilder methodToXML = new StringBuilder();
        StringBuilder methodToXMLExt = new StringBuilder();

        methodToXML.append("Element element = parent.addElement(TABLE);\n");
        StringBuffer dtd = new StringBuffer();
        dtd.append("<!ELEMENT ").append(table.getClassName()).append(" EMPTY>\n        <!ATTLIST ").append(table.getClassName()).append("\n");
        boolean primaryKeyAdded = false;

        // imports
        if (includeXML && !myClass.isEnum()) {
            myClass.addImport("org.dom4j.*");
            //myClass.addImport("org.dom4j.io.*");
        }

        // constants and variables
        String tableName = table.getName();
        myClass.addConstant("String", "TABLE", tableName);
        myClass.addConstant("String", "TABLE_CLASSNAME", JPARecordClassRenderer.createClassName(table));

        List<SchemaField> fields = table.getFields();
        for (SchemaField field : fields) {
            boolean primaryKey = field.isPrimaryKey();

            String fieldNameJavaStyle = field.getName(true);

            // check for second primary key
            if (primaryKey && primaryKeyAdded) {
                throw new IllegalStateException("Cannot have more than 1 Primary Key [" + fieldNameJavaStyle + "]");
            } else {
                primaryKeyAdded = true;
            }

            // constants
            String constName = JavaClass.formatConstant(fieldNameJavaStyle);
            myClass.addConstant("String", "C_" + constName, field.getName());
            myClass.addConstant("String", "FULL_C_" + constName, tableName + "." + field.getName());

            String propertyName = "P_" + constName;
            // P_ is set in the following switch statement

            // skip some types of variables at this point (so that we still get the column name and the property name)
            switch (field.getForeignKeyType()) {
                case MANYTOONE:
                    generateManyToOne(schemaDatabase, packageName, field, table);
                    continue;
                case ONETOMANY:
                    generateOneToMany(table, field, packageName, schemaDatabase);
                    continue;
                case ONETOONE:
                    generateOneToOne(schemaDatabase, table, field, packageName);
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

            // add primary key JPA annotations and default functions
            if (primaryKey && !myClass.isEnum()) {
                // add vanilla getID() for the primary key
                addFieldVariableAnnotations(field, fieldNameJavaStyle, newVariable);
            }

            // JPA stuff
            // JPA @Column
            if (!myClass.isEnum()) {
                myClass.addImport("javax.persistence.Column");
                String columnAnnotation = "@Column(name=\"" + field.getName() + "\"";

                if (field.getJdbcType().equals(SchemaField.TYPE_BLOB) || field.getJdbcType().equals(SchemaField.TYPE_CLOB)) {
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
                if (!useDateTime && field.getJdbcType().equals(SchemaField.TYPE_TIMESTAMP)) {
                    myClass.addImport(temporalImport);
                    myClass.addImport(temporalTypeImport);

                    newVariable.addAnnotation(MessageFormat.format(temporalAnnotation, "TIMESTAMP"));
                } else if (!useDateTime && field.getJdbcType().equals(SchemaField.TYPE_DATE)) {
                    myClass.addImport(temporalImport);
                    myClass.addImport(temporalTypeImport);

                    newVariable.addAnnotation(MessageFormat.format(temporalAnnotation, "DATE"));
                } else if (!useDateTime && field.getJdbcType().equals(SchemaField.TYPE_TIMESTAMP)) {
                    myClass.addImport(temporalImport);
                    myClass.addImport(temporalTypeImport);

                    newVariable.addAnnotation(MessageFormat.format(temporalAnnotation, "TIMESTAMP"));
                }

                if (useDateTime && newVariable.getDataType().endsWith("DateTime")) {
                    myClass.addImport("org.hibernate.annotations.Type");
                    myClass.addImport("org.springframework.format.annotation.DateTimeFormat");

                    newVariable.addAnnotation("@Type(type=\"org.jadira.usertype.dateandtime.joda.PersistentDateTime\")");
                    newVariable.addAnnotation("@DateTimeFormat(style=\"SS\")");
                }
            } // end of JPA stuff

            if (!myClass.isEnum()) {
                myClass.addVariable(newVariable);
            }

            if (includeXML && !myClass.isEnum()) {
                generateXMLCode(schemaDatabase, constructorElement, methodToXML, dtd, TAB, field, constName);
            }
        }

        // constructors
        if (includeXML && !myClass.isEnum()) {
            constructorElement.append("} catch (Exception e) {\n");
            constructorElement.append(TAB).append("e.printStackTrace();\n");
            constructorElement.append("}\n");
//            constructorElement.append("// dtd \n/*\n");
//            constructorElement.append("").append(dtd.toString());
//            constructorElement.append(">\n*/\n");
        }

        // methods
        addForgeignKeyData(schemaDatabase, table, packageName);

        // add method to cleanup many-to-one left-overs (till support CascadeType.DELETE-ORPHAN is supported in JPA)
        if (!myClass.isEnum()) {
            List<JavaVariable> orphanParams = new ArrayList<JavaVariable>();
            orphanParams.add(new JavaVariable("javax.persistence.EntityManager", "em"));
            myClass.addMethod(Access.PROTECTED, "void", CLEANUP_ORPHANS_METHODNAME, orphanParams, cleanupOrphansContent.toString());
        }

        if (!myClass.isEnum()) {
            // to String method
            toStringContent.append("return text;\n");
            JavaMethod toStringMethod = myClass.addMethod(Access.PUBLIC, "String", "toString", toStringContent.toString());
            toStringMethod.addAnnotation("Override");

            // new record check
            myClass.addMethod(Access.PUBLIC, "boolean", "isNewRecord", "return getID() <= 0;");


            // testing methods
            JavaMethod toStringTestMethod = myTestClass.addMethod(Access.PUBLIC, "void", "testToString", "assertNotNull(testRecord.toString());");
            toStringTestMethod.addAnnotation("Test");


        }

        // XML Support Methods
        if (includeXML && !myClass.isEnum()) {
            List<JavaVariable> xmlConstParams = new ArrayList<JavaVariable>();
            xmlConstParams.add(new JavaVariable("Element", "element"));
            myClass.addConstructor(Access.PUBLIC, xmlConstParams, constructorElement.toString());

            String toXMLContent = methodToXML.toString() + "\n// Many to One support (if any)\n" + methodToXMLExt.toString() + "\nreturn element;";
            List<JavaVariable> xmlToStrParams = new ArrayList<JavaVariable>();
            xmlToStrParams.add(new JavaVariable("Element", "parent"));
            myClass.addMethod(Access.PUBLIC, "Element", "toXML", xmlToStrParams, toXMLContent);
        }
    }

    private void addFieldVariableAnnotations(SchemaField field, String fieldNameJavaStyle, JavaVariable newVariable) {
        myClass.addImport("javax.persistence.Id");
        newVariable.addAnnotation("@Id");

        String sequencerName = field.getSequencerName();
        boolean hasSequencer = sequencerName != null && sequencerName.length() > 0;
        if (field.isIncrement() && !hasSequencer) {
            myClass.addImport("javax.persistence.GeneratedValue");
            myClass.addImport("javax.persistence.GenerationType");
            newVariable.addAnnotation("@GeneratedValue(strategy=GenerationType.AUTO)");
        } else if (hasSequencer) {
            myClass.addImport("javax.persistence.GeneratedValue");
            myClass.addImport("javax.persistence.SequenceGenerator");
            newVariable.addAnnotation("@GeneratedValue(generator=\"" + sequencerName + "\")");
            newVariable.addAnnotation("@SequenceGenerator(name=\"" + sequencerName + "\", sequenceName=\"" + sequencerName + "\", allocationSize=1)");
        }

        // add vanilla getID() for the primary key
        myClass.addMethod(Access.PUBLIC, field.getJavaTypeText(), "getID", "return " + fieldNameJavaStyle + ";");
    }

    private void createToStringMethodContent(final SchemaField field, final String fieldNameJavaStyle) {

        String fieldType = field.getJdbcType();
        if (!fieldType.equals(SchemaField.TYPE_BLOB) && !fieldType.equals(SchemaField.TYPE_CLOB)) {
            // toString
            toStringContent.append("text += \"").append(fieldNameJavaStyle).append(" = \"+ ").append(fieldNameJavaStyle).append(" +\"\\n\";\n");
        }
    }

    private JavaVariable generateEnumeration(SchemaField field, String fieldNameJavaStyle, String packageName, JavaVariable newVariable, SchemaDatabase dbSchema) {
        myClass.addImport("javax.persistence.Enumerated");
        myClass.addImport("javax.persistence.EnumType");
        if (field.isNumberDataType()) {
            if (field.getForeignKeyTable().length() > 0) {
                // define name of enum
                ClassInfo enumClassInfo = dbSchema.getTableClassInfo(field.getForeignKeyTable());
                String enumName = enumClassInfo.getClassName();

                // local definition of enumeration?
                List<String> localEnumerations = field.getEnumerations();
                if (localEnumerations != null && localEnumerations.size() > 0) {
                    myClass.addEnum(enumName, field.getEnumerations());
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

                addSetterGetterTest(newVariable);

                // JPA Stuff for enumeration
                newVariable.addAnnotation("Enumerated(EnumType.ORDINAL)");
            } else {
                // ENUM with out a foreign key table
                String javaStyleFieldName = field.getName(true);
                String firstChar = javaStyleFieldName.substring(0, 1).toUpperCase();
                String enumName = firstChar + javaStyleFieldName.substring(1);

                if (useInnerEnums) {
                    myClass.addEnum(enumName, field.getEnumerations());
                } else {
                    enumerationClasses.add(new JavaEnum(enumName, field.getEnumerations()));
                }

                newVariable = new JavaVariable(enumName, fieldNameJavaStyle);
                newVariable.setGenerateSetterGetter(true);
                newVariable.setDefaultValue(enumName + "." + field.getEnumerationDefault(), false);

                addSetterGetterTest(newVariable);

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
//        if (!field.isJavaTypePrimative()) { // && (fractionType || moneyType)) {
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
        if (dateType && useDateTime) {
            newVariable = new JavaVariable("org.joda.time.DateTime", fieldNameJavaStyle);
        } else {
            newVariable = new JavaVariable(typeText, fieldNameJavaStyle);
        }

        boolean immutableDate = field.getJavaClassType() == Date.class && useDateTime; // org.joda.time.DateTime IS immutable
        if (!field.isJavaTypePrimative() && !field.isJavaTypeImmutable() && !immutableDate) {
            newVariable.setCloneSetterGetterVar(true);
        }

        newVariable.setGenerateSetterGetter(true);
        addSetterGetterTest(newVariable);
//        }

        newVariable.setDefaultValue(defaultValue);

        return newVariable;
    }

    private void generateManyToOne(SchemaDatabase schemaDatabase, String packageName, SchemaField field, SchemaTable table) {
        String fkTableName = field.getForeignKeyTable();
        ClassInfo fkTableClassInfo = schemaDatabase.getTableClassInfo(fkTableName);
        String fkTableClassName = fkTableClassInfo.getClassName();
        String varName = field.getCustomVarName();
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

    private void generateOneToMany(SchemaTable table, SchemaField field, String packageName, SchemaDatabase dbSchema) {
        String fkTableName = field.getForeignKeyTable();
        ClassInfo fkTableClassInfo = dbSchema.getTableClassInfo(fkTableName);
        String fkTableClassName = fkTableClassInfo.getClassName();
        String varName = field.getCustomVarName();
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

    private void generateOneToOne(SchemaDatabase dbSchema, SchemaTable table, SchemaField field, String packageName) {
        String fkTableName = field.getForeignKeyTable();
        ClassInfo fkTableClassInfo = dbSchema.getTableClassInfo(fkTableName);
        String fkTableClassName = fkTableClassInfo.getClassName();
        String varName = field.getCustomVarName();
        if (varName.equals("")) {
            varName = JavaClass.formatToJavaVariable(fkTableClassName);
        }

        myClass.addConstant("String", "P_" + JavaClass.formatConstant(varName), varName);

        String newImport = fkTableClassInfo.getPackageName(packageName) + ".*";
        myClass.addImport(newImport);
        JavaVariable oneToOneVar = new JavaVariable(fkTableClassName, varName);

        myClass.addImport("javax.persistence.OneToOne");
        myClass.addImport("javax.persistence.FetchType");

        // determine the casecade type
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

        if (field.isPrimaryKey() && !myClass.isEnum()) {
            JavaMethod getIDMethod = new JavaMethod(Access.PUBLIC, field.getJavaTypeText(), "getID");
            getIDMethod.setContent("return " + varName + ".getID();");
            myClass.addMethod(getIDMethod);
        }
    }

    private void generateXMLCode(final SchemaDatabase dbSchema, final StringBuilder constructorElement, final StringBuilder methodToXML, final StringBuffer dtd, final String TAB, final SchemaField field, final String constName) {

        String fieldNameJavaStyle = field.getName(true);

        // XML methods
        // xmlConstructor
        String attributeName = "C_" + constName;
        String elementPreGetter = "";
        String elementGetter;

        Class fClass = field.getJavaClassType();
        if (!field.isEnumeration()) {
            if (fClass == String.class) {
                //elementGetter = "XMLUtil.getAttribute(element, "+ attributeName +", false, \"\")";
                elementGetter = "element.attributeValue(" + attributeName + ", \"\")";
            } else if (fClass == int.class || fClass == Integer.class) {
                elementGetter = "Integer.parseInt(element.attributeValue(" + attributeName + ", \"0\"))";
            } else if (fClass == float.class || fClass == Float.class) {
                elementGetter = "Float.parseFloat(element.attributeValue(" + attributeName + ", \"0.0\"))";
            } else if (fClass == boolean.class || fClass == Boolean.class) {
                elementGetter = "Boolean.parseBoolean(element.attributeValue(" + attributeName + ", \"false\"))";
            } else if (fClass == double.class || fClass == Double.class) {
                elementGetter = "Double.parseDouble(element.attributeValue(" + attributeName + ", \"0.0\"))";
            } else if (fClass == long.class || fClass == Long.class) {
                elementGetter = "Long.parseLong(element.attributeValue(" + attributeName + ", \"0\"))";
            } else if (fClass == Date.class) {
                String tmpTextName = fieldNameJavaStyle + "Text";
                elementPreGetter = "String " + tmpTextName + " = element.attributeValue(" + attributeName + ");";
                elementGetter = tmpTextName + " != null ? (new java.text.SimpleDateFormat(\"" + DATE_FORMAT + "\")).parse(" + tmpTextName + ")" +
                        " : null";
//            } else if (fClass == Fraction.class) {
//                myClass.addImport("com.jdc.datatypes.Fraction");
//                myClass.addImport("com.jdc.util.XMLUtil");
//                elementGetter = "XMLUtil.getAttributeFraction(element, "+ attributeName +", false, null)";
//            } else if (fClass == Money.class) {
//                myClass.addImport("com.jdc.datatypes.Money");
//                myClass.addImport("com.jdc.util.XMLUtil");
//                elementGetter = "XMLUtil.getAttributeMoney(element, "+ attributeName +", false, null)";
            } else {
                elementGetter = "element.attributeValue(" + attributeName + ", null)";
            }

        } else {
            // Enum
            ClassInfo myTableClassInfo = dbSchema.getTableClassInfo(field.getForeignKeyTable());
            String enumClassName = myTableClassInfo.getClassName();

            String tmpTextName = fieldNameJavaStyle + "Text";
            elementPreGetter = "String " + tmpTextName + " = element.attributeValue(" + attributeName + ");";
            elementGetter = tmpTextName + " != null ? " + enumClassName + ".valueOf(" + tmpTextName + ")"
                    + " : " + enumClassName + "." + field.getEnumerationDefault() + "";
        }

        String xmlSetterMethodName;
        if (field.isCreatedTimeStampField()) {
            xmlSetterMethodName = "setCreatedDate";
        } else if (field.isLastModifiedTimeStampField()) {
            xmlSetterMethodName = "setLastModifiedDate";
        } else {
            xmlSetterMethodName = JavaClass.createSetterMethodName(fieldNameJavaStyle);
        }
        if (!elementPreGetter.isEmpty()) {
            constructorElement.append(TAB).append(elementPreGetter).append("\n");
        }
        constructorElement.append(TAB).append(xmlSetterMethodName).append("(").append(elementGetter).append(");\n");


        // toXML()
        String getterMethodName = JavaClass.createGetterMethodName(fClass, fieldNameJavaStyle);
        if (!field.isEnumeration()) {
            if (fClass == int.class || fClass == float.class || fClass == double.class || fClass == long.class) {
                String toStringSeg = "";
                toStringSeg = field.getJavaType().getMatchingNonPrimativeClassText() + ".toString(";
                methodToXML.append("element.addAttribute(").append("C_").append(constName).append(", ").append(toStringSeg).append(getterMethodName).append("()));\n");// NOPMD
            } else {
                if (fClass == String.class) {
                    methodToXML.append("element.addAttribute(").append("C_").append(constName).append(", ").append(getterMethodName).append("());\n");
                } else {
                    if (fClass.isPrimitive()) {
                        methodToXML.append("element.addAttribute(").append("C_").append(constName).append(", new ").append(field.getJavaType().getMatchingNonPrimativeClassText()).append("(").append(getterMethodName).append("()).toString());\n");
                    } else {
                        if (fClass == Date.class) {
                            String dateGetterMethod;
                            if (field.isCreatedTimeStampField()) {
                                dateGetterMethod = "getCreatedDate()";
                            } else if (field.isLastModifiedTimeStampField()) {
                                dateGetterMethod = "getLastModifiedDate()";
                            } else {
                                dateGetterMethod = getterMethodName + "()";
                            }
                            methodToXML.append("if (").append(dateGetterMethod).append(" != null) {\n");

                            methodToXML.append("    java.text.SimpleDateFormat xmlDF = new java.text.SimpleDateFormat(\"" + DATE_FORMAT + "\");\n");
                            methodToXML.append("    element.addAttribute(").append("C_").append(constName).append(", xmlDF.format(").append(dateGetterMethod).append("));\n");

                            methodToXML.append("}\n");
                        } else {
                            methodToXML.append("element.addAttribute(").append("C_").append(constName).append(", ").append(getterMethodName).append("().toString());\n");
                        }
                    }
                }
            }
        } else {
            // enum
            methodToXML.append("element.addAttribute(").append("C_").append(constName).append(", ").append(getterMethodName).append("().toString());\n");
        }

        // dtd
        // default (String type)
        String dtdType = " CDATA ";
        String dtdDefault = " \"\"";

        if (fClass == int.class || fClass == float.class || fClass == double.class || fClass == long.class || fClass == Date.class) {
            dtdType = " NMTOKEN ";
            dtdDefault = " \"0\"";
        } else if (fClass == boolean.class) {
            dtdType = " (true | false) ";
            dtdDefault = " \"false\"";
        }

        dtd.append(TAB).append(fieldNameJavaStyle).append(dtdType).append(dtdDefault).append("\n");
    }

    private void addForgeignKeyData(SchemaDatabase dbSchema, SchemaTable table, String packageName) {
        String TAB = JavaClass.getTab();

        // find any other tables that depend on this one (MANYTOONE) or other tables this table depends on (ONETOONE)
        for (SchemaTable tmpTable : dbSchema.getTables()) {
            List<SchemaField> fkFields = tmpTable.getForeignKeyFields(table.getName());

            for (SchemaField fkField : fkFields) {
                switch (fkField.getForeignKeyType()) {
                    case ONETOMANY:
                        String fkTableName = tmpTable.getName();
                        ClassInfo fkTableClassInfo = dbSchema.getTableClassInfo(fkTableName);
                        String fkTableClassName = fkTableClassInfo.getClassName();
                        String newImport = fkTableClassInfo.getPackageName(packageName) + ".*";

                        String customVarName = fkField.getCustomVarName();
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
                        String mappedByVarName = fkField.getCustomVarName();
                        if (mappedByVarName == null || mappedByVarName.isEmpty()) {
                            mappedByVarName = JavaClass.formatToJavaVariable(mappedByClassInfo.getClassName());
                        }

                        myClass.addImport("javax.persistence.FetchType");

                        // determine the casecade type
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


                        String fieldName = fkField.getCustomVarName();
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
                        removeMethodContent += TAB + TAB + "throw new IllegalStateException(\"deleteItem failed: Cannot find itemID \"+ " + listVarName + ".getID());\n";
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

    public static String createClassName(SchemaTable table) {
        if (table.isEnumerationTable()) {
            return table.getClassName();
        } else {
            return table.getClassName() + "BaseRecord";
        }
    }

    public void writeToFile(String directoryname) {
        myClass.writeToDisk(directoryname);

        for (JavaEnum enumClass : enumerationClasses) {
            enumClass.writeToDisk(directoryname);
        }
    }

    public void writeTestsToFile(String directoryname) {
        if (writeTestClass) {
            myTestClass.writeToDisk(directoryname);
        }
    }

    private void initTestClass() {
        if (uselegacyJUnit) {
            myTestClass.addImport("junit.framework.*");
            myTestClass.setExtends("TestCase");
        } else {
            myTestClass.addImport("org.junit.*");
            myTestClass.addImport("static org.junit.Assert.*");
        }

        // header comment
        //Date now = new Date();
        //SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
        String fileHeaderComment;
        fileHeaderComment = "/*\n";
        fileHeaderComment += " * " + myTestClass.getName() + ".java\n";
        fileHeaderComment += " * \n";
        fileHeaderComment += " * GENERATED FILE - DO NOT EDIT\n";
        fileHeaderComment += " * \n";
        fileHeaderComment += " */\n";
        myTestClass.setFileHeaderComment(fileHeaderComment);

        if (uselegacyJUnit) {
            List<JavaVariable> params = new ArrayList<JavaVariable>();
            params.add(new JavaVariable("String", "testName"));
            myTestClass.addConstructor(Access.PUBLIC, params, "super(testName);");
        } else {
            myTestClass.setCreateDefaultConstructor(true);
        }

        // variables
        myTestClass.addVariable(myClass.getName(), "testRecord");

        // methods
        JavaMethod setUpMethod = myTestClass.addMethod(Access.PUBLIC, "void", "setUp", "testRecord = new " + myClass.getName() + "();\nassertNotNull(testRecord);");
        if (!uselegacyJUnit) {
            setUpMethod.addAnnotation("Before");
        }

        JavaMethod tearDownMethod = myTestClass.addMethod(Access.PUBLIC, "void", "tearDown", null);
        if (!uselegacyJUnit) {
            tearDownMethod.addAnnotation("After");
        }
    }

    private void addSetterGetterTest(JavaVariable newVariable) {
        DataType dataType = DataType.getDataType(newVariable.getDataType());

        JavaMethod testMethod = new JavaMethod(Access.PUBLIC, "void", "test" + JavaVariable.createBeanMethodName(newVariable.getName()));
        StringBuilder testContent = new StringBuilder();

        if (!uselegacyJUnit) {
            testMethod.addAnnotation("Test");
        }

        switch (dataType) {
            case STRING:
                testContent.append("String testData = \"abc\";\n");
                testContent.append("testRecord.").append(newVariable.getSetterMethodName()).append("(testData);\n");
                testContent.append("String recordData = testRecord.").append(newVariable.getGetterMethodName()).append("();\n");
                testContent.append("assertEquals(testData, recordData);");
                break;
            case CHAR:
                testContent.append("char testData = 'z';\n");
                testContent.append("testRecord.").append(newVariable.getSetterMethodName()).append("(testData);\n");
                testContent.append("char recordData = testRecord.").append(newVariable.getGetterMethodName()).append("();\n");
                testContent.append("assertEquals(testData, recordData);");
                break;
            case BOOLEAN:
                testContent.append("boolean testData = false;\n");
                testContent.append("testRecord.").append(newVariable.getSetterMethodName()).append("(testData);\n");
                testContent.append("boolean recordData = testRecord.").append(newVariable.getGetterMethodName()).append("();\n");
                testContent.append("assertEquals(testData, recordData);");
                break;
            case INT:
                testContent.append("int testData = 123;\n");
                testContent.append("testRecord.").append(newVariable.getSetterMethodName()).append("(testData);\n");
                testContent.append("int recordData = testRecord.").append(newVariable.getGetterMethodName()).append("();\n");
                testContent.append("assertEquals(testData, recordData);");
                break;
            case FLOAT:
                testContent.append("float testData = 123.56f;\n");
                testContent.append("testRecord.").append(newVariable.getSetterMethodName()).append("(testData);\n");
                testContent.append("float recordData = testRecord.").append(newVariable.getGetterMethodName()).append("();\n");
                testContent.append("assertEquals(testData, recordData, 0);");
                break;
            case DOUBLE:
                testContent.append("double testData = 123.56;\n");
                testContent.append("testRecord.").append(newVariable.getSetterMethodName()).append("(testData);\n");
                testContent.append("double recordData = testRecord.").append(newVariable.getGetterMethodName()).append("();\n");
                testContent.append("assertEquals(testData, recordData, 0);");
                break;
            case DATE:
                myTestClass.addImport("java.util.Calendar");
                myTestClass.addImport("java.util.Date");
                testContent.append("Calendar testData = Calendar.getInstance();\n");
                testContent.append("int testYear = 1980;\n");
                testContent.append("int testMonth = 2;\n");
                testContent.append("int testDay = 1;\n");
                testContent.append("testData.set(1980, 2, 1);\n");
                testContent.append("testRecord.").append(newVariable.getSetterMethodName()).append("(testData.getTime());\n");
                testContent.append("Date recordDataDate = testRecord.").append(newVariable.getGetterMethodName()).append("();\n");
                testContent.append("Calendar recordData = Calendar.getInstance();\n");
                testContent.append("recordData.setTime(recordDataDate);\n");
                testContent.append("int year = recordData.get(Calendar.YEAR);\n");
                testContent.append("int month = recordData.get(Calendar.MONTH);\n");
                testContent.append("int day = recordData.get(Calendar.DATE);\n");
                testContent.append("assertEquals(testYear, year);\n");
                testContent.append("assertEquals(testMonth, month);\n");
                testContent.append("assertEquals(testDay, day);\n");
                break;

//            case OBJECT:
//                testContent.append("Object testData = 123.56;\n");
//                testContent.append("testRecord."+ newVariable.getSetterMethodName() +"(testData);\n");
//                testContent.append("double recordData = testRecord."+ newVariable.getGetterMethodName() +"();\n");
//                testContent.append("assertEquals(testData, recordData);");

        }

        testMethod.setContent(testContent.toString());
        myTestClass.addMethod(testMethod);
    }

    public void setIncludeXML(boolean includeXML) {
        this.includeXML = includeXML;
    }

    public void setUseDateTime(boolean useDateTime) {
        this.useDateTime = useDateTime;
    }
}
