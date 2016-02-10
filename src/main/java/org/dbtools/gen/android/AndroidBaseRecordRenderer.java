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

import org.dbtools.codegen.java.*;
import org.dbtools.gen.GenConfig;
import org.dbtools.renderer.SchemaRenderer;
import org.dbtools.renderer.SqliteRenderer;
import org.dbtools.schema.ClassInfo;
import org.dbtools.schema.dbmappings.DatabaseMapping;
import org.dbtools.schema.schemafile.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author Jeff
 */
public class AndroidBaseRecordRenderer {
    private static final String TAB = JavaClass.getTab();
    private static final String CLEANUP_ORPHANS_METHOD_NAME = "cleanupOrphans";
    private static final String ALL_COLUMNS_VAR_NAME = "ALL_COLUMNS";
    private static final String ALL_COLUMNS_FULL_VAR_NAME = "ALL_COLUMNS_FULL";
    public static final String PRIMARY_KEY_COLUMN = "PRIMARY_KEY_COLUMN";

    private JavaClass constClass;
    private JavaClass recordClass;
    private List<JavaEnum> enumerationClasses = new ArrayList<>();
    private StringBuilder cleanupOrphansContent;
    private boolean useInnerEnums = true;
    private GenConfig genConfig;

    /**
     * Creates a new instance of AndroidBaseRecordRenderer.
     */
    public AndroidBaseRecordRenderer() {
    }

