package org.dbtools.gen.android.kotlinroom

import org.dbtools.codegen.kotlin.KotlinClass
import org.dbtools.codegen.kotlin.KotlinVal
import org.dbtools.gen.GenConfig
import org.dbtools.schema.schemafile.DatabaseSchema
import org.dbtools.util.JavaUtil

class KotlinRoomDatabaseRenderer(val genConfig: GenConfig, val outDir: String = "") {

    private var myClass = KotlinClass()

    private val className = "DatabaseManager"

    private var packageBase: String = ""

    fun generate(databaseSchema: DatabaseSchema) {
        println("Generating Database...")

        myClass = KotlinClass(className, packageBase)
        myClass.apply {
            extends = "RoomDatabase" // extend the generated base class
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

//        myClass.writeToDisk(outDir, false)
    }

    private fun createDatabaseVersions(databaseSchema: DatabaseSchema) {
        for (database in databaseSchema.databases) {
            val databaseVersion = JavaUtil.nameToJavaConst("${database.getName(true)}TablesVersion")
            val databaseViewsVersion = JavaUtil.nameToJavaConst("${database.getName(true)}ViewsVersion")

            myClass.addConstant(databaseVersion, defaultValue = "1").apply { const = true }
            myClass.addConstant(databaseViewsVersion, defaultValue = "1").apply { const = true }
        }
    }

    private fun createOnUpgrade() {
        val content = StringBuilder()

        content.append("getLogger().i(TAG, \"Upgrading database [\${androidDatabase.name}] from version \$oldVersion to \$newVersion\")\n")

        val params = listOf(KotlinVal("androidDatabase", "AndroidDatabase"),
                KotlinVal("oldVersion", "Int"),
                KotlinVal("newVersion", "Int"))
        myClass.addFun("onUpgrade", parameters = params, content = content.toString()).apply {
            override = true
        }
    }

    private fun createOnUpgradeViews() {
        val content = StringBuilder()

        content.append("getLogger().i(TAG, \"Upgrading database [\${androidDatabase.name}] VIEWS from version \$oldVersion to \$newVersion\")\n")
        content.append("// automatically drop/create views\n")
        content.append("super.onUpgradeViews(androidDatabase, oldVersion, newVersion)\n")

        val params = listOf(KotlinVal("androidDatabase", "AndroidDatabase"),
                KotlinVal("oldVersion", "Int"),
                KotlinVal("newVersion", "Int"))
        myClass.addFun("onUpgradeViews", parameters = params, content = content.toString()).apply {
            override = true
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
