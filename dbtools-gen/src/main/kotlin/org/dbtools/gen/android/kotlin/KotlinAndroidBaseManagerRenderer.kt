/*
 * KotlinAndroidBaseRecordManager.kt
 *
 * Created on Nov 11, 2015
 *
 * Copyright 2015 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.gen.android.kotlin


import org.dbtools.codegen.kotlin.KotlinClass
import org.dbtools.codegen.kotlin.KotlinVal
import org.dbtools.gen.AnnotationConsts
import org.dbtools.gen.GenConfig
import org.dbtools.gen.android.AndroidGeneratedEntityInfo
import org.dbtools.gen.android.AndroidRecordRenderer
import org.dbtools.schema.schemafile.SchemaEntity
import org.dbtools.schema.schemafile.SchemaEntityType
import org.dbtools.schema.schemafile.SchemaTable

class KotlinAndroidBaseManagerRenderer(val genConfig: GenConfig) {
    private val myClass = KotlinClass()

    fun generate(entity: SchemaEntity, packageName: String, generatedEntityInfo: AndroidGeneratedEntityInfo) {
        val recordClassName = AndroidRecordRenderer.createClassName(entity)
        val className = getClassName(entity)
        myClass.apply {
            name = className
            this.packageName = packageName
            abstract = true
        }

        // header comment
        // Do not place date in file because it will cause a new check-in to scm        
        myClass.fileHeaderComment = "/*\n * $className.kt\n *\n * GENERATED FILE - DO NOT EDIT\n * \n */\n"

        // Since this is generated code.... suppress all warnings
        myClass.addAnnotation("@Suppress(\"unused\", \"ConvertSecondaryConstructorToPrimary\")") // kotlin specific
        myClass.addAnnotation("@SuppressWarnings(\"all\")")

        // generate all of the main methods
        createManager(entity, packageName, recordClassName, generatedEntityInfo)
    }

    private fun createManager(entity: SchemaEntity, packageName: String, recordClassName: String, generatedEntityInfo: AndroidGeneratedEntityInfo) {
        val recordConstClassName = "${recordClassName}Const"
        val type = entity.type

        var databaseManagerPackage = packageName.substring(0, packageName.lastIndexOf('.'))
        if (genConfig.isIncludeDatabaseNameInPackage) {
            databaseManagerPackage = databaseManagerPackage.substring(0, databaseManagerPackage.lastIndexOf('.'))
        }

        myClass.addImport(databaseManagerPackage + ".DatabaseManager")
        myClass.addImport("org.dbtools.android.domain.database.DatabaseWrapper")

        when (type) {
            SchemaEntityType.TABLE -> {
                val tableEntity = entity as SchemaTable
                if (tableEntity.isReadonly) {
                    myClass.addImport(if (genConfig.isRxJavaSupport) "org.dbtools.android.domain.RxKotlinAndroidBaseManagerReadOnly" else "org.dbtools.android.domain.KotlinAndroidBaseManagerReadOnly")
                    myClass.extends = if (genConfig.isRxJavaSupport) "RxKotlinAndroidBaseManagerReadOnly<$recordClassName>" else "KotlinAndroidBaseManagerReadOnly<$recordClassName>"
                } else {
                    myClass.addImport(if (genConfig.isRxJavaSupport) "org.dbtools.android.domain.RxKotlinAndroidBaseManagerWritable" else "org.dbtools.android.domain.KotlinAndroidBaseManagerWritable")
                    myClass.extends = if (genConfig.isRxJavaSupport) "RxKotlinAndroidBaseManagerWritable<$recordClassName>" else "KotlinAndroidBaseManagerWritable<$recordClassName>"
                }
            }
            SchemaEntityType.VIEW, SchemaEntityType.QUERY -> {
                myClass.addImport(if (genConfig.isRxJavaSupport) "org.dbtools.android.domain.RxKotlinAndroidBaseManagerReadOnly" else "org.dbtools.android.domain.KotlinAndroidBaseManagerReadOnly")
                myClass.extends = if (genConfig.isRxJavaSupport) "RxKotlinAndroidBaseManagerReadOnly<$recordClassName>" else "KotlinAndroidBaseManagerReadOnly<$recordClassName>"
            }
        }

        myClass.addFun("getDatabaseName", "String", content = "return $recordConstClassName.DATABASE").apply {
            override = true
        }
        myClass.addFun("newRecord", recordClassName, content = "return $recordClassName()").apply {
            override = true
        }

        if (type != SchemaEntityType.QUERY) {
            myClass.addVal("tableName", defaultValue = "$recordConstClassName.TABLE").apply {
                override = true
            }
        }

        myClass.addVal("allColumns", "Array<String>", defaultValue = "$recordConstClassName.ALL_COLUMNS").apply {
            override = true
        }

        val databaseNameParam = KotlinVal("databaseName", "String")
        if (genConfig.isJsr305Support) {
            databaseNameParam.addAnnotation(AnnotationConsts.NONNULL)
        }

        myClass.addImport("org.dbtools.android.domain.database.contentvalues.DBToolsContentValues")
        myClass.addImport("org.dbtools.android.domain.AndroidBaseRecord")

        myClass.addFun("getReadableDatabase", "DatabaseWrapper<in AndroidBaseRecord, in DBToolsContentValues<*>>", listOf(databaseNameParam), "return databaseManager.getReadableDatabase(databaseName)").apply {
            override = true
        }

        myClass.addFun("getWritableDatabase", "DatabaseWrapper<in AndroidBaseRecord, in DBToolsContentValues<*>>", listOf(databaseNameParam), "return databaseManager.getWritableDatabase(databaseName)").apply {
            override = true
        }

        myClass.addFun("getAndroidDatabase", "org.dbtools.android.domain.AndroidDatabase?", listOf(databaseNameParam), "return databaseManager.getDatabase(databaseName)").apply {
            override = true
        }

        myClass.addVar("databaseManager", "DatabaseManager")
        myClass.addConstructor(listOf(KotlinVal("databaseManager", "DatabaseManager")), "this.databaseManager = databaseManager")

        myClass.addFun("getDatabaseConfig", "org.dbtools.android.domain.config.DatabaseConfig", content = "return databaseManager.databaseConfig").apply {
            override = true
        }

        when (type) {
            SchemaEntityType.TABLE -> {
                if (generatedEntityInfo.isPrimaryKeyAdded) {
                    myClass.addVal("primaryKey", defaultValue = "$recordConstClassName.PRIMARY_KEY_COLUMN").apply { override = true }
                } else {
                    myClass.addVal("primaryKey", defaultValue = "\"NO_PRIMARY_KEY\"").apply { override = true }
                }
                myClass.addVal("dropSql", defaultValue =  "$recordConstClassName.DROP_TABLE").apply { override = true }
                myClass.addVal("createSql", defaultValue =  "$recordConstClassName.CREATE_TABLE").apply { override = true }
                myClass.addVal("insertSql", defaultValue =  "$recordConstClassName.INSERT_STATEMENT").apply { override = true }
                myClass.addVal("updateSql", defaultValue =  "$recordConstClassName.UPDATE_STATEMENT").apply { override = true }
            }
            SchemaEntityType.VIEW -> {
                myClass.addVal("primaryKey", defaultValue =  """""""").apply { override = true }
                myClass.addVal("dropSql", defaultValue =  "$recordClassName.DROP_VIEW").apply { override = true }
                myClass.addVal("createSql", defaultValue =  "$recordClassName.CREATE_VIEW").apply { override = true }
                myClass.addVal("insertSql", defaultValue =  """""""").apply { override = true }
                myClass.addVal("updateSql", defaultValue =  """""""").apply { override = true }
            }
            SchemaEntityType.QUERY -> {
                myClass.addFun("getQuery", "String").apply {
                    abstract = true
                }

                myClass.addVal("tableName", defaultValue =  "getQuery()").apply { override = true }
                myClass.addVal("primaryKey", defaultValue =  """""""").apply { override = true }
                myClass.addVal("dropSql", defaultValue =  """""""").apply { override = true }
                myClass.addVal("createSql", defaultValue =  """""""").apply { override = true }
                myClass.addVal("insertSql", defaultValue =  """""""").apply { override = true }
                myClass.addVal("updateSql", defaultValue =  """""""").apply { override = true }
            }
        }
    }


    fun writeToFile(outDir: String) {
        myClass.writeToDisk(outDir)
    }

    companion object {
        private val TAB = KotlinClass.tab

        fun getClassName(table: SchemaEntity): String {
            val recordClassName = AndroidRecordRenderer.createClassName(table)
            return recordClassName + "BaseManager"
        }
    }
}
