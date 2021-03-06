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
import org.dbtools.codegen.kotlin.KotlinClass
import org.dbtools.codegen.kotlin.KotlinEnum
import org.dbtools.codegen.kotlin.KotlinObjectClass
import org.dbtools.codegen.kotlin.KotlinVal
import org.dbtools.codegen.kotlin.KotlinVar
import org.dbtools.gen.GenConfig
import org.dbtools.gen.android.AndroidGeneratedEntityInfo
import org.dbtools.renderer.SchemaRenderer
import org.dbtools.renderer.SqliteRenderer
import org.dbtools.schema.dbmappings.DatabaseMapping
import org.dbtools.schema.schemafile.SchemaDatabase
import org.dbtools.schema.schemafile.SchemaEntity
import org.dbtools.schema.schemafile.SchemaEntityType
import org.dbtools.schema.schemafile.SchemaField
import org.dbtools.schema.schemafile.SchemaFieldType
import org.dbtools.schema.schemafile.SchemaTable
import java.util.ArrayList
import java.util.Date


class KotlinAndroidBaseRecordRenderer(val genConfig: GenConfig) {

    private var constClass = KotlinObjectClass()
    private var recordClass = KotlinClass()
    private val enumerationClasses = ArrayList<KotlinEnum>()
    private val cleanupOrphansContent = StringBuilder()
    private val useInnerEnums = true
    private var bindInsertStatementContentIndex = 1 // 1 based
    private var bindUpdateStatementContentIndex = 1 // 1 based

