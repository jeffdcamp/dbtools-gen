package org.dbtools.gen.android.kotlin

import org.dbtools.codegen.kotlin.KotlinClass
import org.dbtools.codegen.kotlin.KotlinVal
import org.dbtools.gen.GenConfig
import org.dbtools.schema.schemafile.DatabaseSchema

class KotlinDatabaseManagerRenderer(val genConfig: GenConfig, val outDir: String = "") {

    private var myClass = KotlinClass()

    private val className = "DatabaseManager"

    private var packageBase: String = ""

    fun generate(databaseSchema: DatabaseSchema) {
        println("Generating DatabaseManager...")

        myClass = KotlinClass(className, packageBase)
        myClass.apply {
            extends = "DatabaseBaseManager" // extend the generated base class
        }

        val params = listOf(KotlinVal("databaseConfig", "DatabaseConfig"))
        val constructor = myClass.addConstructor(params, returnType = "super(databaseConfig)")

        if (genConfig.isInjectionSupport) {
            myClass.addAnnotation("Singleton")
            constructor.addAnnotation("javax.inject.Inject")
        }
        addImports()
        createDatabaseVersions(databaseSchema)
        createOnUpgrade()
        createOnUpgradeViews()

        myClass.writeToDisk(outDir, false)
    }

    private fun createDatabaseVersions(databaseSchema: DatabaseSchema) {
        for (database in databaseSchema.databases) {
            val databaseVersion = database.name + "TablesVersion"
            val databaseViewsVersion = database.name + "ViewsVersion"

            myClass.addConstant(databaseVersion, defaultValue = "1")
            myClass.addConstant(databaseViewsVersion, defaultValue = "1")
        }
    }

    private fun createOnUpgrade() {
        val content = StringBuilder()

        content.append("getLogger().i(TAG, \"Upgrading database [\$androidDatabase.name] from version \$oldVersion to \$newVersion\")\n")

        val params = listOf(KotlinVal("androidDatabase", "AndroidDatabase"),
                KotlinVal("oldVersion", "Int"),
                KotlinVal("newVersion", "Int"))
        myClass.addFun("onUpgrade", parameters = params, content = content.toString()).apply {
            isOverride = true
        }
    }

    private fun createOnUpgradeViews() {
        val content = StringBuilder()

        content.append("getLogger().i(TAG, \"Upgrading database [\$androidDatabase.name] VIEWS from version \$oldVersion to \$newVersion\")\n")
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
        myClass.addImport("org.dbtools.android.domain.config.DatabaseConfig")
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