    public void generate(SchemaDatabase database, SchemaEntity entity, String packageName, DatabaseMapping databaseMapping) {
        boolean enumTable = entity.isEnumerationTable();
        SchemaEntityType entityType = entity.getType();
        String entityClassName = entity.getClassName();

        String className = createClassName(enumTable, entityClassName);

        String constClassName = entityClassName + "Const";
        constClass = new JavaClass(packageName, constClassName);

        if (enumTable) {
            initClassAsEnum(packageName, className, entity);
        } else {
            recordClass = new JavaClass(packageName, className);
            recordClass.setAbstract(true);

            recordClass.addImport("org.dbtools.android.domain.AndroidBaseRecord");
            recordClass.setExtends("AndroidBaseRecord");
        }

        // prep
        cleanupOrphansContent = new StringBuilder();

        // header comment
        addHeader(constClass, className);
        addHeader(recordClass, className);

        boolean primaryKeyAdded = false;

        // constants and variables
        String databaseName = database.getName();
        constClass.addConstant("String", "DATABASE", databaseName);

        String tableName = entity.getName();
        if (entityType != SchemaEntityType.QUERY) {
            constClass.addConstant("String", "TABLE", tableName);
            constClass.addConstant("String", "FULL_TABLE", databaseName + "." + tableName);
        }

        // post field method content
        StringBuilder contentValuesContent = new StringBuilder("ContentValues values = new ContentValues();\n");
        StringBuilder valuesContent = new StringBuilder("Object[] values = new Object[]{\n");
        String setContentValuesContent = "";
        String setContentCursorContent = "";

        List<String> columns = new ArrayList<>();
        for (SchemaField field : entity.getFields()) {
            boolean primaryKey = field.isPrimaryKey();

            String fieldName = field.getName();

            // override default name
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
            String fieldColumn = "C_" + constName;
            String fullFieldColumn = constClassName + ".C_" + constName;
            columns.add(fieldColumn);

            if (primaryKey) {
                constClass.addConstant("String", PRIMARY_KEY_COLUMN, fieldName); // add a reference to this column
            }

            constClass.addConstant("String", fieldColumn, fieldName);
            constClass.addConstant("String", "FULL_C_" + constName, tableName + "." + fieldName);

            // skip some types of variables at this point (so that we still get the column name and the property name)
            switch (field.getForeignKeyType()) {
                case MANYTOONE:
                    generateManyToOne(database, packageName, field);
                    continue;
                case ONETOMANY:
                    generateOneToMany(database, packageName, field);
                    continue;
                case ONETOONE:
                    generateOneToOne(database, packageName, field);
                    continue;
                default:
            }

            // creates the variable OR changes the var to an enum

            JavaVariable newVariable;
            if (field.isEnumeration()) {
                newVariable = generateEnumeration(entity.isReadonly(), field, fieldNameJavaStyle, packageName, database);
            } else {
                newVariable = generateFieldVariable(entity.isReadonly(), fieldNameJavaStyle, field);
            }

            // Primary key / not enum methods
            if (primaryKey && !recordClass.isEnum()) {
                recordClass.addMethod(Access.PUBLIC, "String", "getIdColumnName", "return " + fullFieldColumn + ";").addAnnotation("Override");

                // add vanilla getPrimaryKeyId() / setPrimaryKeyId(...) for the primary key
                recordClass.addMethod(Access.PUBLIC, field.getJavaTypeText(), "getPrimaryKeyId", "return " + fieldNameJavaStyle + ";").addAnnotation("Override");

                List<JavaVariable> setIdParams = new ArrayList<>();
                setIdParams.add(new JavaVariable(newVariable.getDataType(), "id"));
                recordClass.addMethod(Access.PUBLIC, "void", "setPrimaryKeyId", setIdParams, "this." + fieldNameJavaStyle + " = id;").addAnnotation("Override");
            }

            if (!recordClass.isEnum()) {
                recordClass.addVariable(newVariable);
            }

            // method values
            if (!(primaryKey && field.isIncrement())) {
                String value = fieldNameJavaStyle;
                SchemaFieldType fieldType = field.getJdbcDataType();
                if (field.isEnumeration()) {
                    value = newVariable.getName() + ".ordinal()";
                } else if (fieldType == SchemaFieldType.DATE || fieldType == SchemaFieldType.TIMESTAMP || fieldType == SchemaFieldType.TIME) {
                    String getTimeMethod = genConfig.isDateTimeSupport() ? ".getMillis()" : ".getTime()";
                    value = fieldNameJavaStyle + " != null ? " + fieldNameJavaStyle + getTimeMethod + " : null";
                } else if (fieldType == SchemaFieldType.BOOLEAN) {
                    if (field.isNotNull()) {
                        value = fieldNameJavaStyle + " ? 1 : 0";
                    } else {
                        value = fieldNameJavaStyle + " != null ? (" + fieldNameJavaStyle + " ? 1 : 0) : 0";
                    }
                }
                contentValuesContent.append("values.put(").append(fullFieldColumn).append(", ").append(value).append(");\n");
                valuesContent.append(TAB).append(value).append(",\n");

                setContentValuesContent += fieldNameJavaStyle + " = " + getContentValuesGetterMethod(field, fullFieldColumn, newVariable) + ";\n";
            } else {
                // id column
                valuesContent.append(TAB).append(fieldNameJavaStyle).append(",\n");
            }

            setContentCursorContent += fieldNameJavaStyle + " = " + getContentValuesCursorGetterMethod(field, fullFieldColumn, newVariable) + ";\n";

            // static getter method that takes a Cursor parameter
            constClass.addImport("android.database.Cursor");
            JavaMethod cursorGetter = constClass.addMethod(Access.PUBLIC, newVariable.getDataType(), newVariable.getGetterMethodName(), "return " + getContentValuesCursorGetterMethod(field, fieldColumn, newVariable) + ";");
            cursorGetter.setStatic(true);
            cursorGetter.setParameters(Arrays.asList(new JavaVariable("Cursor", "cursor")));
        }

        if (!primaryKeyAdded && (entityType == SchemaEntityType.VIEW || entityType == SchemaEntityType.QUERY)) {
            recordClass.addMethod(Access.PUBLIC, "String", "getIdColumnName", "return null;").addAnnotation("Override");

            // add vanilla getPrimaryKeyId() / setPrimaryKeyId() for the primary key
            recordClass.addMethod(Access.PUBLIC, "long", "getPrimaryKeyId", "return 0;").addAnnotation("Override");
            recordClass.addMethod(Access.PUBLIC, "void", "setPrimaryKeyId", Arrays.asList(new JavaVariable("long", "id")), "").addAnnotation("Override");
        }

        // SchemaDatabase variables
        if (entityType == SchemaEntityType.TABLE) {
            SchemaTable table = (SchemaTable) entity;
            String createTable = SqliteRenderer.generateTableSchema(table, databaseMapping);
            createTable = createTable.replace("\n", "\" + \n" + TAB + TAB + "\"");
            createTable = createTable.replace("\t", ""); // remove tabs
            constClass.addConstant("String", "CREATE_TABLE", createTable);
            constClass.addConstant("String", "DROP_TABLE", SchemaRenderer.generateDropSchema(true, table));
        }

        // Content values

        // All keys constant
        if (!recordClass.isEnum()) {
            recordClass.addImport("android.content.ContentValues");
            recordClass.addImport("android.database.Cursor");


            // columns
            String allColumnsDefaultValue = "new String[] {\n";
            String allColumnsFullDefaultValue = "new String[] {\n";
            boolean hasColumn = false;
            for (String column : columns) {
                if (hasColumn) {
                    allColumnsDefaultValue += ",\n";
                    allColumnsFullDefaultValue += ",\n";
                }
                allColumnsDefaultValue += TAB + TAB + column;
                allColumnsFullDefaultValue += TAB + TAB + "FULL_" + column;
                hasColumn = true;
            }
            allColumnsDefaultValue += "}";
            allColumnsFullDefaultValue += "}";

            // columns
            JavaVariable allColumnsVar = constClass.addConstant("String[]", ALL_COLUMNS_VAR_NAME, allColumnsDefaultValue);
            allColumnsVar.setAccess(Access.PUBLIC);
            recordClass.addMethod(Access.PUBLIC, "String[]", "getAllColumns", "return " + constClassName + "." + ALL_COLUMNS_VAR_NAME + ".clone();").addAnnotation("Override");

            // columns full
            JavaVariable allColumnsFullVar = constClass.addConstant("String[]", ALL_COLUMNS_FULL_VAR_NAME, allColumnsFullDefaultValue);
            allColumnsFullVar.setAccess(Access.PUBLIC);
            recordClass.addMethod(Access.PUBLIC, "String[]", "getAllColumnsFull", "return " + constClassName + "." + ALL_COLUMNS_FULL_VAR_NAME + ".clone();");

            contentValuesContent.append("return values;");
            recordClass.addMethod(Access.PUBLIC, "ContentValues", "getContentValues", contentValuesContent.toString()).addAnnotation("Override");

            valuesContent.append("};\n");
            valuesContent.append("return values;");
            recordClass.addMethod(Access.PUBLIC, "Object[]", "getValues", valuesContent.toString()).addAnnotation("Override");

            List<JavaVariable> setCValuesParams = new ArrayList<>();
            setCValuesParams.add(new JavaVariable("ContentValues", "values"));
            recordClass.addMethod(Access.PUBLIC, "void", "setContent", setCValuesParams, setContentValuesContent);

            List<JavaVariable> setCCursorParams = new ArrayList<>();
            setCCursorParams.add(new JavaVariable("Cursor", "cursor"));
            recordClass.addMethod(Access.PUBLIC, "void", "setContent", setCCursorParams, setContentCursorContent).addAnnotation("Override");
        }

        // methods
        addForeignKeyData(database, entity.getName(), packageName);

        // add method to cleanup many-to-one left-overs
        if (!recordClass.isEnum()) {
            List<JavaVariable> orphanParams = new ArrayList<>();

            if (cleanupOrphansContent.length() > 0) {
                recordClass.addMethod(Access.PROTECTED, "void", CLEANUP_ORPHANS_METHOD_NAME, orphanParams, cleanupOrphansContent.toString());
            }

            // new record check
            recordClass.addMethod(Access.PUBLIC, "boolean", "isNewRecord", "return getPrimaryKeyId() <= 0;");
        }
    }

