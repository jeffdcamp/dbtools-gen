/*
 * KotlinAndroidBaseRecordClassRenderer.kt
 *
 * Created on Sep 9, 2010
 *
 * Copyright 2010 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.gen.android.kotlin

import org.dbtools.codegen.java.JavaClass
import org.dbtools.codegen.java.JavaVariable
import org.dbtools.codegen.kotlin.*
import org.dbtools.gen.GenConfig
import org.dbtools.renderer.SchemaRenderer
import org.dbtools.renderer.SqliteRenderer
import org.dbtools.schema.dbmappings.DatabaseMapping
import org.dbtools.schema.schemafile.*
import java.util.*

class KotlinAndroidBaseRecordRenderer(val genConfig: GenConfig) {

    private var constClass = KotlinObjectClass()
    private var recordClass = KotlinClass()
    private val enumerationClasses = ArrayList<KotlinEnum>()
    private val cleanupOrphansContent = StringBuilder()
    private val useInnerEnums = true

    fun generate(database: SchemaDatabase, entity: SchemaEntity, packageName: String, databaseMapping: DatabaseMapping) {
        val enumTable = entity.isEnumerationTable
        val entityType = entity.type
        val entityClassName = entity.className

        val className = createClassName(enumTable, entityClassName)

        val constClassName = entityClassName + "Const"
        constClass = KotlinObjectClass(constClassName, packageName)

        if (enumTable) {
            initClassAsEnum(packageName, className, entity)
        } else {
            recordClass = KotlinClass(className, packageName).apply {
                abstract = true
                addImport("org.dbtools.android.domain.AndroidBaseRecord")
                extends = "AndroidBaseRecord()"
            }
        }

        // header comment
        addHeader(constClass, className)
        addHeader(recordClass, className)

        var primaryKeyAdded = false

        // constants and variables
        val databaseName = database.name
        constClass.addConstant("DATABASE", "\"$databaseName\"").apply { const = true }

        val tableName = entity.name
        if (entityType != SchemaEntityType.QUERY) {
            constClass.addConstant("TABLE", "\"$tableName\"").apply { const = true }
            constClass.addConstant("FULL_TABLE", "\"$databaseName.$tableName\"").apply { const = true }
        }

        // post field method content
        val contentValuesContent = StringBuilder("val values = ContentValues()\n")
        val valuesContent = StringBuilder("return arrayOf(\n")
        var valuesContentItemCount = 0;
        var setContentValuesContent = ""
        var setContentCursorContent = ""

        val columns = ArrayList<String>()
        for (field in entity.fields) {
            val primaryKey = field.isPrimaryKey
            val fieldName = field.name

            // override default name
            val fieldNameJavaStyle = field.getName(true)

            // check for second primary key
            if (primaryKey) {
                if (primaryKeyAdded) {
                    throw IllegalStateException("Cannot have more than 1 Primary Key [$fieldNameJavaStyle]")
                } else {
                    primaryKeyAdded = true
                }
            }

            // constants
            val constName = KotlinClass.formatConstant(fieldNameJavaStyle)
            val fieldColumn = "C_$constName"
            val fullFieldColumn = "$constClassName.C_$constName"
            columns.add(fieldColumn)

            if (primaryKey) {
                constClass.addConstant(PRIMARY_KEY_COLUMN, "\"$fieldName\"").apply { const = true } // add a reference to this column
            }

            constClass.addConstant(fieldColumn, "\"$fieldName\"").apply { const = true }
            constClass.addConstant("FULL_C_$constName", "\"$tableName.$fieldName\"").apply { const = true }

            // creates the variable OR changes the var to an enum

            val newVariable: KotlinVar
            if (field.isEnumeration) {
                newVariable = generateEnumeration(entity.isReadonly, field, fieldNameJavaStyle, packageName, database)
            } else {
                newVariable = generateFieldVariable(fieldNameJavaStyle, field)
            }

            // Primary key / not enum methods
            if (primaryKey && !recordClass.isEnum()) {
                recordClass.addFun("getIdColumnName", "String", content = "return $fullFieldColumn").apply {
                    isOverride = true
                }

                // add vanilla getPrimaryKeyId() / setPrimaryKeyId(...) for the primary key
                recordClass.addFun("getPrimaryKeyId", field.kotlinTypeText, content = "return $fieldNameJavaStyle").apply {
                    isOverride = true
                }

                recordClass.addFun("setPrimaryKeyId", parameters = listOf(KotlinVal("id", newVariable.dataType)), content = "this.$fieldNameJavaStyle = id").apply {
                    isOverride = true
                }
            }

            if (!recordClass.isEnum()) {
                recordClass.addVar(newVariable)
            }

            // method values
            if (!(primaryKey && field.isIncrement)) {
                var value = fieldNameJavaStyle
                val fieldType = field.jdbcDataType
                if (field.isEnumeration) {
                    value = newVariable.name + ".ordinal"
                } else if (fieldType == SchemaFieldType.DATETIME || fieldType == SchemaFieldType.DATE || fieldType == SchemaFieldType.TIMESTAMP || fieldType == SchemaFieldType.TIME) {
                    value = genConfig.dateType.getValuesValue(field, fieldNameJavaStyle)
                } else if (fieldType == SchemaFieldType.BOOLEAN) {
                    if (field.isNotNull!!) {
                        value = "if ($fieldNameJavaStyle) 1 else 0"
                    } else {
                        value = "if ($fieldNameJavaStyle != null) (if ($fieldNameJavaStyle) 1 else 0) else 0"
                    }
                }
                contentValuesContent.append("values.put(").append(fullFieldColumn).append(", ").append(value).append(")\n")

                if (valuesContentItemCount > 0) {
                    valuesContent.append(",\n")
                }
                valuesContentItemCount++
                valuesContent.append(TAB).append(value)

                setContentValuesContent += fieldNameJavaStyle + " = " + getContentValuesGetterMethod(field, fullFieldColumn, newVariable) + "\n"
            } else {
                // id column
                if (valuesContentItemCount > 0) {
                    valuesContent.append(",\n")
                }
                valuesContentItemCount++
                valuesContent.append(TAB).append(fieldNameJavaStyle)
            }

            setContentCursorContent += fieldNameJavaStyle + " = " + getContentValuesCursorGetterMethod(field, fullFieldColumn, newVariable) + "\n"

            // static getter method that takes a Cursor parameter
            constClass.addImport("android.database.Cursor")
            constClass.addFun(newVariable.getGetterMethodName(), newVariable.dataType, listOf(KotlinVal("cursor", "Cursor")), "return " + getContentValuesCursorGetterMethod(field, fieldColumn, newVariable) + "")
        }

        if (!primaryKeyAdded && (entityType == SchemaEntityType.VIEW || entityType == SchemaEntityType.QUERY)) {
            recordClass.addFun("getIdColumnName", "String", content = "return \"\"").apply {
                isOverride = true
            }

            // add vanilla getPrimaryKeyId() / setPrimaryKeyId() for the primary key
            recordClass.addFun("getPrimaryKeyId", "Long", content = "return 0").apply {
                isOverride = true
            }
            recordClass.addFun("setPrimaryKeyId", parameters = listOf(KotlinVal("id", "Long")), content = "").apply {
                isOverride = true
            }
        }

        // SchemaDatabase variables
        if (entityType == SchemaEntityType.TABLE) {
            val table = entity as SchemaTable
            var createTable = SqliteRenderer.generateTableSchema(table, databaseMapping)
            createTable = createTable.replace("\n", "\" + \n" + TAB + TAB + "\"")
            createTable = createTable.replace("\t", "") // remove tabs
            constClass.addConstant("CREATE_TABLE", "\"$createTable\"").apply { const = true }
            constClass.addConstant("DROP_TABLE", "\"${SchemaRenderer.generateDropSchema(true, table)}\"").apply { const = true }
        }

        // Content values

        // All keys constant
        if (!recordClass.isEnum()) {
            recordClass.addImport("android.content.ContentValues")
            recordClass.addImport("android.database.Cursor")

            // columns
            var allColumnsDefaultValue = "arrayOf(\n"
            var allColumnsFullDefaultValue = "arrayOf(\n"

            var hasColumn = false
            for (column in columns) {
                if (hasColumn) {
                    allColumnsDefaultValue += ",\n"
                    allColumnsFullDefaultValue += ",\n"
                }
                allColumnsDefaultValue += TAB + TAB + column
                allColumnsFullDefaultValue += TAB + TAB + "FULL_" + column
                hasColumn = true
            }
            allColumnsDefaultValue += ")"
            allColumnsFullDefaultValue += ")"

            // columns
            constClass.addConstant(ALL_COLUMNS_VAR_NAME, defaultValue = allColumnsDefaultValue)
            recordClass.addFun("getAllColumns", "Array<String>", content = "return $constClassName.$ALL_COLUMNS_VAR_NAME.clone()").apply {
                isOverride = true
            }

            // columns full
            constClass.addConstant(ALL_COLUMNS_FULL_VAR_NAME, defaultValue = allColumnsFullDefaultValue)
            recordClass.addFun("getAllColumnsFull", "Array<String>", content = "return $constClassName.$ALL_COLUMNS_FULL_VAR_NAME.clone()")

            contentValuesContent.append("return values")
            recordClass.addFun("getContentValues", "ContentValues", content = contentValuesContent.toString()).apply {
                isOverride = true
            }

            valuesContent.append(")\n")
            recordClass.addFun("getValues", "Array<Any?>", content = valuesContent.toString()).apply {
                isOverride = true
            }

            recordClass.addFun("setContent", parameters = listOf(KotlinVal("values", "ContentValues")), content = setContentValuesContent)
            recordClass.addFun("setContent", parameters = listOf(KotlinVal("cursor", "Cursor")), content = setContentCursorContent).apply {
                isOverride = true
            }
        }

        // add method to cleanup many-to-one left-overs
        if (!recordClass.isEnum()) {
            val orphanParams = ArrayList<JavaVariable>()

            if (cleanupOrphansContent.length > 0) {
                recordClass.addFun(CLEANUP_ORPHANS_METHOD_NAME, content = cleanupOrphansContent.toString())
            }

            // new record check
            recordClass.addFun("isNewRecord", "Boolean", content = "return primaryKeyId <= 0").apply {
                isOverride = true
            }
        }
    }

    private fun addHeader(someClass: KotlinClass, className: String) {
        // Do not place date in file because it will cause a new check-in to scm
        someClass.fileHeaderComment = "/*\n" +
                " * $className.kt\n" +
                " *\n" +
                " * GENERATED FILE - DO NOT EDIT\n" +
                " * CHECKSTYLE:OFF\n" +
                " * \n" +
                " */\n"

        // Since this is generated code.... suppress all warnings
        someClass.addAnnotation("@SuppressWarnings(\"all\")")
    }

    private fun initClassAsEnum(packageName: String, enumClassName: String, entity: SchemaEntity) {
        if (entity.type != SchemaEntityType.TABLE) {
            return
        }

        val table = entity as SchemaTable
        recordClass = KotlinEnum(enumClassName, packageName, table.tableEnumsText)
    }

    /**
     * For method setContent(ContentValues values).
     */
    private fun getContentValuesGetterMethod(field: SchemaField, paramValue: String, newVariable: KotlinVar): String {
        if (field.isEnumeration) {
            return newVariable.dataType + ".values()[values.getAsInteger(" + paramValue + ")]"
        }

        val type = field.javaClassType
        if (type == Integer.TYPE || type == Int::class.java) {
            return "values.getAsInteger($paramValue)"
        } else if (type == String::class.java) {
            return "values.getAsString($paramValue)"
        } else if (type == java.lang.Long.TYPE || type == Long::class.java) {
            return "values.getAsLong($paramValue)"
        } else if (type == java.lang.Boolean.TYPE || type == Boolean::class.java) {
            return "values.getAsBoolean($paramValue)"
        } else if (type == Date::class.java) {
            var method = genConfig.dateType.getValuesDbStringToObjectMethod(field, paramValue)
            if (field.isNotNull) {
                method += "!!"
            }
            return method
        } else if (type == java.lang.Float.TYPE || type == Float::class.java) {
            // || type == Fraction.class || type == Money.class) {
            return "values.getAsFloat($paramValue)"
        } else if (type == java.lang.Double.TYPE || type == Double::class.java) {
            return "values.getAsDouble($paramValue)"
        } else if (type == ByteArray::class.java || type == Array<Byte>::class.java) {
            return "values.getAsByteArray($paramValue)"
        } else {
            return "[[UNHANDLED FIELD TYPE: $type]]"
        }
    }

    /**
     * For method setContent(Cursor cursor).
     */
    private fun getContentValuesCursorGetterMethod(field: SchemaField, paramValue: String, newVariable: KotlinVar): String {
        if (field.isEnumeration) {
            return newVariable.dataType + ".values()[cursor.getInt(cursor.getColumnIndexOrThrow(" + paramValue + "))]"
        }

        val type = field.javaClassType
        if (type == Integer.TYPE) {
            return "cursor.getInt(cursor.getColumnIndexOrThrow($paramValue))"
        } else if (type == Int::class.java) {
            return "if (!cursor.isNull(cursor.getColumnIndexOrThrow($paramValue))) cursor.getInt(cursor.getColumnIndexOrThrow($paramValue)) else null"
        } else if (type == String::class.java && field.isNotNull!!) {
            return "cursor.getString(cursor.getColumnIndexOrThrow($paramValue))"
        } else if (type == String::class.java) {
            return "if (!cursor.isNull(cursor.getColumnIndexOrThrow($paramValue))) cursor.getString(cursor.getColumnIndexOrThrow($paramValue)) else null"
        } else if (type == java.lang.Long.TYPE) {
            return "cursor.getLong(cursor.getColumnIndexOrThrow($paramValue))"
        } else if (type == Long::class.java) {
            return "if (!cursor.isNull(cursor.getColumnIndexOrThrow($paramValue))) cursor.getLong(cursor.getColumnIndexOrThrow($paramValue)) else null"
        } else if (type == java.lang.Boolean.TYPE) {
            return "if (cursor.getInt(cursor.getColumnIndexOrThrow($paramValue)) != 0) true else false"
        } else if (type == Boolean::class.java) {
            return "if (!cursor.isNull(cursor.getColumnIndexOrThrow($paramValue))) (if (cursor.getInt(cursor.getColumnIndexOrThrow($paramValue)) != 0) true else false) else null"
        } else if (type == Date::class.java) {
            return genConfig.dateType.getCursorDbStringToObjectMethod(field, paramValue, true)
        } else if (type == java.lang.Float.TYPE) {
            return "cursor.getFloat(cursor.getColumnIndexOrThrow($paramValue))"
        } else if (type == Float::class.java) {
            return "if (!cursor.isNull(cursor.getColumnIndexOrThrow($paramValue))) cursor.getFloat(cursor.getColumnIndexOrThrow($paramValue)) else null"
        } else if (type == java.lang.Double.TYPE || type == Double::class.java) {
            return "cursor.getDouble(cursor.getColumnIndexOrThrow($paramValue))"
        } else if (type == Double::class.java) {
            return "if (!cursor.isNull(cursor.getColumnIndexOrThrow($paramValue))) cursor.getDouble(cursor.getColumnIndexOrThrow($paramValue)) else null"
        } else if (type == ByteArray::class.java || type == Array<Byte>::class.java) {
            return "cursor.getBlob(cursor.getColumnIndexOrThrow($paramValue))"
        } else {
            return "[[UNHANDLED FIELD TYPE: $type]]"
        }
    }

    private fun generateEnumeration(readOnlyEntity: Boolean, field: SchemaField, fieldNameJavaStyle: String, packageName: String, database: SchemaDatabase): KotlinVar {
        val newVar: KotlinVar
        if (field.jdbcDataType.isNumberDataType) {
            if (!field.foreignKeyTable.isEmpty()) {
                // define name of enum
                val enumClassInfo = database.getTableClassInfo(field.foreignKeyTable)
                val enumName = enumClassInfo.className

                // local definition of enumeration?
                val localEnumerations = field.enumValues
                if (localEnumerations != null && localEnumerations.size > 0) {
                    recordClass.addEnum(enumName, field.enumValues)
                } else {
                    // we must import the enum
                    val enumPackage = enumClassInfo.getPackageName(packageName) + "." + enumName

                    // build foreign key packagename
                    //                    String[] packageElements = packageName.split("\\.")
                    //                    for (int i = 0 i < packageElements.length - 1 i++) {
                    //                        enumPackage += packageElements[i] + "."
                    //                    }
                    //                    enumPackage += enumName.toLowerCase() + "." + enumName


                    constClass.addImport(enumPackage)
                    recordClass.addImport(enumPackage)
                }

                newVar = KotlinVar(fieldNameJavaStyle, enumName)
                newVar.setDefaultValue(enumName + "." + field.enumerationDefault, false)
            } else if (!field.enumerationClass.isEmpty()) {
                // use user defined class
                val enumClassName = field.enumerationClass

                newVar = KotlinVar(fieldNameJavaStyle, enumClassName)
                newVar.setDefaultValue(enumClassName + "." + field.enumerationDefault, false)
            } else {
                // ENUM without a foreign key table
                val javaStyleFieldName = field.getName(true)
                val firstChar = javaStyleFieldName.substring(0, 1).toUpperCase()
                val enumName = firstChar + javaStyleFieldName.substring(1)

                if (useInnerEnums) {
                    recordClass.addEnum(enumName, field.enumValues)
                } else {
                    enumerationClasses.add(KotlinEnum(enumName, field.enumValues))
                }

                newVar = KotlinVar(enumName, fieldNameJavaStyle)
                newVar.setDefaultValue(enumName + "." + field.enumerationDefault, false)
            }
        } else {
            newVar = KotlinVar(field.kotlinTypeText, fieldNameJavaStyle)
        }

        return newVar
    }

    private fun generateFieldVariable(fieldNameJavaStyle: String, field: SchemaField): KotlinVar {
        var typeText = field.kotlinTypeText
        var fieldDefaultValue = field.defaultValue

        // check to see if we need to override the Date type
        if (typeText.endsWith("Date") && genConfig.dateType.isAlternative) {
            typeText = genConfig.dateType.getJavaClassDataType(field)
            fieldDefaultValue = genConfig.dateType.getJavaClassDataTypeDefaultValue(field)
        }

        if (typeText.endsWith("Date?") && genConfig.dateType.isAlternative) {
            typeText = genConfig.dateType.getJavaClassDataType(field) + "?"
            fieldDefaultValue = "null"
        }

        if (fieldDefaultValue.isBlank()) {
            fieldDefaultValue = field.getJdbcDataType().kotlinDefaultValue
        }

        return KotlinVar(fieldNameJavaStyle, typeText).apply {
            defaultValue = fieldDefaultValue
        }
    }

    fun writeToFile(directoryName: String) {
        constClass.writeToDisk(directoryName)
        recordClass.writeToDisk(directoryName)

        for (enumClass in enumerationClasses) {
            enumClass.writeToDisk(directoryName)
        }
    }

    companion object {
        private val TAB = JavaClass.getTab()
        private val CLEANUP_ORPHANS_METHOD_NAME = "cleanupOrphans"
        private val ALL_COLUMNS_VAR_NAME = "ALL_COLUMNS"
        private val ALL_COLUMNS_FULL_VAR_NAME = "ALL_COLUMNS_FULL"
        val PRIMARY_KEY_COLUMN = "PRIMARY_KEY_COLUMN"

        fun createClassName(enumTable: Boolean, className: String): String {
            return if (enumTable) className else className + "BaseRecord"
        }
    }
}
/**
 * Creates a new instance of AndroidBaseRecordRenderer.
 */
