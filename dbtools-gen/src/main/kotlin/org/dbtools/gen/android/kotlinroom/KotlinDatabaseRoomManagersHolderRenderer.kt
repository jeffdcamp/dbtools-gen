package org.dbtools.gen.android.kotlinroom

import org.dbtools.codegen.kotlin.KotlinClass
import org.dbtools.gen.android.AndroidRecordRenderer
import org.dbtools.schema.schemafile.SchemaDatabase
import org.dbtools.schema.schemafile.SchemaEntity
import org.dbtools.schema.schemafile.SchemaQuery
import org.dbtools.schema.schemafile.SchemaTable
import org.dbtools.schema.schemafile.SchemaView
import org.dbtools.util.JavaUtil
import java.util.ArrayList

class KotlinDatabaseRoomManagersHolderRenderer {
    private var packageName: String = ""
    private var myClass = KotlinClass()
    private val entityList = ArrayList<String>()

    fun generate(database: SchemaDatabase, packageBase: String, packageName: String, tables: List<SchemaTable>, views: List<SchemaView>, queries: List<SchemaQuery>, outDir: String) {
        println("Generating Room Database...")

        this.packageName = packageName

        var preName = database.getName(true).toLowerCase()

        // uppercase the first letter
        preName = Character.toString(preName[0]).toUpperCase() + preName.substring(1)

        val className = preName + "RoomDatabase"
        myClass = KotlinClass(className, packageName)
        myClass.apply {
            abstract = true
            extends = "RoomDatabase()"
        }

        myClass.addConstant("DATABASE_NAME", defaultValue = preName, dataType = "String")

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

        myClass.addImport("android.arch.persistence.room.Database")
        myClass.addImport("android.arch.persistence.room.RoomDatabase")


        myClass.addAnnotation("@Database(entities = arrayOf(${entityList.joinToString(",")}), version = 1)")


        myClass.writeToDisk(outDir, true)
    }

    private fun addSchemaEntityToInit(initContent: StringBuilder, entity: SchemaEntity) {
        val daoClassName = AndroidRecordRenderer.createClassName(entity) + "Dao"
        val daoFunctionName = Character.toString(daoClassName[0]).toLowerCase() + daoClassName.substring(1)

        myClass.addFun(daoFunctionName, returnType = daoClassName).apply {
            abstract = true
        }

        myClass.addImport(JavaUtil.createTablePackageName(packageName, entity.className) + "." + daoClassName)

        val entityClassname = AndroidRecordRenderer.createClassName(entity)
        myClass.addImport(JavaUtil.createTablePackageName(packageName, entity.className) + "." + entityClassname)
        entityList.add("\n        $entityClassname::class")
    }
}