    private void addHeader(JavaClass someClass, String className) {
        // Do not place date in file because it will cause a new check-in to scm
        String fileHeaderComment;
        fileHeaderComment = "/*\n";
        fileHeaderComment += " * " + className + ".java\n";
        fileHeaderComment += " *\n";
        fileHeaderComment += " * GENERATED FILE - DO NOT EDIT\n";
        fileHeaderComment += " * CHECKSTYLE:OFF\n";
        fileHeaderComment += " * \n";
        fileHeaderComment += " */\n";
        someClass.setFileHeaderComment(fileHeaderComment);

        // Since this is generated code.... suppress all warnings
        someClass.addAnnotation("@SuppressWarnings(\"all\")");
    }

    private void initClassAsEnum(String packageName, String enumClassName, SchemaEntity entity) {
        if (entity.getType() != SchemaEntityType.TABLE) {
            return;
        }

        SchemaTable table = (SchemaTable) entity;
        List<TableEnum> enums = table.getTableEnums();

        recordClass = new JavaEnum(packageName, enumClassName, table.getTableEnumsText());
        recordClass.setCreateDefaultConstructor(false);

        if (enums.size() > 0) {
            recordClass.addImport("java.util.Map");
            recordClass.addImport("java.util.EnumMap");
            JavaVariable enumStringMapVar = recordClass.addVariable("Map<" + enumClassName + ", String>", "enumStringMap",
                    "new EnumMap<" + enumClassName + ", String>(" + enumClassName + ".class)");
            enumStringMapVar.setStatic(true);

            recordClass.addImport("java.util.List");
            recordClass.addImport("java.util.ArrayList");
            JavaVariable stringListVar = recordClass.addVariable("List<String>", "stringList", "new ArrayList<String>()");
            stringListVar.setStatic(true);

            for (TableEnum enumItem : enums) {
                recordClass.appendStaticInitializer("enumStringMap.put(" + enumItem.getName() + ", \"" + enumItem.getValue() + "\");");
                recordClass.appendStaticInitializer("stringList.add(\"" + enumItem.getValue() + "\");");
                recordClass.appendStaticInitializer("");
            }

            List<JavaVariable> getStringMParam = new ArrayList<>();
            getStringMParam.add(new JavaVariable(enumClassName, "key"));
            JavaMethod getStringM = recordClass.addMethod(Access.PUBLIC, "String", "getString", getStringMParam, "return enumStringMap.get(key);");
            getStringM.setStatic(true);

            recordClass.addImport("java.util.Collections");
            JavaMethod getListM = recordClass.addMethod(Access.PUBLIC, "List<String>", "getList", "return Collections.unmodifiableList(stringList);");
            getListM.setStatic(true);
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
            SchemaFieldType fieldType = field.getJdbcDataType();
            if (fieldType == SchemaFieldType.DATE) {
                if (genConfig.isDateTimeSupport()) {
                    return "dbStringToDateTime(values.getAsString(" + paramValue + "))";
                } else {
                    return "dbStringToDate(values.getAsString(" + paramValue + "))";
                }
            } else {
                if (genConfig.isDateTimeSupport()) {
                    return "new org.joda.time.DateTime(values.getAsLong(" + paramValue + "))";
                } else {
                    return "new java.util.Date(values.getAsLong(" + paramValue + "))";
                }
            }
        } else if (type == float.class || type == Float.class) { // || type == Fraction.class || type == Money.class) {
            return "values.getAsFloat(" + paramValue + ")";
        } else if (type == double.class || type == Double.class) {
            return "values.getAsDouble(" + paramValue + ")";
        } else if (type == byte[].class || type == Byte[].class) {
            return "values.getAsByteArray(" + paramValue + ")";
        } else {
            return "[[UNHANDLED FIELD TYPE: " + type + "]]";
        }
    }

