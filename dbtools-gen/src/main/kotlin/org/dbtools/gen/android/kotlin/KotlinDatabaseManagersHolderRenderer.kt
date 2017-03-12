package org.dbtools.gen.android.kotlin

import org.dbtools.codegen.kotlin.KotlinObjectClass
import org.dbtools.codegen.kotlin.KotlinVal
import org.dbtools.gen.android.AndroidManagerRenderer
import org.dbtools.schema.schemafile.SchemaDatabase
import org.dbtools.schema.schemafile.SchemaEntity
import org.dbtools.schema.schemafile.SchemaQuery
import org.dbtools.schema.schemafile.SchemaTable
import org.dbtools.schema.schemafile.SchemaView
import org.dbtools.util.JavaUtil

class KotlinDatabaseManagersHolderRenderer {
    private var packageName: String = ""
    private var myClass = KotlinObjectClass()

    fun generate(database: SchemaDatabase, packageBase: String, packageName: String, tables: List<SchemaTable>, views: List<SchemaView>, queries: List<SchemaQuery>, outDir: String) {
        println("Generating DatabaseManagersHolder...")

        this.packageName = packageName

        var preName = database.getName(true).toLowerCase()

        // uppercase the first letter
        preName = Character.toString(preName[0]).toUpperCase() + preName.substring(1)

        val className = preName + "DatabaseManagers"
        myClass = KotlinObjectClass(className, packageName)
        myClass.fileHeaderComment = "/*\n * GENERATED FILE - DO NOT EDIT\n */\n"
        myClass.addAnnotation("@SuppressWarnings(\"all\")")

        val initContent = StringBuilder()

        for (table in tables) {
            if (!table.isEnumerationTable) {
                addSchemaEntityToInit(initContent, table)
            }
        }

        for (view in views) {
            addSchemaEntityToInit(initContent, view)
        }

        for (query in queries) {
            addSchemaEntityToInit(initContent, query)
        }

        myClass.addImport("$packageBase.DatabaseManager")

        myClass.addFun("init", parameters = listOf(KotlinVal("databaseManager", "DatabaseManager")), content = initContent.toString())

        myClass.writeToDisk(outDir, true)
    }

    private fun addSchemaEntityToInit(initContent: StringBuilder, entity: SchemaEntity) {
        val managerClassName = AndroidManagerRenderer.getClassName(entity)
        val managerVarName = Character.toString(managerClassName[0]).toLowerCase() + managerClassName.substring(1)

        myClass.addVar(managerVarName, "$managerClassName private set").apply {
            lateInit = true
        }

        initContent.append(managerVarName).append(" = ").append(AndroidManagerRenderer.getClassName(entity)).append("(databaseManager);\n")

        myClass.addImport(JavaUtil.createTablePackageName(packageName, entity.className) + "." + managerClassName)
    }
}
