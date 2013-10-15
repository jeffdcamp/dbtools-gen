/*
 * AndroidBaseRecordClassRenderer.java
 *
 * Created on Sep 9, 2010
 *
 * Copyright 2010 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.gen.android;

import org.dbtools.codegen.*;
import org.dbtools.schema.*;

import java.io.PrintStream;
import java.util.*;

/**
 * @author Jeff
 */
public class AndroidBaseRecordClassRenderer {

    private JavaClass myClass;
    private JavaClass myTestClass;
    private boolean writeTestClass = true;
    private List<JavaEnum> enumerationClasses = new ArrayList<JavaEnum>();
    private StringBuilder toStringContent;
    private StringBuilder cleanupOrphansContent;
    private boolean includeXML = false;
    private boolean useInnerEnums = true;
    public static final String CLEANUP_ORPHANS_METHODNAME = "cleanupOrphans";
    private static final String ALL_KEYS_VARNAME = "ALL_KEYS";
    private boolean uselegacyJUnit = false;

    private boolean useDateTime = false; // use joda datetime or jsr 310
    private boolean dateTimeHelperMethodsAdded = false;

    /**
     * Creates a new instance of AndroidBaseRecordClassRenderer.
     */
    public AndroidBaseRecordClassRenderer() {
    }

    public void generateObjectCode(SchemaDatabase database, SchemaTable table, String packageName, PrintStream psLog) {
        String className = createClassName(table);

        // sqlite table field types
        Map<String, String> sqlDataTypes = new HashMap<String, String>();
        Map<String, String> javaTypes = new HashMap<String, String>();
        SchemaRenderer.readXMLTypes(this.getClass(), SchemaRenderer.DEFAULT_TYPE_MAPPING_FILENAME, "sqlite", sqlDataTypes, javaTypes);


        if (table.isEnumerationTable()) {
            String enumClassname = createClassName(table);
            List<String> enums = table.getEnumerations();

            myClass = new JavaEnum(packageName, enumClassname, enums);
            myClass.setCreateDefaultConstructor(false);
            writeTestClass = false;

            if (enums.size() > 0) {
                myClass.addImport("java.util.Map");
                myClass.addImport("java.util.EnumMap");
                JavaVariable enumStringMapVar = myClass.addVariable("Map<" + enumClassname + ", String>", "enumStringMap",
                        "new EnumMap<" + enumClassname + ", String>(" + enumClassname + ".class)");
                enumStringMapVar.setStatic(true);

                myClass.addImport("java.util.List");
                myClass.addImport("java.util.ArrayList");
                JavaVariable stringListVar = myClass.addVariable("List<String>", "stringList", "new ArrayList<String>()");
                stringListVar.setStatic(true);

                Map<String, String> enumValues = table.getEnumValues();

                for (String enumItem : enums) {
                    String enumValue = enumValues.get(enumItem);
                    if (enumValue != null) {
                        myClass.appendStaticInitializer("enumStringMap.put(" + enumItem + ", \"" + enumValue + "\");");
                        myClass.appendStaticInitializer("stringList.add(\"" + enumValue + "\");");
                        myClass.appendStaticInitializer("");
                    }
                }

                List<JavaVariable> getStringMParam = new ArrayList<JavaVariable>();
                getStringMParam.add(new JavaVariable(enumClassname, "key"));
                JavaMethod getStringM = myClass.addMethod(Access.PUBLIC, "String", "getString", getStringMParam, "return enumStringMap.get(key);");
                getStringM.setStatic(true);

                myClass.addImport("java.util.Collections");
                JavaMethod getListM = myClass.addMethod(Access.PUBLIC, "List<String>", "getList", "return Collections.unmodifiableList(stringList);");
                getListM.setStatic(true);
            }
        } else {
            myClass = new JavaClass(packageName, className);
            myClass.addImport(packageName.substring(0, packageName.lastIndexOf('.')) + ".BaseRecord");
            myClass.setExtends("BaseRecord");

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
        myClass.addAnnotation("@SuppressWarnings(\"PMD\")");

        final String TAB = JavaClass.getTab();

        if (psLog == null) {
            psLog = System.out;
        }

        StringBuilder constructorElement = new StringBuilder();
        constructorElement.append("try {\n");

        boolean primaryKeyAdded = false;

        // constants and variables
        String databaseName = database.getName();
        myClass.addConstant("String", "DATABASE", databaseName);

        String tableName = table.getName();
        myClass.addConstant("String", "TABLE", tableName);
        myClass.addConstant("String", "FULL_TABLE", databaseName + "." + tableName);

        if (!myClass.isEnum()) {
            myClass.addMethod(Access.PUBLIC, "String", "getDatabaseName", "return DATABASE;").addAnnotation("Override");
            myClass.addMethod(Access.PUBLIC, "String", "getTableName", "return TABLE;").addAnnotation("Override");
        }

        // post field method content
        String contentValuesContent = "ContentValues values = new ContentValues();\n";
        String setContentValuesContent = "";
        String setContentCursorContent = "";

        List<SchemaField> fields = table.getFields();
        List<String> keys = new ArrayList<String>();
        for (SchemaField field : fields) {
            boolean primaryKey = field.isPrimaryKey();

            String fieldName = field.getName();

            // override default name
            String fieldNameJavaStyle = field.getName(true);

            // check for second primary key
            if (primaryKey && primaryKeyAdded) {
                throw new IllegalStateException("Cannot have more than 1 Primary Key [" + fieldNameJavaStyle + "]");
            } else {
                primaryKeyAdded = true;
            }

            // constants
            String constName = JavaClass.formatConstant(fieldNameJavaStyle);
            String fieldKey = "C_" + constName;
            keys.add(fieldKey);

            if (primaryKey) {
                myClass.addImport("android.provider.BaseColumns");
                myClass.addConstant("String", fieldKey, "BaseColumns._ID", false); // do not format the var because it is set to a static Class var
            } else {
                myClass.addConstant("String", fieldKey, fieldName);
            }

            myClass.addConstant("String", "FULL_C_" + constName, tableName + "." + fieldName);

            // skip some types of variables at this point (so that we still get the column name and the property name)
            switch (field.getForeignKeyType()) {
                case MANYTOONE:
                    generateManyToOne(database, packageName, field, table);
                    continue;
                case ONETOMANY:
                    generateOneToMany(table, field, packageName, database);
                    continue;
                case ONETOONE:
                    generateOneToOne(database, table, field, packageName);
                    continue;
                default:
            }

            createToStringMethodContent(field, fieldNameJavaStyle);

            // creates the variable OR changes the var to an enum
            JavaVariable newVariable = null;
            if (field.isEnumeration()) {
                newVariable = generateEnumeration(field, fieldNameJavaStyle, packageName, database);
            } else {
                newVariable = generateFieldVariable(fieldNameJavaStyle, field);
            }

            // Primary key / not enum methods
            if (primaryKey && !myClass.isEnum()) {
                myClass.addMethod(Access.PUBLIC, "String", "getRowIDKey", "return " + fieldKey + ";").addAnnotation("Override");

                // add vanilla getID() / setID(...) for the primary key
                myClass.addMethod(Access.PUBLIC, field.getJavaTypeText(), "getID", "return " + fieldNameJavaStyle + ";").addAnnotation("Override");

                List<JavaVariable> setIDParams = new ArrayList<JavaVariable>();
                setIDParams.add(new JavaVariable(newVariable.getDataType(), "id"));
                myClass.addMethod(Access.PUBLIC, "void", "setID", setIDParams, fieldNameJavaStyle + " = id;").addAnnotation("Override");
            }


            if (!myClass.isEnum()) {
                myClass.addVariable(newVariable);
            }

            // method values
            if (!(primaryKey && field.isIncrement())) {
                String value = fieldNameJavaStyle;
                if (field.isEnumeration()) {
                    value = newVariable.getName() + ".ordinal()";
                } else if (field.getJavaClassType() == Date.class) {
                    if (field.getJdbcType().equals("DATE")) {
                        String methodName = useDateTime ? "dateTimeToDBString" : "dateToDBString";
                        value = methodName + "(" + fieldNameJavaStyle + ")";
                    } else {
                        String getTimeMethod = useDateTime ? ".getMillis()" : ".getTime()";

                        value = fieldNameJavaStyle + " != null ? " + fieldNameJavaStyle + getTimeMethod + " : null";
                    }
                }
                contentValuesContent += "values.put(" + fieldKey + ", " + value + ");\n";

                setContentValuesContent += fieldNameJavaStyle + " = " + getContentValuesGetterMethod(field, fieldKey, newVariable) + ";\n";
            }

            setContentCursorContent += fieldNameJavaStyle + " = " + getContentValuesCursorGetterMethod(field, fieldKey, newVariable) + ";\n";
        }

        // SchemaDatabase variables

        String createTable = SqliteRenderer.generateTableSchema(table, sqlDataTypes);
        createTable = createTable.replace("\n", "\" + \n" + TAB + TAB + "\"");
        createTable = createTable.replace("\t", ""); // remove tabs
//        createTable = createTable.replace(";", ""); // remove last ;
        myClass.addConstant("String", "CREATE_TABLE", createTable);

        myClass.addConstant("String", "DROP_TABLE", SchemaRenderer.generateDropSchema(true, table));

        // Content values

        // All keys constant
        if (!myClass.isEnum()) {
            myClass.addImport("android.content.ContentValues");
            myClass.addImport("android.database.Cursor");

            String allKeysDefaultValue = "new String[] {\n";
            boolean hasKey = false;
            for (String key : keys) {
                if (hasKey) {
                    allKeysDefaultValue += ",\n";
                }
                allKeysDefaultValue += TAB + TAB + key;
                hasKey = true;
            }
            allKeysDefaultValue += "}";

//            JavaVariable allKeysVar = new JavaVariable("String[]", ALL_KEYS_VARNAME);
//            allKeysVar.setDefaultValue(allKeysDefaultValue);
//            allKeysVar.setAccess(Access.PROTECTED);
//            myClass.addConstant()
//            allKeysVar.setStatic(true);
//            myClass.addVariable(allKeysVar);
            JavaVariable allKeysVar = myClass.addConstant("String[]", ALL_KEYS_VARNAME, allKeysDefaultValue);
            allKeysVar.setAccess(Access.DEFAULT_NONE);
            myClass.addMethod(Access.PUBLIC, "String[]", "getAllKeys", "return " + ALL_KEYS_VARNAME + ".clone();").addAnnotation("Override");

            contentValuesContent += "return values;";
            myClass.addMethod(Access.PUBLIC, "ContentValues", "getContentValues", contentValuesContent).addAnnotation("Override");

            List<JavaVariable> setCValuesParams = new ArrayList<JavaVariable>();
            setCValuesParams.add(new JavaVariable("ContentValues", "values"));
            myClass.addMethod(Access.PUBLIC, "void", "setContent", setCValuesParams, setContentValuesContent);

            List<JavaVariable> setCCursorParams = new ArrayList<JavaVariable>();
            setCCursorParams.add(new JavaVariable("Cursor", "cursor"));
            myClass.addMethod(Access.PUBLIC, "void", "setContent", setCCursorParams, setContentCursorContent).addAnnotation("Override");
        }

        // methods
        addForgeignKeyData(database, table, packageName);

        // add method to cleanup many-to-one left-overs
        if (!myClass.isEnum()) {
            List<JavaVariable> orphanParams = new ArrayList<JavaVariable>();
            myClass.addMethod(Access.PROTECTED, "void", CLEANUP_ORPHANS_METHODNAME, orphanParams, cleanupOrphansContent.toString());

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

        // Enum classes need a cleanTable and createTable
        if (myClass.isEnum()) {
            // Remove to allow the change of the database
//            myClass.addImport("android.database.sqlite.SQLiteDatabase");
//            myClass.addImport(packageName.substring(0, packageName.lastIndexOf('.')) + ".BaseManager");
//
//            List<JavaVariable> tableParams = new ArrayList<JavaVariable>();
//            tableParams.add(new JavaVariable("SQLiteDatabase", "db"));

//            myClass.addMethod(Access.PUBLIC, "void", "cleanTable", tableParams, "BaseManager.executeSQL(db, DROP_TABLE);").setStatic(true);
//            myClass.addMethod(Access.PUBLIC, "void", "createTable", tableParams, "BaseManager.executeSQL(db, CREATE_TABLE);").setStatic(true);
        }

    }


    /**
     * For method setContent(ContentValues values).
     */
    private String getContentValuesGetterMethod(SchemaField field, String paramValue, JavaVariable newVariable) {
        if (field.isEnumeration()) {
            return newVariable.getDataType() + ".values()[values.getAsInteger(" + paramValue + ")]";
        }

        Class<?> type = field.getJavaClassType();
        if (type == int.class || type == Integer.class) {
            return "values.getAsInteger(" + paramValue + ")";
        } else if (type == String.class) {
            return "values.getAsString(" + paramValue + ")";
        } else if (type == long.class || type == Long.class) {
            return "values.getAsLong(" + paramValue + ")";
        } else if (type == boolean.class || type == Boolean.class) {
            return "values.getAsBoolean(" + paramValue + ")";
        } else if (type == Date.class) {
            if (field.getJdbcType().equals("DATE")) {
                if (useDateTime) {
                    return "dbStringToDateTime(values.getAsString(" + paramValue + "))";
                } else {
                    return "dbStringToDate(values.getAsString(" + paramValue + "))";
                }
            } else {
                if (useDateTime) {
                    return "new org.joda.time.DateTime(values.getAsLong(" + paramValue + "))";
                } else {
                    return "new java.util.Date(values.getAsLong(" + paramValue + "))";
                }
            }
        } else if (type == float.class || type == Float.class) { // || type == Fraction.class || type == Money.class) {
            return "values.getAsFloat(" + paramValue + ")";
        } else if (type == double.class || type == Double.class) {
            return "values.getAsDouble(" + paramValue + ")";
        } else {
            return "[[UNHANDLED FIELD TYPE: " + type + "]]";
        }
    }

    /**
     * For method setContent(Cursor cursor).
     *
     * @param field
     * @param paramValue
     * @param newVariable
     * @return
     */
    private String getContentValuesCursorGetterMethod(SchemaField field, String paramValue, JavaVariable newVariable) {
        if (field.isEnumeration()) {
            return newVariable.getDataType() + ".values()[cursor.getInt(cursor.getColumnIndex(" + paramValue + "))]";
        }

        Class<?> type = field.getJavaClassType();
        if (type == int.class || type == Integer.class) {
            return "cursor.getInt(cursor.getColumnIndex(" + paramValue + "))";
        } else if (type == String.class) {
            return "cursor.getString(cursor.getColumnIndex(" + paramValue + "))";
        } else if (type == long.class || type == Long.class) {
            return "cursor.getLong(cursor.getColumnIndex(" + paramValue + "))";
        } else if (type == boolean.class || type == Boolean.class) {
            return "cursor.getInt(cursor.getColumnIndex(" + paramValue + ")) != 0 ? true : false";
        } else if (type == Date.class) {
            if (field.getJdbcType().equals("DATE")) {
                if (useDateTime) {
                    return "dbStringToDateTime(cursor.getString(cursor.getColumnIndex(" + paramValue + ")))";
                } else {
                    return "dbStringToDate(cursor.getString(cursor.getColumnIndex(" + paramValue + ")))";
                }
            } else {
                if (useDateTime) {
                    return "!cursor.isNull(cursor.getColumnIndex(" + paramValue + ")) ? new org.joda.time.DateTime(cursor.getLong(cursor.getColumnIndex(" + paramValue + "))) : null";
                } else {
                    return "!cursor.isNull(cursor.getColumnIndex(" + paramValue + ")) ? new java.util.Date(cursor.getLong(cursor.getColumnIndex(" + paramValue + "))) : null";
                }
            }
        } else if (type == float.class || type == Float.class) { // || type == Fraction.class || type == Money.class) {
            return "cursor.getFloat(cursor.getColumnIndex(" + paramValue + "))";
        } else if (type == double.class || type == Double.class) {
            return "cursor.getDouble(cursor.getColumnIndex(" + paramValue + "))";
        } else {
            return "[[UNHANDLED FIELD TYPE: " + type + "]]";
        }
    }

    private void createToStringMethodContent(final SchemaField field, final String fieldNameJavaStyle) {

        String fieldType = field.getJdbcType();
        if (!fieldType.equals(SchemaField.TYPE_BLOB) && !fieldType.equals(SchemaField.TYPE_CLOB)) {
            // toString
            toStringContent.append("text += \"").append(fieldNameJavaStyle).append(" = \"+ ").append(fieldNameJavaStyle).append(" +\"\\n\";\n");
        }
    }

    private JavaVariable generateEnumeration(SchemaField field, String fieldNameJavaStyle, String packageName, SchemaDatabase database) {
        JavaVariable newVariable;
        if (field.isNumberDataType()) {
            if (field.getForeignKeyTable().length() > 0) {
                // define name of enum
                ClassInfo enumClassInfo = database.getTableClassInfo(field.getForeignKeyTable());
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
            }
        } else {
            newVariable = new JavaVariable(field.getJavaTypeText(), fieldNameJavaStyle);
        }

        return newVariable;
    }

    private JavaVariable generateFieldVariable(String fieldNameJavaStyle, SchemaField field) {
        JavaVariable newVariable = null;

        String typeText = field.getJavaTypeText();
        String defaultValue = field.getFormattedClassDefaultValue();

//        boolean fractionType = typeText.endsWith("Fraction");
//        boolean moneyType = typeText.endsWith("Money");
        boolean dateType = typeText.endsWith("Date");

        // Special handling for Fraction and Money
//        if (!field.isJavaTypePrimative() && (fractionType || moneyType)) {
//            // both Money and Fraction are both float at the core
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

    private void generateManyToOne(SchemaDatabase dbSchema, String packageName, SchemaField field, SchemaTable table) {
        String fkTableName = field.getForeignKeyTable();
        ClassInfo fkTableClassInfo = dbSchema.getTableClassInfo(fkTableName);
        String fkTableClassName = fkTableClassInfo.getClassName();
        String varName = field.getCustomVarName();
        if (varName.equals("")) {
            varName = JavaClass.formatToJavaVariable(fkTableClassName);
        }

        String newImport = fkTableClassInfo.getPackageName(packageName) + ".*";
        myClass.addImport(newImport);
        JavaVariable manyToOneVar = new JavaVariable(fkTableClassName, varName);

        myClass.addVariable(manyToOneVar, true);
    }

    private void generateOneToMany(SchemaTable table, SchemaField field, String packageName, SchemaDatabase database) {
        String fkTableName = field.getForeignKeyTable();
        ClassInfo fkTableClassInfo = database.getTableClassInfo(fkTableName);
        String fkTableClassName = fkTableClassInfo.getClassName();
        String varName = field.getCustomVarName();
        if (varName.equals("")) {
            varName = JavaClass.formatToJavaVariable(fkTableClassName);
        }

        String newImport = fkTableClassInfo.getPackageName(packageName) + ".*";
        myClass.addImport(newImport);
        JavaVariable manyToOneVar = new JavaVariable(fkTableClassName, varName);

        myClass.addVariable(manyToOneVar, true);
    }

    private void generateOneToOne(SchemaDatabase database, SchemaTable table, SchemaField field, String packageName) {
        String fkTableName = field.getForeignKeyTable();
        ClassInfo fkTableClassInfo = database.getTableClassInfo(fkTableName);
        String fkTableClassName = fkTableClassInfo.getClassName();
        String varName = field.getCustomVarName();
        if (varName.equals("")) {
            varName = JavaClass.formatToJavaVariable(fkTableClassName);
        }

        String newImport = fkTableClassInfo.getPackageName(packageName) + ".*";
        myClass.addImport(newImport);
        JavaVariable oneToOneVar = new JavaVariable(fkTableClassName, varName);

        myClass.addVariable(oneToOneVar, true);
    }

    private void addForgeignKeyData(SchemaDatabase database, SchemaTable table, String packageName) {
        String TAB = JavaClass.getTab();

        // find any other tables that depend on this one (MANYTOONE) or other tables this table depends on (ONETOONE)
        for (SchemaTable tmpTable : database.getTables()) {
            List<SchemaField> fkFields = tmpTable.getForeignKeyFields(table.getName());

            for (SchemaField fkField : fkFields) {
                switch (fkField.getForeignKeyType()) {
                    case ONETOMANY:
                        String fkTableName = tmpTable.getName();
                        ClassInfo fkTableClassInfo = database.getTableClassInfo(fkTableName);
                        String fkTableClassName = fkTableClassInfo.getClassName();
                        String fkTableVarName = JavaClass.formatToJavaVariable(fkTableClassName); // mealItem
                        String newImport = fkTableClassInfo.getPackageName(packageName) + ".*";

                        String items = fkTableVarName + "Items";
                        String itemsToDelete = fkTableVarName + "ItemsToDelete";

                        myClass.addImport(newImport);

                        myClass.addImport("java.util.Set");
                        myClass.addImport("java.util.HashSet");
                        String listType = "Set<" + fkTableClassName + ">";
                        String defaultListTypeValue = "new HashSet<" + fkTableClassName + ">()";
                        JavaVariable itemsList = myClass.addVariable(listType, items);
                        itemsList.setDefaultValue(defaultListTypeValue);

                        myClass.addMethod(Access.PUBLIC, listType, JavaVariable.getGetterMethodName(listType, items), "return java.util.Collections.unmodifiableSet(" + items + ");");

                        ClassInfo mappedByClassInfo = database.getTableClassInfo(fkField.getForeignKeyTable());
                        String mappedByVarName = JavaClass.formatToJavaVariable(mappedByClassInfo.getClassName());

                        // addItem method
                        JavaMethod addMethod = new JavaMethod("add" + fkTableClassName);
                        addMethod.setAccess(Access.PUBLIC);
                        addMethod.addParameter(new JavaVariable(fkTableClassName, fkTableVarName));
                        String addMethodContent = "";

                        ClassInfo myTableClassInfo = database.getTableClassInfo(fkField.getForeignKeyTable());
                        String tableClassName = myTableClassInfo.getClassName();


                        String fieldName = fkField.getCustomVarName();
                        if (fieldName == null || fieldName.length() == 0) {
                            fieldName = tableClassName;
                        }

                        String setterMethodName = "set" + fieldName.toUpperCase().charAt(0) + fieldName.substring(1, fieldName.length());

                        addMethodContent += fkTableVarName + "." + setterMethodName + "((" + tableClassName + ")this);\n";
                        addMethodContent += items + ".add(" + fkTableVarName + ");\n";
                        addMethod.setContent(addMethodContent);
                        myClass.addMethod(addMethod);

                        // deleteItem method
                        JavaVariable itemsToDeleteList = myClass.addVariable(listType, itemsToDelete);
                        itemsToDeleteList.setDefaultValue(defaultListTypeValue);

                        JavaMethod removeMethod = new JavaMethod("delete" + fkTableClassName);
                        removeMethod.setAccess(Access.PUBLIC);
                        removeMethod.addParameter(new JavaVariable(fkTableClassName, fkTableVarName));

                        String removeMethodContent = "";
                        removeMethodContent += "if (" + fkTableVarName + " == null) {\n";
                        removeMethodContent += TAB + "return;\n";
                        removeMethodContent += "}\n\n";
                        removeMethodContent += "java.util.Iterator<" + fkTableClassName + "> itr = " + items + ".iterator();\n";
                        removeMethodContent += "while (itr.hasNext()) {\n";
                        removeMethodContent += TAB + fkTableClassName + " item = itr.next();\n";
                        removeMethodContent += TAB + "if (item.equals(" + fkTableVarName + ")) {\n";
                        removeMethodContent += TAB + TAB + "itr.remove();\n";
                        removeMethodContent += TAB + TAB + itemsToDelete + ".add(item);\n";
                        removeMethodContent += TAB + TAB + "break;\n";
                        removeMethodContent += TAB + "}\n";
                        removeMethodContent += TAB + "if (!itr.hasNext()) {\n";
                        removeMethodContent += TAB + TAB + "throw new IllegalStateException(\"deleteItem failed: Cannot find itemID \"+ " + fkTableVarName + ".getID());\n";
                        removeMethodContent += TAB + "}\n";
                        removeMethodContent += "}";

                        removeMethod.setContent(removeMethodContent);
                        myClass.addMethod(removeMethod);

                        // add to cleanup orphans
                        cleanupOrphansContent.append("for (" + fkTableClassName + " itemToDelete : " + itemsToDelete + ") {\n");
                        cleanupOrphansContent.append(TAB + "try {\n");
                        cleanupOrphansContent.append(TAB + TAB + "em.remove(itemToDelete);\n");
                        cleanupOrphansContent.append(TAB + "} catch(RuntimeException e) {// do nothing... it is ok if it does not exist\n");
                        cleanupOrphansContent.append(TAB + "}\n");
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

    public String getFilename() {
        return myClass.getFilename();
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
        myTestClass.addImport("org.junit.*");
        myTestClass.addImport("static org.junit.Assert.*");

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
                testContent.append("testRecord." + newVariable.getSetterMethodName() + "(testData);\n");
                testContent.append("String recordData = testRecord." + newVariable.getGetterMethodName() + "();\n");
                testContent.append("assertEquals(testData, recordData);");
                break;
            case CHAR:
                testContent.append("char testData = 'z';\n");
                testContent.append("testRecord." + newVariable.getSetterMethodName() + "(testData);\n");
                testContent.append("char recordData = testRecord." + newVariable.getGetterMethodName() + "();\n");
                testContent.append("assertEquals(testData, recordData);");
                break;
            case BOOLEAN:
                testContent.append("boolean testData = false;\n");
                testContent.append("testRecord." + newVariable.getSetterMethodName() + "(testData);\n");
                testContent.append("boolean recordData = testRecord." + newVariable.getGetterMethodName() + "();\n");
                testContent.append("assertEquals(testData, recordData);");
                break;
            case INT:
                testContent.append("int testData = 123;\n");
                testContent.append("testRecord." + newVariable.getSetterMethodName() + "(testData);\n");
                testContent.append("int recordData = testRecord." + newVariable.getGetterMethodName() + "();\n");
                testContent.append("assertEquals(testData, recordData);");
                break;
            case FLOAT:
                testContent.append("float testData = 123.56f;\n");
                testContent.append("testRecord." + newVariable.getSetterMethodName() + "(testData);\n");
                testContent.append("float recordData = testRecord." + newVariable.getGetterMethodName() + "();\n");
                testContent.append("assertEquals(testData, recordData, 0);");
                break;
            case DOUBLE:
                testContent.append("double testData = 123.56;\n");
                testContent.append("testRecord." + newVariable.getSetterMethodName() + "(testData);\n");
                testContent.append("double recordData = testRecord." + newVariable.getGetterMethodName() + "();\n");
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
                testContent.append("testRecord." + newVariable.getSetterMethodName() + "(testData.getTime());\n");
                testContent.append("Date recordDataDate = testRecord." + newVariable.getGetterMethodName() + "();\n");
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

    public boolean isIncludeXML() {
        return includeXML;
    }

    public void setIncludeXML(boolean includeXML) {
        this.includeXML = includeXML;
    }

    public void setUseDateTime(boolean useDateTime) {
        this.useDateTime = useDateTime;
    }

    public boolean isUseInnerEnums() {
        return useInnerEnums;
    }

    public void setUseInnerEnums(boolean useInnerEnums) {
        this.useInnerEnums = useInnerEnums;
    }
}