    /**
     * For method setContent(Cursor cursor).
     */
    private String getContentValuesCursorGetterMethod(SchemaField field, String paramValue, JavaVariable newVariable) {
        if (field.isEnumeration()) {
            return newVariable.getDataType() + ".values()[cursor.getInt(cursor.getColumnIndexOrThrow(" + paramValue + "))]";
        }

        Class<?> type = field.getJavaClassType();
        if (type == int.class) {
            return "cursor.getInt(cursor.getColumnIndexOrThrow(" + paramValue + "))";
        } else if (type == Integer.class) {
            return "!cursor.isNull(cursor.getColumnIndexOrThrow(" + paramValue + ")) ? cursor.getInt(cursor.getColumnIndexOrThrow(" + paramValue + ")) : null";
        } else if (type == String.class && field.isNotNull()) {
            return "cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + "))";
        } else if (type == String.class) {
            return "!cursor.isNull(cursor.getColumnIndexOrThrow(" + paramValue + ")) ? cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")) : null";
        } else if (type == long.class) {
            return "cursor.getLong(cursor.getColumnIndexOrThrow(" + paramValue + "))";
        } else if (type == Long.class) {
            return "!cursor.isNull(cursor.getColumnIndexOrThrow(" + paramValue + ")) ? cursor.getLong(cursor.getColumnIndexOrThrow(" + paramValue + ")) : null";
        } else if (type == boolean.class) {
            return "cursor.getInt(cursor.getColumnIndexOrThrow(" + paramValue + ")) != 0 ? true : false";
        } else if (type == Boolean.class) {
            return "!cursor.isNull(cursor.getColumnIndexOrThrow(" + paramValue + ")) ? cursor.getInt(cursor.getColumnIndexOrThrow(" + paramValue + ")) != 0 ? true : false : null";
        } else if (type == Date.class) {
            SchemaFieldType fieldType = field.getJdbcDataType();
            if (fieldType == SchemaFieldType.DATE) {
                if (genConfig.isDateTimeSupport()) {
                    return "dbStringToDateTime(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))";
                } else {
                    return "dbStringToDate(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))";
                }
            } else {
                if (genConfig.isDateTimeSupport()) {
                    return "!cursor.isNull(cursor.getColumnIndexOrThrow(" + paramValue + ")) ? new org.joda.time.DateTime(cursor.getLong(cursor.getColumnIndexOrThrow(" + paramValue + "))) : null";
                } else {
                    return "!cursor.isNull(cursor.getColumnIndexOrThrow(" + paramValue + ")) ? new java.util.Date(cursor.getLong(cursor.getColumnIndexOrThrow(" + paramValue + "))) : null";
                }
            }
        } else if (type == float.class) { // || type == Fraction.class || type == Money.class) {
            return "cursor.getFloat(cursor.getColumnIndexOrThrow(" + paramValue + "))";
        } else if (type == Float.class) {
            return "!cursor.isNull(cursor.getColumnIndexOrThrow(" + paramValue + ")) ? cursor.getFloat(cursor.getColumnIndexOrThrow(" + paramValue + ")) : null";
        } else if (type == double.class || type == Double.class) {
            return "cursor.getDouble(cursor.getColumnIndexOrThrow(" + paramValue + "))";
        } else if (type == Double.class) {
            return "!cursor.isNull(cursor.getColumnIndexOrThrow(" + paramValue + ")) ? cursor.getDouble(cursor.getColumnIndexOrThrow(" + paramValue + ")) : null";
        } else if (type == byte[].class || type == Byte[].class) {
            return "cursor.getBlob(cursor.getColumnIndexOrThrow(" + paramValue + "))";
        } else {
            return "[[UNHANDLED FIELD TYPE: " + type + "]]";
        }
    }

    private JavaVariable generateEnumeration(boolean readOnlyEntity, SchemaField field, String fieldNameJavaStyle, String packageName, SchemaDatabase database) {
        JavaVariable newVariable;
        if (field.getJdbcDataType().isNumberDataType()) {
            if (!field.getForeignKeyTable().isEmpty()) {
                // define name of enum
                ClassInfo enumClassInfo = database.getTableClassInfo(field.getForeignKeyTable());
                String enumName = enumClassInfo.getClassName();

                // local definition of enumeration?
                List<String> localEnumerations = field.getEnumValues();
                if (localEnumerations != null && localEnumerations.size() > 0) {
                    recordClass.addEnum(enumName, field.getEnumValues());
                } else {
                    // we must import the enum
                    String enumPackage = enumClassInfo.getPackageName(packageName) + "." + enumName;

                    // build foreign key packagename
//                    String[] packageElements = packageName.split("\\.");
//                    for (int i = 0; i < packageElements.length - 1; i++) {
//                        enumPackage += packageElements[i] + ".";
//                    }
//                    enumPackage += enumName.toLowerCase() + "." + enumName;


                    recordClass.addImport(enumPackage);
                    constClass.addImport(enumPackage);
                }

                newVariable = new JavaVariable(enumName, fieldNameJavaStyle);

                newVariable.setGenerateGetter(true, field.isNotNull(), genConfig.isJsr305Support());
                if (!readOnlyEntity) {
                    newVariable.setGenerateSetter(true, field.isNotNull(), genConfig.isJsr305Support());
                }

                newVariable.setDefaultValue(enumName + "." + field.getEnumerationDefault(), false);
            } else if (!field.getEnumerationClass().isEmpty()) {
                // use user defined class
                String enumClassName = field.getEnumerationClass();

                newVariable = new JavaVariable(enumClassName, fieldNameJavaStyle);

                newVariable.setGenerateGetter(true, field.isNotNull(), genConfig.isJsr305Support());
                if (!readOnlyEntity) {
                    newVariable.setGenerateSetter(true, field.isNotNull(), genConfig.isJsr305Support());
                }

                newVariable.setDefaultValue(enumClassName + "." + field.getEnumerationDefault(), false);
            } else {
                // ENUM without a foreign key table
                String javaStyleFieldName = field.getName(true);
                String firstChar = javaStyleFieldName.substring(0, 1).toUpperCase();
                String enumName = firstChar + javaStyleFieldName.substring(1);

                if (useInnerEnums) {
                    recordClass.addEnum(enumName, field.getEnumValues());
                } else {
                    enumerationClasses.add(new JavaEnum(enumName, field.getEnumValues()));
                }

                newVariable = new JavaVariable(enumName, fieldNameJavaStyle);

                Class varClass = field.getJavaClassType();
                boolean jsr305SupportedField = !varClass.isPrimitive() || varClass.isEnum();

                newVariable.setGenerateGetter(true, field.isNotNull(), jsr305SupportedField && genConfig.isJsr305Support());
                if (!readOnlyEntity) {
                    newVariable.setGenerateSetter(true, field.isNotNull(), jsr305SupportedField && genConfig.isJsr305Support());
                }
                newVariable.setDefaultValue(enumName + "." + field.getEnumerationDefault(), false);
            }
        } else {
            newVariable = new JavaVariable(field.getJavaTypeText(), fieldNameJavaStyle);
        }

        return newVariable;
    }

    private JavaVariable generateFieldVariable(boolean readOnlyEntity, String fieldNameJavaStyle, SchemaField field) {
        JavaVariable newVariable;

        String typeText = field.getJavaTypeText();
        String defaultValue = field.getFormattedClassDefaultValue();

//        boolean fractionType = typeText.endsWith("Fraction");
//        boolean moneyType = typeText.endsWith("Money");
        boolean dateType = typeText.endsWith("Date");

        // Special handling for Fraction and Money
//        if (!field.isJavaTypePrimitive() && (fractionType || moneyType)) {
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
        if (dateType && genConfig.isDateTimeSupport()) {
            newVariable = new JavaVariable("org.joda.time.DateTime", fieldNameJavaStyle);
        } else {
            newVariable = new JavaVariable(typeText, fieldNameJavaStyle);
        }

        SchemaFieldType fieldType = field.getJdbcDataType();
        boolean immutableDate = field.getJavaClassType() == Date.class && genConfig.isDateTimeSupport(); // org.joda.time.DateTime IS immutable
        if (!fieldType.isJavaTypePrimitive() && !fieldType.isJavaTypeImmutable() && !immutableDate) {
            newVariable.setGetterReturnsClone(true);
            if (!readOnlyEntity) {
                newVariable.setSetterClonesParam(true);
            }
        }

        Class varClass = field.getJavaClassType();
        boolean jsr305SupportedField = !varClass.isPrimitive() || varClass.isEnum();

        newVariable.setGenerateGetter(true, field.isNotNull(), jsr305SupportedField && genConfig.isJsr305Support());
        if (!readOnlyEntity) {
            newVariable.setGenerateSetter(true, field.isNotNull(), jsr305SupportedField && genConfig.isJsr305Support());
        }

        newVariable.setDefaultValue(defaultValue);

        return newVariable;
    }

    private void generateManyToOne(SchemaDatabase dbSchema, String packageName, SchemaField field) {
        String fkTableName = field.getForeignKeyTable();
        ClassInfo fkTableClassInfo = dbSchema.getTableClassInfo(fkTableName);
        String fkTableClassName = fkTableClassInfo.getClassName();
        String varName = field.getVarName();
        if (varName.equals("")) {
            varName = JavaClass.formatToJavaVariable(fkTableClassName);
        }

        String newImport = fkTableClassInfo.getPackageName(packageName) + ".*";
        recordClass.addImport(newImport);
        JavaVariable manyToOneVar = new JavaVariable(fkTableClassName, varName);

        recordClass.addVariable(manyToOneVar, true);
    }

    private void generateOneToMany(SchemaDatabase database, String packageName, SchemaField field) {
        String fkTableName = field.getForeignKeyTable();
        ClassInfo fkTableClassInfo = database.getTableClassInfo(fkTableName);
        String fkTableClassName = fkTableClassInfo.getClassName();
        String varName = field.getVarName();
        if (varName.equals("")) {
            varName = JavaClass.formatToJavaVariable(fkTableClassName);
        }

        String newImport = fkTableClassInfo.getPackageName(packageName) + ".*";
        recordClass.addImport(newImport);
        JavaVariable manyToOneVar = new JavaVariable(fkTableClassName, varName);

        recordClass.addVariable(manyToOneVar, true);
    }

    private void generateOneToOne(SchemaDatabase database, String packageName, SchemaField field) {
        String fkTableName = field.getForeignKeyTable();
        ClassInfo fkTableClassInfo = database.getTableClassInfo(fkTableName);
        String fkTableClassName = fkTableClassInfo.getClassName();
        String varName = field.getVarName();
        if (varName.equals("")) {
            varName = JavaClass.formatToJavaVariable(fkTableClassName);
        }

        String newImport = fkTableClassInfo.getPackageName(packageName) + ".*";
        recordClass.addImport(newImport);
        JavaVariable oneToOneVar = new JavaVariable(fkTableClassName, varName);

        recordClass.addVariable(oneToOneVar, true);
    }

    private void addForeignKeyData(SchemaDatabase database, String entityName, String packageName) {
        String TAB = JavaClass.getTab();

        // find any other tables that depend on this one (MANYTOONE) or other tables this table depends on (ONETOONE)
        for (SchemaTable tmpTable : database.getTables()) {
            List<SchemaTableField> fkFields = tmpTable.getForeignKeyFields(entityName);

            for (SchemaTableField fkField : fkFields) {
                switch (fkField.getForeignKeyType()) {
                    case ONETOMANY:
                        String fkTableName = tmpTable.getName();
                        ClassInfo fkTableClassInfo = database.getTableClassInfo(fkTableName);
                        String fkTableClassName = fkTableClassInfo.getClassName();
                        String newImport = fkTableClassInfo.getPackageName(packageName) + ".*";

                        String customVarName = fkField.getVarName();
                        String listVarName = customVarName != null && !customVarName.isEmpty() ?
                                JavaClass.formatToJavaVariable(customVarName, entityName) :
                                JavaClass.formatToJavaVariable(fkTableClassName);

                        String items = listVarName + "Items";
                        String itemsToDelete = listVarName + "ItemsToDelete";

                        recordClass.addImport(newImport);

                        recordClass.addImport("java.util.Set");
                        recordClass.addImport("java.util.HashSet");
                        String listType = "Set<" + fkTableClassName + ">";
                        String defaultListTypeValue = "new HashSet<" + fkTableClassName + ">()";
                        JavaVariable itemsList = recordClass.addVariable(listType, items);
                        itemsList.setDefaultValue(defaultListTypeValue);

                        recordClass.addMethod(Access.PUBLIC, listType, JavaVariable.getGetterMethodName(listType, items), "return java.util.Collections.unmodifiableSet(" + items + ");");

                        ClassInfo mappedByClassInfo = database.getTableClassInfo(fkField.getForeignKeyTable());
                        JavaClass.formatToJavaVariable(mappedByClassInfo.getClassName());

                        // addItem method
                        JavaMethod addMethod = new JavaMethod("add" + fkTableClassName);
                        addMethod.setAccess(Access.PUBLIC);
                        addMethod.addParameter(new JavaVariable(fkTableClassName, listVarName));
                        String addMethodContent = "";

                        ClassInfo myTableClassInfo = database.getTableClassInfo(fkField.getForeignKeyTable());
                        String tableClassName = myTableClassInfo.getClassName();


                        String fieldName = fkField.getVarName();
                        if (fieldName == null || fieldName.length() == 0) {
                            fieldName = tableClassName;
                        }

                        String setterMethodName = "set" + fieldName.toUpperCase().charAt(0) + fieldName.substring(1, fieldName.length());

                        addMethodContent += listVarName + "." + setterMethodName + "((" + tableClassName + ")this);\n";
                        addMethodContent += items + ".add(" + listVarName + ");\n";
                        addMethod.setContent(addMethodContent);
                        recordClass.addMethod(addMethod);

                        // deleteItem method
                        JavaVariable itemsToDeleteList = recordClass.addVariable(listType, itemsToDelete);
                        itemsToDeleteList.setDefaultValue(defaultListTypeValue);

                        JavaMethod removeMethod = new JavaMethod("delete" + fkTableClassName);
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
                        removeMethodContent += TAB + TAB + "throw new IllegalStateException(\"deleteItem failed: Cannot find itemId \"+ " + listVarName + ".getPrimaryKeyId());\n";
                        removeMethodContent += TAB + "}\n";
                        removeMethodContent += "}";

                        removeMethod.setContent(removeMethodContent);
                        recordClass.addMethod(removeMethod);

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

    public static String createClassName(boolean enumTable, String className) {
        return enumTable ? className : className + "BaseRecord";
    }

    public void writeToFile(String directoryName) {
        constClass.writeToDisk(directoryName);
        recordClass.writeToDisk(directoryName);

        for (JavaEnum enumClass : enumerationClasses) {
            enumClass.writeToDisk(directoryName);
        }
    }

    public void setGenConfig(GenConfig genConfig) {
        this.genConfig = genConfig;
    }
}
