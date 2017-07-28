/*
 * KotlinAndroidBaseRecordClassRenderer.kt
 *
 * Created on Sep 9, 2010
 *
 * Copyright 2010 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.gen.android.kotlinroom

import org.dbtools.codegen.kotlin.KotlinClass
import org.dbtools.codegen.kotlin.KotlinVar
import org.dbtools.gen.GenConfig
import org.dbtools.gen.android.AndroidGeneratedEntityInfo
import org.dbtools.schema.dbmappings.DatabaseMapping
import org.dbtools.schema.schemafile.SchemaDatabase
import org.dbtools.schema.schemafile.SchemaEntity
import org.dbtools.schema.schemafile.SchemaEntityType
import org.dbtools.schema.schemafile.SchemaField
import org.dbtools.schema.schemafile.SchemaFieldType


class KotlinAndroidRoomEntityRenderer(val genConfig: GenConfig) {

    private var roomEntityClass = KotlinClass()

    fun generate(database: SchemaDatabase, entity: SchemaEntity, packageName: String, databaseMapping: DatabaseMapping): AndroidGeneratedEntityInfo {
        val generatedEntityInfo = AndroidGeneratedEntityInfo()

        val enumTable = entity.isEnumerationTable
        val entityType = entity.type
        val entityClassName = entity.className

        roomEntityClass.packageName = packageName
        roomEntityClass.name = entityClassName

        val tableName = entity.name
        if (entityType == SchemaEntityType.QUERY) {
            println("*** WARNING: QUERY Tables not yet supported")
            return generatedEntityInfo
        }

        roomEntityClass.addImport("android.arch.persistence.room.Entity")
        roomEntityClass.addAnnotation("@Entity(tableName = \"$tableName\")")

        for (field in entity.fields) {
            val primaryKey = field.isPrimaryKey
            val fieldName = field.name
            val fieldType = field.jdbcDataType
            val notNullField = field.isNotNull
            val primitiveField = fieldType.isJavaTypePrimitive(!field.isNotNull)
            val dateTypeField = fieldType == SchemaFieldType.DATETIME || fieldType == SchemaFieldType.DATE || fieldType == SchemaFieldType.TIMESTAMP || fieldType == SchemaFieldType.TIME

            // override default name
            val fieldNameJavaStyle = field.getName(true)

            val newVariable: KotlinVar
            if (field.isEnumeration) {
                newVariable = generateEnumeration(field, fieldNameJavaStyle, packageName, database)
            } else {
                newVariable = generateFieldVariable(fieldNameJavaStyle, field)
            }

            // use default value for datatype... if possible
            if (!newVariable.defaultValue.isNullOrBlank()) {
                when (newVariable.dataType) {
                    "String", "Int", "Boolean" -> {newVariable.dataType = ""}
                    "Long" -> {
                        newVariable.dataType = ""

                        val defaultValue = newVariable.defaultValue
                        if (!defaultValue.endsWith("L", ignoreCase = true)) {
                            newVariable.defaultValue = defaultValue + "L"
                        }
                    }
                }

                if (field.isEnumeration) {
                    newVariable.dataType = ""
                }
            }

            if (primaryKey) {
                roomEntityClass.addImport("android.arch.persistence.room.PrimaryKey")
                if (field.isIncrement) {
                    newVariable.addAnnotation("@PrimaryKey(autoGenerate = true)")
                } else {
                    newVariable.addAnnotation("@PrimaryKey()")
                }
            }

            if (newVariable.name != fieldName) {
                roomEntityClass.addImport("android.arch.persistence.room.ColumnInfo")
                newVariable.addAnnotation("@ColumnInfo(name = \"$fieldName\")")
            }

            roomEntityClass.addVar(newVariable)


        }

        return generatedEntityInfo
    }

    private fun generateEnumeration(field: SchemaField, fieldNameJavaStyle: String, packageName: String, database: SchemaDatabase): KotlinVar {
        val newVar: KotlinVar
            if (!field.foreignKeyTable.isEmpty()) {
                // define name of enum
                val enumClassInfo = database.getTableClassInfo(field.foreignKeyTable)
                val enumName = enumClassInfo.className

                // we must import the enum
                val enumPackage = enumClassInfo.getPackageName(packageName) + "." + enumName
                roomEntityClass.addImport(enumPackage)

                newVar = KotlinVar(fieldNameJavaStyle, enumName)
                newVar.defaultValue = enumName + "." + field.enumerationDefault
            } else { //if (!field.enumerationClass.isEmpty()) {
                // use user defined class
                val enumClassName = field.enumerationClass

                newVar = KotlinVar(fieldNameJavaStyle, enumClassName)
                newVar.defaultValue = enumClassName + "." + field.enumerationDefault
            }
//            else {
//                // ENUM without a foreign key table
//                val javaStyleFieldName = field.getName(true)
//                val firstChar = javaStyleFieldName.substring(0, 1).toUpperCase()
//                val enumName = firstChar + javaStyleFieldName.substring(1)
//
//                if (field.enumValues != null && !field.enumValues.isEmpty()) {
//                    if (useInnerEnums) {
//                        recordClass.addEnum(enumName, field.enumValues)
//                    } else {
//                        enumerationClasses.add(KotlinEnum(enumName, field.enumValues))
//                    }
//                }
//
//                newVar = KotlinVar(enumName, fieldNameJavaStyle)
//                newVar.defaultValue = enumName + "." + field.enumerationDefault
//            }

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

    fun writeToFile(directoryName: String) {
        roomEntityClass.writeToDisk(directoryName)
    }
}