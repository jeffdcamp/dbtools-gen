package org.dbtools.gen.android.kotlin

import org.dbtools.codegen.kotlin.KotlinClass
import org.dbtools.codegen.kotlin.KotlinVal
import org.dbtools.gen.GenConfig
import org.dbtools.schema.schemafile.DatabaseSchema
import org.dbtools.util.JavaUtil

class KotlinDatabaseManagerRenderer(val genConfig: GenConfig, val outDir: String = "") {

    private var myClass = KotlinClass()

    private val className = "DatabaseManager"
    private val dbConstClassName = "DatabaseManagerConst";

    private var packageBase: String = ""

    fun generate(databaseSchema: DatabaseSchema) {
        println("Generating DatabaseManager...")

        myClass = KotlinClass(className, packageBase)
        myClass.apply {
            extends = "DatabaseBaseManager" // extend the generated base class
        }

        myClass.addVal("application", "Application")

        val params = listOf(KotlinVal("application", "Application"))
        val constructorContent = "this.application = application"
        val constructor = myClass.addConstructor(params, constructorContent)

        if (genConfig.isInjectionSupport) {
            myClass.addAnnotation("Singleton")
            constructor.addAnnotation("javax.inject.Inject")
        }
        addImports()
        createIdentifyDatabases(databaseSchema)
        createCreateNewDatabaseWrapper()
        createOnUpgrade()
        createOnUpgradeViews()

        myClass.writeToDisk(outDir, false)
    }

    private fun createIdentifyDatabases(databaseSchema: DatabaseSchema) {
        val content = StringBuilder()

        for (database in databaseSchema.databases) {
            val databaseConstName = JavaUtil.nameToJavaConst(database.name) + "_DATABASE_NAME"
            val databaseVersion = database.name + "TablesVersion"
            val databaseViewsVersion = database.name + "ViewsVersion"

            content.append("addDatabase(application, $dbConstClassName.$databaseConstName, $databaseVersion, $databaseViewsVersion)\n")

            myClass.addVal(databaseVersion, defaultValue = "1")
            myClass.addVal(databaseViewsVersion, defaultValue = "1")
        }

        myClass.addFun("identifyDatabases", content = content.toString()).apply {
            isOverride = true
        }
    }

    private fun createCreateNewDatabaseWrapper() {
        myClass.addImport("org.dbtools.android.domain.database.DatabaseWrapper")
        myClass.addImport("org.dbtools.android.domain.database.AndroidDatabaseWrapper")

        myClass.addFun("createNewDatabaseWrapper", "DatabaseWrapper<*>", listOf(KotlinVal("androidDatabase", "AndroidDatabase")), "return AndroidDatabaseWrapper(androidDatabase.path)").apply {
            isOverride = true
        }
    }

    private fun createOnUpgrade() {
        val content = StringBuilder()

        content.append("Log.i(TAG, \"Upgrading database [\$androidDatabase.name] from version \$oldVersion to \$newVersion\")\n")

        val params = listOf(KotlinVal("androidDatabase", "AndroidDatabase"),
                KotlinVal("oldVersion", "Int"),
                KotlinVal("newVersion", "Int"))
        myClass.addFun("onUpgrade", parameters = params, content = content.toString()).apply {
            isOverride = true
        }
    }

    private fun createOnUpgradeViews() {
        val content = StringBuilder()

        content.append("Log.i(TAG, \"Upgrading database [\$androidDatabase.name] VIEWS from version \$oldVersion to \$newVersion\")\n")
        content.append("// automatically drop/create views\n")
        content.append("super.onUpgradeViews(androidDatabase, oldVersion, newVersion)\n")

        val params = listOf(KotlinVal("androidDatabase", "AndroidDatabase"),
                KotlinVal("oldVersion", "Int"),
                KotlinVal("newVersion", "Int"))
        myClass.addFun("onUpgradeViews", parameters = params, content = content.toString()).apply {
            isOverride = true
        }
    }

    private fun addImports() {
        myClass.addImport("android.util.Log")
        myClass.addImport("android.app.Application")
        myClass.addImport("org.dbtools.android.domain.AndroidDatabase")

        if (genConfig.isInjectionSupport) {
            myClass.addImport("javax.inject.Inject")
            myClass.addImport("javax.inject.Singleton")
        }
    }

    fun setPackageBase(packageBase: String) {
        this.packageBase = packageBase
    }
}
