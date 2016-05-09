/*
 * KotlinAndroidRecordClassRenderer.kt
 *
 * Created on Sep 9, 2010
 *
 * Copyright 2010 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.gen.android.kotlin

import org.dbtools.codegen.kotlin.KotlinClass
import org.dbtools.codegen.kotlin.KotlinVal
import org.dbtools.gen.GenConfig
import org.dbtools.gen.android.AndroidBaseRecordRenderer
import org.dbtools.schema.schemafile.SchemaEntity
import org.dbtools.schema.schemafile.SchemaEntityType
import java.text.SimpleDateFormat
import java.util.*

class KotlinAndroidRecordRenderer(val genConfig: GenConfig) {

    private val myClass = KotlinClass()

    fun generate(entity: SchemaEntity, packageName: String) {
        val baseClassName = AndroidBaseRecordRenderer.createClassName(false, entity.className)
        val className = createClassName(entity)
        myClass.apply {
            name = className
            this.packageName = packageName
            extends = baseClassName
        }
        // header comment
        val now = Date()
        val dateFormat = SimpleDateFormat("MM/dd/yyyy hh:mm:ss")
        var fileHeaderComment: String
        fileHeaderComment = "/*\n"
        fileHeaderComment += " * $className.kt\n"
        fileHeaderComment += " *\n"
        fileHeaderComment += " * Created: " + dateFormat.format(now) + "\n"
        fileHeaderComment += " */\n"
        myClass.fileHeaderComment = fileHeaderComment

        if (!myClass.isEnum()) {
            myClass.addImport("android.database.Cursor")
            myClass.addImport("org.dbtools.android.domain.database.contentvalues.DBToolsContentValues")
            myClass.addConstructor()
            myClass.addConstructor(listOf(KotlinVal("cursor", "Cursor")), "setContent(cursor)")
            myClass.addConstructor(listOf(KotlinVal("values", "DBToolsContentValues<*>")), "setContent(values)")
        }

        if (entity.type == SchemaEntityType.VIEW) {
            createView(entity)
        }

        if (entity.type == SchemaEntityType.QUERY) {
            createQuery(entity)
        }
    }

    private fun createView(entity: SchemaEntity) {
        val entityClassName = createClassName(entity) + "Const"

        myClass.addConstant("DROP_VIEW", dataType = "String", formatDefaultValue = false)
        myClass.appendStaticInitializer("DROP_VIEW = \"DROP VIEW IF EXISTS \" + $entityClassName.TABLE + \"\"")

        val headerComment = StringBuilder()
        headerComment.append("// todo Replace the following the CREATE_VIEW sql (The following is a template suggestion for your view)\n")
        headerComment.append("// todo SUGGESTION: Keep the \" AS ").append(entityClassName).append(".<columnname>\" portion of the sql")
        myClass.classHeaderComment = headerComment.toString()


        if (genConfig.isSqlQueryBuilderSupport) {
            createSqlBuilderView(entity)
        } else {
            createStandardView(entity)
        }
    }

    private fun createStandardView(entity: SchemaEntity) {
        val entityClassName = createClassName(entity) + "Const"

        val createContent = StringBuilder()
        createContent.append("\"CREATE VIEW IF NOT EXISTS \" + ").append(entityClassName).append(".TABLE + \" AS SELECT \" +\n")

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

        myClass.addConstant("CREATE_VIEW", createContent.toString(), formatDefaultValue = false)
    }

    private fun createQuery(entity: SchemaEntity) {
        if (genConfig.isSqlQueryBuilderSupport) {
            createSQLBuilderQuery(entity)
        } else {
            createStandardQuery(entity)
        }
    }

    private fun createStandardQuery(entity: SchemaEntity) {
        val entityClassName = createClassName(entity) + "Const"
        val headerComment = StringBuilder()
        headerComment.append("// todo Replace the following the QUERY sql (The following is a template suggestion for your query)\n")
        headerComment.append("// todo BE SURE TO KEEP THE OPENING AND CLOSING PARENTHESES (so queries can be run as sub-select: select * from (select a, b from t) )\n")
        headerComment.append("// todo SUGGESTION: Keep the \" AS ").append(entityClassName).append(".<columnname>\" portion of the sql")
        myClass.classHeaderComment = headerComment.toString()

        val createContent = StringBuilder()
        createContent.append("\"(\" +\n")
        createContent.append(TAB).append(TAB).append(TAB).append("\"SELECT \" +\n")

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
        createContent.append("\" FROM SOME TABLE(S)\" +\n")
        createContent.append(TAB).append(TAB).append(TAB)
        createContent.append("\")\"")

        myClass.addConstant("QUERY", createContent.toString(), formatDefaultValue = false)
        myClass.addConstant("QUERY_RAW", "\"SELECT * FROM \" + QUERY", formatDefaultValue = false)
    }

    private fun createSqlBuilderView(entity: SchemaEntity) {
        val entityClassName = createClassName(entity) + "Const"

        val createContent = StringBuilder()
        createContent.append("\"CREATE VIEW IF NOT EXISTS \" + ").append(entityClassName).append(".TABLE + \" AS \" +\n")

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

        myClass.addConstant("CREATE_VIEW", dataType = "String", formatDefaultValue = false)
        myClass.appendStaticInitializer("\nCREATE_VIEW = " + createContent.toString())

        myClass.addFun("getDropSql", "String", content = "return DROP_VIEW")
        myClass.addFun("getCreateSql", "String", content = "return CREATE_VIEW")

    }

    private fun createSQLBuilderQuery(entity: SchemaEntity) {
        val entityClassName = createClassName(entity) + "Const"
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
            val entityConstClassName = entityClassName + "Const"
            createContent.append(".field(")
            createContent.append(entityConstClassName).append(".").append("FULL_C_").append(fieldConstName)
            createContent.append(", ")
            createContent.append(entityConstClassName).append(".").append("C_").append(fieldConstName)
            createContent.append(")")
        }

        createContent.append("\n")
        createContent.append(TAB).append(TAB).append(TAB)
        createContent.append(".table(\"FROM SOME TABLE(S)\")\n")
        createContent.append(".buildQuery()\n")

        myClass.addConstant("QUERY", dataType = "String", formatDefaultValue = false)
        myClass.appendStaticInitializer("\nQUERY = " + createContent.toString())
    }

    fun writeToFile(directoryName: String) {
        myClass.writeToDisk(directoryName)
    }

    companion object {
        private val TAB = KotlinClass.tab

        fun createClassName(entity: SchemaEntity): String {
            return entity.className
        }
    }
}
/**
 * Creates a new instance of AndroidRecordRenderer.
 */