    fun generate(database: SchemaDatabase, entity: SchemaEntity, packageName: String, databaseMapping: DatabaseMapping): AndroidGeneratedEntityInfo {
        val generatedEntityInfo = AndroidGeneratedEntityInfo()

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
                extends = "AndroidBaseRecord"
                createDefaultConstructor = true
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
        constClass.addConstant("DATABASE", "\"${database.getName(false)}\"").apply { const = true }

        val databaseName = database.getName(false)
        val tableName = entity.name
        if (entityType != SchemaEntityType.QUERY) {
            constClass.addConstant("TABLE", "\"$tableName\"").apply { const = true }
            constClass.addConstant("FULL_TABLE", "\"$databaseName.$tableName\"").apply { const = true }
        }

        // post field method content
        val contentValuesContent = StringBuilder()
        val valuesContent = StringBuilder("return arrayOf(\n")
        val copyContent = StringBuilder("val copy = $entityClassName()\n")
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
            val notNullField = field.isNotNull
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

            newVariable.open = true

            // Primary key / not enum methods
            if (primaryKey && !recordClass.isEnum()) {
                addPrimaryKeyFunctions(newVariable.dataType, fullFieldColumn, fieldNameJavaStyle)
            }

            if (!recordClass.isEnum()) {
                recordClass.addVar(newVariable)
            }

            // copy (include primary key)
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
                    when(fieldType) {
                        SchemaFieldType.BIT, SchemaFieldType.TINYINT, SchemaFieldType.SMALLINT,
                        SchemaFieldType.INTEGER, SchemaFieldType.NUMERIC, SchemaFieldType.BIGINT -> {
                            value = when(notNullField) {
                                true -> "${newVariable.name}.ordinal.toLong()"
                                else -> "${newVariable.name}?.ordinal?.toLong()"
                            }
                        }
                        else -> {
                            value = when(notNullField) {
                                true -> "${newVariable.name}.toString()"
                                else -> "${newVariable.name}?.toString()"
                            }
                        }
                    }

                } else if (dateTypeField) {
                    val dateValue = genConfig.dateType.getValuesValue(field, fieldNameJavaStyle)
                    if (notNullField) {
                        value = dateValue + "!!"
                    } else {
                        value = dateValue
                    }
                } else if (fieldType == SchemaFieldType.INTEGER) {
                    value = when(notNullField) {
                        true -> "$fieldNameJavaStyle.toLong()"
                        else -> "$fieldNameJavaStyle?.toLong()"
                    }
                } else if (fieldType == SchemaFieldType.FLOAT || fieldType == SchemaFieldType.REAL || fieldType == SchemaFieldType.DECIMAL) {
                    value = when(notNullField) {
                        true -> "$fieldNameJavaStyle.toDouble()"
                        else -> "$fieldNameJavaStyle?.toDouble()"
                    }
                } else if (fieldType == SchemaFieldType.BOOLEAN) {
                    value = when (notNullField) {
                        true -> "if ($fieldNameJavaStyle) 1L else 0L"
                        else -> "if ($fieldNameJavaStyle ?: false) 1L else 0L"
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
                    else -> {
                        // do nothing
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
                override = true
            }

            // add vanilla getPrimaryKeyId() / setPrimaryKeyId() for the primary key
            recordClass.addFun("getPrimaryKeyId", "Long", content = "return 0").apply {
                override = true
            }
            recordClass.addFun("setPrimaryKeyId", parameters = listOf(KotlinVal("id", "Long")), content = "").apply {
                override = true
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
                override = true
            }

            // columns full
            constClass.addConstant(ALL_COLUMNS_FULL_VAR_NAME, defaultValue = allColumnsFullDefaultValue)
            recordClass.addFun("getAllColumnsFull", "Array<String>", content = "return $constClassName.$ALL_COLUMNS_FULL_VAR_NAME.clone()")

            recordClass.addFun("getContentValues", parameters = listOf(KotlinVal("values", "DBToolsContentValues<*>")), content = contentValuesContent.toString()).apply {
                override = true
            }

            valuesContent.append(")\n")
            recordClass.addFun("getValues", "Array<Any?>", content = valuesContent.toString()).apply {
                override = true
            }

            copyContent.append("return copy")
            recordClass.addFun("copy", entityClassName, content = copyContent.toString()).apply {
                open = true
            }

            recordClass.addFun("bindInsertStatement", parameters = listOf(KotlinVal("statement", "StatementWrapper")), content = bindInsertStatementContent.toString()).apply {
                addAnnotation("""@Suppress("UNNECESSARY_NOT_NULL_ASSERTION")""")
                override = true
            }
            recordClass.addFun("bindUpdateStatement", parameters = listOf(KotlinVal("statement", "StatementWrapper")), content = bindUpdateStatementContent.toString()).apply {
                addAnnotation("""@Suppress("UNNECESSARY_NOT_NULL_ASSERTION")""")
                override = true
            }

            recordClass.addFun("setContent", parameters = listOf(KotlinVal("values", "DBToolsContentValues<*>")), content = setContentValuesContent).apply {
                override = true
            }
            recordClass.addFun("setContent", parameters = listOf(KotlinVal("cursor", "Cursor")), content = setContentCursorContent).apply {
                override = true
            }
        }

        // add method to cleanup many-to-one left-overs
        if (!recordClass.isEnum()) {
            if (cleanupOrphansContent.isNotEmpty()) {
                recordClass.addFun(CLEANUP_ORPHANS_METHOD_NAME, content = cleanupOrphansContent.toString())
            }

            // new record check
            recordClass.addFun("isNewRecord", "Boolean", content = "return primaryKeyId <= 0").apply {
                override = true
            }
        }

        if (!primaryKeyAdded && entityType == SchemaEntityType.TABLE) {
            // make sure that overridden methods are
            addPrimaryKeyFunctions("Long", "\"NO_PRIMARY_KEY\"", "0")
        }

        generatedEntityInfo.isPrimaryKeyAdded = primaryKeyAdded
        return generatedEntityInfo
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

        if (!someClass.isEnum()) {
            // Since this is generated code.... suppress all warnings
            someClass.addAnnotation("""@Suppress("LeakingThis", "unused", "RemoveEmptySecondaryConstructorBody", "ConvertSecondaryConstructorToPrimary")""") // kotlin specific
            someClass.addAnnotation("""@SuppressWarnings("all")""")
        }
    }

