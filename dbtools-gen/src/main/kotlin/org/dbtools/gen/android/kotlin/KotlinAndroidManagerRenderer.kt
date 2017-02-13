/*
 * KotlinAndroidRecordManager.kt
 *
 * Created on Sep 9, 2010
 *
 * Copyright 2010 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.gen.android.kotlin

import org.dbtools.codegen.kotlin.KotlinClass
import org.dbtools.gen.GenConfig
import org.dbtools.gen.android.AndroidRecordRenderer
import org.dbtools.schema.schemafile.SchemaEntity
import org.dbtools.schema.schemafile.SchemaEntityType
import java.text.SimpleDateFormat
import java.util.Date

class KotlinAndroidManagerRenderer(val genConfig: GenConfig) {

    private val myClass = KotlinClass()
    private val TAB = KotlinClass.tab

    /**
     * Creates a new instance of AndroidManagerRenderer.
     */
    fun generate(entity: SchemaEntity, packageName: String) {
        val className = getClassName(entity)
        myClass.apply {
            this.packageName = packageName

            name = className

            // primary constructor
            if (genConfig.isInjectionSupport) {
                primaryConstructor = "@Inject constructor(databaseManager: DatabaseManager)"
            } else {
                primaryConstructor = "constructor(databaseManager: DatabaseManager)"
            }

            extends = KotlinAndroidBaseManagerRenderer.getClassName(entity) + "(databaseManager)"
        }

        // header comment
        val now = Date()
        val dateFormat = SimpleDateFormat("MM/dd/yyyy hh:mm:ss")
        var fileHeaderComment: String
        fileHeaderComment = "/*\n"
        fileHeaderComment += " * $className.kt\n"
        fileHeaderComment += " *\n"
        fileHeaderComment += " * Generated on: " + dateFormat.format(now) + "\n"
        fileHeaderComment += " *\n"
        fileHeaderComment += " */\n"
        myClass.fileHeaderComment = fileHeaderComment

        // Injection support
        if (genConfig.isInjectionSupport) {
            myClass.addImport("javax.inject.Inject")
            myClass.addAnnotation("javax.inject.Singleton")
        }

        // constructor DatabaseManager import
        var databaseManagerPackage = packageName.substring(0, packageName.lastIndexOf('.'))
        if (genConfig.isIncludeDatabaseNameInPackage) {
            databaseManagerPackage = databaseManagerPackage.substring(0, databaseManagerPackage.lastIndexOf('.'))
        }
        myClass.addImport(databaseManagerPackage + ".DatabaseManager")

        when (entity.type) {
            SchemaEntityType.VIEW -> createView(entity)
            SchemaEntityType.QUERY -> createQuery(entity)
        }

    }

    private fun createView(entity: SchemaEntity) {
        val entityClassName = KotlinAndroidRecordRenderer.createClassName(entity) + "Const"

        myClass.addConstant("DROP_VIEW", dataType = "String",
                defaultValue = "\"DROP VIEW IF EXISTS \" + $entityClassName.TABLE",
                formatDefaultValue = false).apply { const = true }

        // begin header
        val headerComment = StringBuilder()
        headerComment.append("// todo Replace the following the CREATE_VIEW sql (The following is a template suggestion for your view)\n")


        if (genConfig.isSqlQueryBuilderSupport) {
            createSqlBuilderView(entity)
            headerComment.append("// todo SUGGESTION: Keep the second parameter of each filter(<replace>, <keep>)")
        } else {
            createStandardView(entity)
            headerComment.append("// todo SUGGESTION: Keep the \" AS ").append(entityClassName).append(".<columnname>\" portion of the sql")
        }

        myClass.classHeaderComment = headerComment.toString()
    }

    private fun createStandardView(entity: SchemaEntity) {
        val entityClassName = KotlinAndroidRecordRenderer.createClassName(entity) + "Const"

        val createContent = StringBuilder()
        createContent.append("\"CREATE VIEW IF NOT EXISTS \" + $entityClassName.TABLE + \" AS SELECT \" +\n")

        for (i in 0..entity.fields.size - 1) {
            if (i > 0) {
                createContent.append(" + \", \" +\n")
            }

            createContent.append(TAB).append(TAB).append(TAB)
            val schemaField = entity.fields[i]

            val fieldConstName = KotlinClass.formatConstant(schemaField.getName(true))
            val entityConstClassName = entityClassName
            createContent.append(entityConstClassName).append(".").append("FULL_C_").append(fieldConstName)
            createContent.append(" + \" AS \" + ")
            createContent.append(entityConstClassName).append(".").append("C_").append(fieldConstName)
        }

        createContent.append(" +\n")
        createContent.append(TAB).append(TAB).append(TAB)
        createContent.append("\" FROM \" + ").append(entityClassName).append(".TABLE")

        myClass.addConstant("CREATE_VIEW", createContent.toString(), formatDefaultValue = false).apply { const = true }
    }

    private fun createQuery(entity: SchemaEntity) {
        myClass.addFun("getQuery", "String", content = "return QUERY").apply {
            override = true
        }

        if (genConfig.isSqlQueryBuilderSupport) {
            createSQLBuilderQuery(entity)
        } else {
            createStandardQuery(entity)
        }
    }

    private fun createStandardQuery(entity: SchemaEntity) {
        val entityClassName = KotlinAndroidRecordRenderer.createClassName(entity) + "Const"
        val headerComment = StringBuilder()
        headerComment.append("// todo Replace the following the QUERY sql (The following is a template suggestion for your query)\n")
        headerComment.append("// todo SUGGESTION: Keep the \" AS ").append(entityClassName).append(".<columnname>\" portion of the sql")
        myClass.classHeaderComment = headerComment.toString()

        val createContent = StringBuilder()
        createContent.append(" \"SELECT \" +\n")

        for (i in 0..entity.fields.size - 1) {
            if (i > 0) {
                createContent.append(" + \", \" +\n")
            }

            createContent.append(TAB).append(TAB).append(TAB)
            val schemaField = entity.fields[i]

            val fieldConstName = KotlinClass.formatConstant(schemaField.getName(true))
            createContent.append(entityClassName).append(".").append("FULL_C_").append(fieldConstName)
            createContent.append(" + \" AS \" + ")
            createContent.append(entityClassName).append(".").append("C_").append(fieldConstName)
        }

        createContent.append(" +\n")
        createContent.append(TAB).append(TAB).append(TAB)
        createContent.append("\" FROM SOME TABLE(S)\"")

        myClass.addConstant("QUERY", defaultValue = createContent.toString(), formatDefaultValue = false).apply { const = true }
    }

    private fun createSqlBuilderView(entity: SchemaEntity) {
        val entityClassName = KotlinAndroidRecordRenderer.createClassName(entity) + "Const"

        val createContent = StringBuilder()
        createContent.append("\"CREATE VIEW IF NOT EXISTS \" + $entityClassName.TABLE + \" AS \" +\n")

        createContent.append(TAB).append(TAB).append(TAB)
        myClass.addImport("org.dbtools.query.sql.SQLQueryBuilder")
        createContent.append("SQLQueryBuilder()").append("\n")


        for (i in 0..entity.fields.size - 1) {
            if (i > 0) {
                createContent.append("\n")
            }

            createContent.append(TAB).append(TAB).append(TAB)
            val schemaField = entity.fields[i]

            val fieldConstName = KotlinClass.formatConstant(schemaField.getName(true))
            createContent.append(".field(")

            createContent.append(entityClassName).append(".").append("FULL_C_").append(fieldConstName)
            createContent.append(", ")
            createContent.append(entityClassName).append(".").append("C_").append(fieldConstName)
            createContent.append(")")
        }

        createContent.append("\n")
        createContent.append(TAB).append(TAB).append(TAB)
        createContent.append(".table(").append(entityClassName).append(".TABLE)")
        createContent.append("\n")
        createContent.append(TAB).append(TAB).append(TAB)
        createContent.append(".buildQuery()")

        myClass.addConstant("CREATE_VIEW", dataType = "String", defaultValue = createContent.toString(), formatDefaultValue = false).apply { const = true }
    }

    private fun createSQLBuilderQuery(entity: SchemaEntity) {
        val entityClassName = KotlinAndroidRecordRenderer.createClassName(entity) + "Const"
        val headerComment = StringBuilder()
        headerComment.append("// todo Replace the following the QUERY sql (The following is a template suggestion for your query)\n")
        headerComment.append("// todo SUGGESTION: Keep the second parameter of each filter(<replace>, <keep>)")
        myClass.classHeaderComment = headerComment.toString()

        val createContent = StringBuilder()
        myClass.addImport("org.dbtools.query.sql.SQLQueryBuilder")
        createContent.append("SQLQueryBuilder()").append("\n")


        for (i in 0..entity.fields.size - 1) {
            if (i > 0) {
                createContent.append("\n")
            }

            createContent.append(TAB).append(TAB).append(TAB)
            val schemaField = entity.fields[i]

            val fieldConstName = KotlinClass.formatConstant(schemaField.getName(true))
            val entityConstClassName = entityClassName
            createContent.append(".field(")
            createContent.append(entityConstClassName).append(".").append("FULL_C_").append(fieldConstName)
            createContent.append(", ")
            createContent.append(entityConstClassName).append(".").append("C_").append(fieldConstName)
            createContent.append(")")
        }

        createContent.append("\n")
        createContent.append(TAB).append(TAB).append(TAB)
        createContent.append(".table(\"FROM SOME TABLE(S)\")\n")
        createContent.append(TAB).append(TAB).append(TAB)
        createContent.append(".buildQuery()\n")

        myClass.addConstant("QUERY", dataType = "String", defaultValue = createContent.toString(), formatDefaultValue = false).apply { const = true }
    }


    fun writeToFile(outDir: String) {
        myClass.writeToDisk(outDir)
    }

    companion object {
        fun getClassName(entity: SchemaEntity): String {
            val recordClassName = AndroidRecordRenderer.createClassName(entity)
            return recordClassName + "Manager"
        }
    }
}
