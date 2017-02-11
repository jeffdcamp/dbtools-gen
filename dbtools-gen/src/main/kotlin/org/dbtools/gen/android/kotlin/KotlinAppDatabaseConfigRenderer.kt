package org.dbtools.gen.android.kotlin

import org.dbtools.codegen.kotlin.KotlinClass
import org.dbtools.codegen.kotlin.KotlinVal
import org.dbtools.gen.GenConfig
import org.dbtools.schema.schemafile.DatabaseSchema
import org.dbtools.util.JavaUtil

class KotlinAppDatabaseConfigRenderer(val genConfig: GenConfig, val outDir: String = "") {

    private var myClass = KotlinClass()

    private val className = "AppDatabaseConfig"
    private val dbConstClassName = "DatabaseManagerConst"
    private var packageBase: String = ""

    fun generate(databaseSchema: DatabaseSchema) {
        println("Generating KotlinAppDatabaseConfigRenderer...")

        myClass = KotlinClass(className, packageBase)
        myClass.apply {
            addImplements("DatabaseConfig") // extend the generated base class
        }

        myClass.addVal("application", "Application")

        val params = listOf(KotlinVal("application", "Application"))
        val constructorContent = "this.application = application"
        myClass.addConstructor(params, constructorContent)

        addImports()
        createIdentifyDatabases(databaseSchema)
        createCreateNewDatabaseWrapper()
        createNewDBToolsContentValues()
        createNewDBToolsLogger()

        myClass.writeToDisk(outDir, false)
    }

    private fun createIdentifyDatabases(databaseSchema: DatabaseSchema) {
        myClass.addImport("org.dbtools.android.domain.AndroidDatabaseBaseManager")
        val content = StringBuilder()

        for (database in databaseSchema.databases) {
            val databaseConstName = JavaUtil.nameToJavaConst(database.name) + "_DATABASE_NAME"
            val databaseVersion = database.name + "TablesVersion"
            val databaseViewsVersion = database.name + "ViewsVersion"

            content.append("databaseManager.addDatabase(application, $dbConstClassName.$databaseConstName, DatabaseManager.$databaseVersion, DatabaseManager.$databaseViewsVersion)\n")
        }

        myClass.addFun("identifyDatabases", parameters = listOf(KotlinVal("databaseManager", "AndroidDatabaseBaseManager")), content = content.toString()).apply {
            override = true
        }
    }

    private fun createCreateNewDatabaseWrapper() {
        myClass.addImport("org.dbtools.android.domain.database.DatabaseWrapper")
        myClass.addImport("org.dbtools.android.domain.database.AndroidDatabaseWrapper")

        myClass.addFun("createNewDatabaseWrapper", "DatabaseWrapper<*, *>", listOf(KotlinVal("androidDatabase", "AndroidDatabase")), "return AndroidDatabaseWrapper(androidDatabase.path)").apply {
            override = true
        }
    }

    private fun createNewDBToolsLogger() {
        myClass.addImport("org.dbtools.android.domain.log.DBToolsAndroidLogger")
        myClass.addImport("org.dbtools.android.domain.log.DBToolsLogger")
        myClass.addFun("createNewDBToolsLogger", "DBToolsLogger", content = "return DBToolsAndroidLogger()").apply {
            override = true
        }
    }

    private fun createNewDBToolsContentValues() {
        myClass.addImport("org.dbtools.android.domain.database.contentvalues.AndroidDBToolsContentValues")
        myClass.addImport("org.dbtools.android.domain.database.contentvalues.DBToolsContentValues")
        myClass.addFun("createNewDBToolsContentValues", "DBToolsContentValues<*>", content = "return AndroidDBToolsContentValues()").apply {
            override = true
        }
    }

    private fun addImports() {
        myClass.addImport("android.app.Application")
        myClass.addImport("org.dbtools.android.domain.config.DatabaseConfig")
        myClass.addImport("org.dbtools.android.domain.AndroidDatabase")
    }

    fun setPackageBase(packageBase: String) {
        this.packageBase = packageBase
    }
}