    private fun addPrimaryKeyFunctions(dataType: String, fullFieldColumn: String, fieldNameJavaStyle: String) {
        recordClass.addFun("getIdColumnName", "String", content = "return $fullFieldColumn").apply {
            override = true
        }

        // add vanilla getPrimaryKeyId() / setPrimaryKeyId(...) for the primary key
        recordClass.addFun("getPrimaryKeyId", dataType, content = "return $fieldNameJavaStyle").apply {
            override = true
        }

        if (fieldNameJavaStyle != "0") {
            recordClass.addFun("setPrimaryKeyId", parameters = listOf(KotlinVal("id", dataType)), content = "this.$fieldNameJavaStyle = id").apply {
                override = true
            }
        } else {
            recordClass.addFun("setPrimaryKeyId", parameters = listOf(KotlinVal("id", dataType)), content = "// NO_PRIMARY_KEY").apply {
                override = true
            }
        }
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
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun getContentValuesGetterMethod(field: SchemaField, paramValue: String, newVariable: KotlinVar): String {
        if (field.isEnumeration) {
            when(field.jdbcDataType) {
                SchemaFieldType.BIT, SchemaFieldType.TINYINT, SchemaFieldType.SMALLINT,
                SchemaFieldType.INTEGER, SchemaFieldType.NUMERIC, SchemaFieldType.BIGINT -> {
                    return "org.dbtools.android.domain.util.EnumUtil.ordinalToEnum(${newVariable.dataType}::class.java, values.getAsInteger($paramValue), ${newVariable.defaultValue})"
                }
                else -> {
                    return "org.dbtools.android.domain.util.EnumUtil.stringToEnum(${newVariable.dataType}::class.java, values.getAsString($paramValue), ${newVariable.defaultValue})"
                }
            }
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
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun getContentValuesCursorGetterMethod(field: SchemaField, paramValue: String, newVariable: KotlinVar): String {
        if (field.isEnumeration) {
            when(field.jdbcDataType) {
                SchemaFieldType.BIT, SchemaFieldType.TINYINT, SchemaFieldType.SMALLINT,
                SchemaFieldType.INTEGER, SchemaFieldType.NUMERIC, SchemaFieldType.BIGINT -> {
                    return "org.dbtools.android.domain.util.EnumUtil.ordinalToEnum(${newVariable.dataType}::class.java, cursor.getInt(cursor.getColumnIndexOrThrow($paramValue)), ${newVariable.defaultValue})"
                }
                else -> {
                    return "org.dbtools.android.domain.util.EnumUtil.stringToEnum(${newVariable.dataType}::class.java, cursor.getString(cursor.getColumnIndexOrThrow($paramValue)), ${newVariable.defaultValue})"
                }
            }
        }

        val type = field.javaClassType
        if (type == Integer.TYPE) {
            return "cursor.getInt(cursor.getColumnIndexOrThrow($paramValue))"
        } else if (type == Int::class.java || type == java.lang.Integer::class.java) {
            if (field.isNotNull()) {
                return "cursor.getInt(cursor.getColumnIndexOrThrow($paramValue))"
            } else {
                return "if (!cursor.isNull(cursor.getColumnIndexOrThrow($paramValue))) cursor.getInt(cursor.getColumnIndexOrThrow($paramValue)) else null"
            }
        } else if (type == String::class.java) {
            if (field.isNotNull()) {
                return "cursor.getString(cursor.getColumnIndexOrThrow($paramValue))"
            } else {
                return "if (!cursor.isNull(cursor.getColumnIndexOrThrow($paramValue))) cursor.getString(cursor.getColumnIndexOrThrow($paramValue)) else null"
            }
        } else if (type == java.lang.Long.TYPE || type == Long::class.java || type == java.lang.Long::class.java) {
            if (field.isNotNull()) {
                return "cursor.getLong(cursor.getColumnIndexOrThrow($paramValue))"
            } else {
                return "if (!cursor.isNull(cursor.getColumnIndexOrThrow($paramValue))) cursor.getLong(cursor.getColumnIndexOrThrow($paramValue)) else null"
            }
        } else if (type == java.lang.Boolean.TYPE || type == Boolean::class.java || type == java.lang.Boolean::class.java) {
            if (field.isNotNull()) {
                return "cursor.getInt(cursor.getColumnIndexOrThrow($paramValue)) != 0"
            } else {
                return "if (!cursor.isNull(cursor.getColumnIndexOrThrow($paramValue))) (cursor.getInt(cursor.getColumnIndexOrThrow($paramValue)) != 0) else null"
            }
        } else if (type == Date::class.java) {
            return genConfig.dateType.getCursorDbStringToObjectMethod(field, paramValue, true)
        } else if (type == java.lang.Float.TYPE || type == Float::class.java || type == java.lang.Float::class.java) {
            if (field.isNotNull()) {
                return "cursor.getFloat(cursor.getColumnIndexOrThrow($paramValue))"
            } else {
                return "if (!cursor.isNull(cursor.getColumnIndexOrThrow($paramValue))) cursor.getFloat(cursor.getColumnIndexOrThrow($paramValue)) else null"
            }
        } else if (type == java.lang.Double.TYPE || type == Double::class.java || type == java.lang.Double::class.java) {
            if (field.isNotNull()) {
                return "cursor.getDouble(cursor.getColumnIndexOrThrow($paramValue))"
            } else {
                return "if (!cursor.isNull(cursor.getColumnIndexOrThrow($paramValue))) cursor.getDouble(cursor.getColumnIndexOrThrow($paramValue)) else null"
            }
        } else if (type == ByteArray::class.java || type == Array<Byte>::class.java) {
            if (field.isNotNull()) {
                return "cursor.getBlob(cursor.getColumnIndexOrThrow($paramValue))"
            } else {
                return "if (!cursor.isNull(cursor.getColumnIndexOrThrow($paramValue))) cursor.getBlob(cursor.getColumnIndexOrThrow($paramValue)) else null"
            }
        } else {
            return "[[UNHANDLED FIELD TYPE: $type]]"
        }
    }

    private fun generateEnumeration(field: SchemaField, fieldNameJavaStyle: String, packageName: String, database: SchemaDatabase): KotlinVar {
        val newVar: KotlinVar
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

                    constClass.addImport(enumPackage)
                    recordClass.addImport(enumPackage)
                }

                newVar = KotlinVar(fieldNameJavaStyle, enumName)
                newVar.defaultValue = enumName + "." + field.enumerationDefault
            } else if (!field.enumerationClass.isEmpty()) {
                // use user defined class
                val enumClassName = field.enumerationClass

                newVar = KotlinVar(fieldNameJavaStyle, enumClassName)
                newVar.defaultValue = enumClassName + "." + field.enumerationDefault
            } else {
                // ENUM without a foreign key table
                val javaStyleFieldName = field.getName(true)
                val firstChar = javaStyleFieldName.substring(0, 1).toUpperCase()
                val enumName = firstChar + javaStyleFieldName.substring(1)

                if (field.enumValues != null && !field.enumValues.isEmpty()) {
                    if (useInnerEnums) {
                        recordClass.addEnum(enumName, field.enumValues)
                    } else {
                        enumerationClasses.add(KotlinEnum(enumName, field.enumValues))
                    }
                }

                newVar = KotlinVar(enumName, fieldNameJavaStyle)
                newVar.defaultValue = enumName + "." + field.enumerationDefault
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

        // create the variable object
        val newVar = KotlinVar(fieldNameJavaStyle, typeText)

        // set the default value
        if (field.isNotNull) {
            // NOT NULL
            if (fieldDefaultValue.isNotBlank()) {
                newVar.defaultValue = KotlinClass.formatDefaultValue(newVar.dataType, fieldDefaultValue)
            } else {
                newVar.defaultValue = field.jdbcDataType.kotlinDefaultValue
            }
        } else {
            // NULLABLE
            if (fieldDefaultValue.isNotBlank()) {
                newVar.defaultValue = KotlinClass.formatDefaultValue(newVar.dataType, fieldDefaultValue)
            } else {
                newVar.defaultValue = "null"
            }
        }

        return newVar
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