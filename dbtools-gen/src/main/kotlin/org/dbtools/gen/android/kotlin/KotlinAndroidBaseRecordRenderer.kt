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
    private var bindInsertStatementContentIndex = 1 // 1 based
    private var bindUpdateStatementContentIndex = 1 // 1 based

    fun generate(database: SchemaDatabase, entity: SchemaEntity, packageName: String, databaseMapping: DatabaseMapping) {
        // reset data
        bindInsertStatementContentIndex = 1
        bindUpdateStatementContentIndex = 1

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
            recordClass.addImport("org.dbtools.android.domain.database.statement.StatementWrapper")
        }

        // header comment
        addHeader(constClass, className)
        addHeader(recordClass, className)

        var primaryKeyAdded = false
        var primaryKeyFieldName = ""
        var primaryKeyField: SchemaField? = null

        // constants and variables
        val databaseName = database.name
        constClass.addConstant("DATABASE", "\"$databaseName\"").apply { const = true }

        val tableName = entity.name
        if (entityType != SchemaEntityType.QUERY) {
            constClass.addConstant("TABLE", "\"$tableName\"").apply { const = true }
            constClass.addConstant("FULL_TABLE", "\"$databaseName.$tableName\"").apply { const = true }
        }

        // post field method content
        val contentValuesContent = StringBuilder()
        val valuesContent = StringBuilder("return arrayOf(\n")
        val copyContent = StringBuilder("var copy = $entityClassName()\n")
        val bindInsertStatementContent = StringBuilder()
        val bindUpdateStatementContent = StringBuilder()
        var valuesContentItemCount = 0
        var setContentValuesContent = ""
        var setContentCursorContent = ""

        val columns = ArrayList<String>()
        for (field in entity.fields) {
            val primaryKey = field.isPrimaryKey
            val fieldName = field.name
            val fieldType = field.jdbcDataType
            val notNullField = field.isNotNull!!
            val primitiveField = fieldType.isJavaTypePrimitive(!field.isNotNull)
            val dateTypeField = fieldType == SchemaFieldType.DATETIME || fieldType == SchemaFieldType.DATE || fieldType == SchemaFieldType.TIMESTAMP || fieldType == SchemaFieldType.TIME

            // override default name
            val fieldNameJavaStyle = field.getName(true)

            // check for second primary key
            if (primaryKey) {
                if (primaryKeyAdded) {
                    throw IllegalStateException("Cannot have more than 1 Primary Key [$fieldNameJavaStyle]")
                } else {
                    primaryKeyField = field
                    primaryKeyFieldName = fieldName
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
                newVariable = generateEnumeration(field, fieldNameJavaStyle, packageName, database)
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

            // copy
            copyContent.append("copy.").append(newVariable.name).append(" = ")
            if (dateTypeField) {
                copyContent.append(genConfig.dateType.getCopy(newVariable.name, true, field.isNotNull))
            } else {
                copyContent.append(newVariable.name)
            }
            copyContent.append("\n")

            // method values
            if (!(primaryKey && field.isIncrement)) {
                var value = fieldNameJavaStyle

                if (field.isEnumeration) {
                    value = newVariable.name + ".ordinal.toLong()"
                } else if (dateTypeField) {
                    val dateValue = genConfig.dateType.getValuesValue(field, fieldNameJavaStyle)
                    if (notNullField) {
                        value = dateValue + "!!"
                    } else {
                        value = dateValue
                    }
                } else if (fieldType == SchemaFieldType.INTEGER) {
                    value = "($fieldNameJavaStyle as Int).toLong()"
                } else if (fieldType == SchemaFieldType.FLOAT) {
                    value = "($fieldNameJavaStyle as Float).toDouble()"
                } else if (fieldType == SchemaFieldType.BOOLEAN) {
                    if (field.isNotNull) {
                        value = "if ($fieldNameJavaStyle) 1L else 0L"
                    } else {
                        value = "if ($fieldNameJavaStyle != null) (if ($fieldNameJavaStyle as Boolean) 1L else 0L) else 0L"
                    }
                }

                contentValuesContent.append("values.put(").append(fullFieldColumn).append(", ").append(value).append(")\n")

                // bindStatementContent
                when (fieldType) {
                    SchemaFieldType.BOOLEAN, SchemaFieldType.BIT, SchemaFieldType.TINYINT, SchemaFieldType.SMALLINT, SchemaFieldType.INTEGER, SchemaFieldType.BIGINT, SchemaFieldType.NUMERIC, SchemaFieldType.BIGINTEGER, SchemaFieldType.TIMESTAMP -> {
                        addBindInsert(bindInsertStatementContent, "bindLong", fieldNameJavaStyle, value, primitiveField, notNullField)
                        addBindUpdate(bindUpdateStatementContent, "bindLong", fieldNameJavaStyle, value, primitiveField, notNullField)
                    }
                    SchemaFieldType.REAL, SchemaFieldType.FLOAT, SchemaFieldType.DOUBLE, SchemaFieldType.DECIMAL, SchemaFieldType.BIGDECIMAL -> {
                        addBindInsert(bindInsertStatementContent, "bindDouble", fieldNameJavaStyle, value, primitiveField, notNullField)
                        addBindUpdate(bindUpdateStatementContent, "bindDouble", fieldNameJavaStyle, value, primitiveField, notNullField)
                    }
                    SchemaFieldType.CHAR, SchemaFieldType.VARCHAR, SchemaFieldType.LONGVARCHAR, SchemaFieldType.CLOB, SchemaFieldType.DATETIME, SchemaFieldType.DATE, SchemaFieldType.TIME -> {
                        addBindInsert(bindInsertStatementContent, "bindString", fieldNameJavaStyle, value, primitiveField, notNullField)
                        addBindUpdate(bindUpdateStatementContent, "bindString", fieldNameJavaStyle, value, primitiveField, notNullField)
                    }
                    SchemaFieldType.BLOB -> {
                        addBindInsert(bindInsertStatementContent, "bindBlob", fieldNameJavaStyle, value, primitiveField, notNullField)
                        addBindUpdate(bindUpdateStatementContent, "bindBlob", fieldNameJavaStyle, value, primitiveField, notNullField)
                    }
                }

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

        // bind the primary key value LAST (it is the where clause part of the update code)
        if (primaryKeyField != null) {
            addBindUpdate(bindUpdateStatementContent, "bindLong", primaryKeyField.getName(true), primaryKeyField.getName(true), primaryKeyField.jdbcDataType.isJavaTypePrimitive, primaryKeyField.isNotNull)
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
            // CREATE TABLE
            val table = entity as SchemaTable
            var createTable = SqliteRenderer.generateTableSchema(table, databaseMapping)
            createTable = createTable.replace("\n", "\" + \n" + TAB + TAB + "\"")
            createTable = createTable.replace("\t", "") // remove tabs
            constClass.addConstant("CREATE_TABLE", "\"$createTable\"").apply { const = true }
            constClass.addConstant("DROP_TABLE", "\"${SchemaRenderer.generateDropSchema(true, table)}\"").apply { const = true }

            // INSERT and UPDATE
            val insertStatement = StringBuilder("INSERT INTO $tableName (")
            val updateStatement = StringBuilder("UPDATE $tableName SET ")

            // columns
            var columnCount = 0
            for (field in entity.fields) {
                if (!(field.isPrimaryKey && field.isIncrement)) {
                    insertStatement.append(if (columnCount > 0) "," else "")
                    insertStatement.append(field.name)

                    updateStatement.append(if (columnCount > 0) ", " else "")
                    updateStatement.append(field.name).append("=").append("?")

                    columnCount++
                }
            }

            // mid
            insertStatement.append(')')
            insertStatement.append(" VALUES (")

            updateStatement.append(" WHERE ").append(primaryKeyFieldName).append(" = ?")

            // ?'s
            for (i in 0..columnCount - 1) {
                insertStatement.append(if (i > 0) ",?" else "?")
            }

            // close
            insertStatement.append(')')

            // add to class
            constClass.addConstant("INSERT_STATEMENT", defaultValue = "\"" + insertStatement.toString() + "\"").apply { const = true }
            constClass.addConstant("UPDATE_STATEMENT", defaultValue = "\"" + updateStatement.toString() + "\"").apply { const = true }
        }

        // Content values

        // All keys constant
        if (!recordClass.isEnum()) {
            recordClass.addImport("org.dbtools.android.domain.database.contentvalues.DBToolsContentValues")
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

            recordClass.addFun("getContentValues", parameters = listOf(KotlinVal("values", "DBToolsContentValues<*>")), content = contentValuesContent.toString()).apply {
                isOverride = true
            }

            valuesContent.append(")\n")
            recordClass.addFun("getValues", "Array<Any?>", content = valuesContent.toString()).apply {
                isOverride = true
            }

            copyContent.append("return copy")
            recordClass.addFun("copy", entityClassName, content = copyContent.toString())

            recordClass.addFun("bindInsertStatement", parameters = listOf(KotlinVal("statement", "StatementWrapper")), content = bindInsertStatementContent.toString()).apply {
                isOverride = true
            }
            recordClass.addFun("bindUpdateStatement", parameters = listOf(KotlinVal("statement", "StatementWrapper")), content = bindUpdateStatementContent.toString()).apply {
                isOverride = true
            }

            recordClass.addFun("setContent", parameters = listOf(KotlinVal("values", "DBToolsContentValues<*>")), content = setContentValuesContent).apply {
                isOverride = true
            }
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
        if (type == Integer.TYPE || type == Int::class.java || type == java.lang.Integer::class.java) {
            return "values.getAsInteger($paramValue)"
        } else if (type == String::class.java || type == java.lang.String::class.java) {
            return "values.getAsString($paramValue)"
        } else if (type == java.lang.Long.TYPE || type == Long::class.java || type == java.lang.Long::class.java) {
            return "values.getAsLong($paramValue)"
        } else if (type == java.lang.Boolean.TYPE || type == Boolean::class.java || type == java.lang.Boolean::class.java) {
            return "values.getAsBoolean($paramValue)"
        } else if (type == Date::class.java) {
            var method = genConfig.dateType.getValuesDbStringToObjectMethod(field, paramValue, true)
            if (field.isNotNull) {
                method += "!!"
            }
            return method
        } else if (type == java.lang.Float.TYPE || type == Float::class.java || type == java.lang.Float::class.java) {
            // || type == Fraction.class || type == Money.class) {
            return "values.getAsFloat($paramValue)"
        } else if (type == java.lang.Double.TYPE || type == Double::class.java || type == java.lang.Double::class.java) {
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
        } else if (type == Int::class.java || type == java.lang.Integer::class.java) {
            return "if (!cursor.isNull(cursor.getColumnIndexOrThrow($paramValue))) cursor.getInt(cursor.getColumnIndexOrThrow($paramValue)) else null"
        } else if (type == String::class.java && field.isNotNull!!) {
            return "cursor.getString(cursor.getColumnIndexOrThrow($paramValue))"
        } else if (type == String::class.java || type == java.lang.String::class.java) {
            return "if (!cursor.isNull(cursor.getColumnIndexOrThrow($paramValue))) cursor.getString(cursor.getColumnIndexOrThrow($paramValue)) else null"
        } else if (type == java.lang.Long.TYPE) {
            return "cursor.getLong(cursor.getColumnIndexOrThrow($paramValue))"
        } else if (type == Long::class.java || type == java.lang.Long::class.java) {
            return "if (!cursor.isNull(cursor.getColumnIndexOrThrow($paramValue))) cursor.getLong(cursor.getColumnIndexOrThrow($paramValue)) else null"
        } else if (type == java.lang.Boolean.TYPE) {
            return "if (cursor.getInt(cursor.getColumnIndexOrThrow($paramValue)) != 0) true else false"
        } else if (type == Boolean::class.java || type == java.lang.Boolean::class.java) {
            return "if (!cursor.isNull(cursor.getColumnIndexOrThrow($paramValue))) (if (cursor.getInt(cursor.getColumnIndexOrThrow($paramValue)) != 0) true else false) else null"
        } else if (type == Date::class.java) {
            return genConfig.dateType.getCursorDbStringToObjectMethod(field, paramValue, true)
        } else if (type == java.lang.Float.TYPE) {
            return "cursor.getFloat(cursor.getColumnIndexOrThrow($paramValue))"
        } else if (type == Float::class.java || type == java.lang.Float::class.java) {
            return "if (!cursor.isNull(cursor.getColumnIndexOrThrow($paramValue))) cursor.getFloat(cursor.getColumnIndexOrThrow($paramValue)) else null"
        } else if (type == java.lang.Double.TYPE || type == Double::class.java) {
            return "cursor.getDouble(cursor.getColumnIndexOrThrow($paramValue))"
        } else if (type == Double::class.java || type == java.lang.Double::class.java) {
            return "if (!cursor.isNull(cursor.getColumnIndexOrThrow($paramValue))) cursor.getDouble(cursor.getColumnIndexOrThrow($paramValue)) else null"
        } else if (type == ByteArray::class.java || type == Array<Byte>::class.java) {
            return "cursor.getBlob(cursor.getColumnIndexOrThrow($paramValue))"
        } else {
            return "[[UNHANDLED FIELD TYPE: $type]]"
        }
    }

    private fun generateEnumeration(field: SchemaField, fieldNameJavaStyle: String, packageName: String, database: SchemaDatabase): KotlinVar {
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

    private fun addBindInsert(bindStatementContent: StringBuilder, bindMethodName: String, fieldNameJavaStyle: String, value: String, primitive: Boolean, notNull: Boolean) {
        addBind(bindStatementContent, bindInsertStatementContentIndex, bindMethodName, fieldNameJavaStyle, value, primitive, notNull)
        bindInsertStatementContentIndex++
    }

    private fun addBindUpdate(bindStatementContent: StringBuilder, bindMethodName: String, fieldNameJavaStyle: String, value: String, primitive: Boolean, notNull: Boolean) {
        addBind(bindStatementContent, bindUpdateStatementContentIndex, bindMethodName, fieldNameJavaStyle, value, primitive, notNull)
        bindUpdateStatementContentIndex++
    }

    private fun addBind(bindStatementContent: StringBuilder, bindIndex: Int, bindMethodName: String, fieldNameJavaStyle: String, value: String, isPrimitive: Boolean, notNull: Boolean) {
        if (isPrimitive || notNull) {
            bindStatementContent.append("statement.$bindMethodName(").append(bindIndex).append(", ").append(value).append(")\n")
        } else {
            bindStatementContent.append("if (").append(fieldNameJavaStyle).append(" != null").append(") {\n")
            bindStatementContent.append(TAB).append("statement.$bindMethodName(").append(bindIndex).append(", ").append(value).append("!!)\n")
            bindStatementContent.append("} else {\n")
            bindStatementContent.append(TAB).append("statement.bindNull(").append(bindIndex).append(")\n")
            bindStatementContent.append("}\n")
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