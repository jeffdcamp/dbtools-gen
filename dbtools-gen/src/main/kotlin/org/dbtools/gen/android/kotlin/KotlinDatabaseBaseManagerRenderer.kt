package org.dbtools.gen.android.kotlin

import org.dbtools.codegen.kotlin.KotlinClass
import org.dbtools.codegen.kotlin.KotlinObjectClass
import org.dbtools.codegen.kotlin.KotlinVal
import org.dbtools.gen.GenConfig
import org.dbtools.gen.android.AndroidRecordRenderer
import org.dbtools.schema.schemafile.DatabaseSchema
import org.dbtools.schema.schemafile.SchemaDatabase
import org.dbtools.schema.schemafile.SchemaEntity
import org.dbtools.util.JavaUtil

class KotlinDatabaseBaseManagerRenderer(val genConfig: GenConfig, val outDir: String = "") {

    private val myClass = KotlinClass()
    private val myConstClass = KotlinObjectClass()
    private val constClassName = "DatabaseManagerConst"
    private var packageBase: String = ""

    fun generate(databaseSchema: DatabaseSchema, packageName: String) {
        println("Generating DatabaseBaseManager...")
        this.packageBase = packageName

        val className = "DatabaseBaseManager"
        myClass.apply { 
            name = className
            this.packageName = packageName
            extends = "AndroidDatabaseManager"
            abstract = true
        }
        myConstClass.apply {
            name = constClassName
            this.packageName = packageName
        }

        addHeader(myClass)
        addHeader(myConstClass)

        addImports()

        // constructor
        myClass.addImport("org.dbtools.android.domain.config.DatabaseConfig")
        myClass.addConstructor(listOf(KotlinVal("databaseConfig", "DatabaseConfig")), returnType = "super(databaseConfig)")
        createOnCreate(databaseSchema)
        createOnCreateViews(databaseSchema)

        myClass.writeToDisk(outDir, true)
        myConstClass.writeToDisk(outDir, true)
    }

    private fun addHeader(someClass: KotlinClass) {
        // Do not place date in file because it will cause a new check-in to scm
        var fileHeaderComment: String
        fileHeaderComment = "/*\n"
        fileHeaderComment += " * ${someClass.name}.kt\n"
        fileHeaderComment += " *\n"
        fileHeaderComment += " * GENERATED FILE - DO NOT EDIT\n"
        fileHeaderComment += " * CHECKSTYLE:OFF\n"
        fileHeaderComment += " * \n"
        fileHeaderComment += " */\n"
        someClass.fileHeaderComment = fileHeaderComment

        // Since this is generated code.... suppress all warnings
        someClass.addAnnotation("@SuppressWarnings(\"all\")")
    }

    private fun addImports() {
        myClass.addImport("org.dbtools.android.domain.AndroidDatabase")
        myClass.addImport("org.dbtools.android.domain.AndroidBaseManager")
        myClass.addImport("org.dbtools.android.domain.AndroidDatabaseManager")
        myClass.addImport("org.dbtools.android.domain.database.DatabaseWrapper")
    }

    private fun createOnCreate(databaseSchema: DatabaseSchema) {
        val content = StringBuilder()
        content.append("getLogger().i(TAG, \"Creating database: \$androidDatabase.name\")\n")

        for (database in databaseSchema.databases) {
            var databaseName = database.name
            databaseName = databaseName.replace(".", "") // remove any periods (example: "mydb.sqlite")

            val databaseConstName = JavaUtil.nameToJavaConst(databaseName) + "_DATABASE_NAME"
            val databaseMethodName = JavaUtil.nameToJavaConst(databaseName) + "_TABLES"
            myConstClass.addConstant(databaseConstName, "\"${database.name}\"" ).apply { const = true }
            createCreateDatabase(content, databaseConstName, databaseMethodName, database)
        }

        myClass.addFun("onCreate", parameters = listOf(KotlinVal("androidDatabase", "AndroidDatabase")), content = content.toString()).apply {
            isOverride = true
        }
    }

    private fun createCreateDatabase(content: StringBuilder, databaseConstName: String, databaseMethodName: String, database: SchemaDatabase) {
        val varName = JavaUtil.sqlNameToJavaVariableName(databaseMethodName)
        val createDatabaseMethodName = "create" + Character.toUpperCase(varName[0]) + varName.substring(1)

        content.append("if (androidDatabase.name.equals($constClassName.$databaseConstName)) {\n")
        content.append(TAB).append(createDatabaseMethodName).append("(androidDatabase)\n")
        content.append("}\n")

        val createDatabaseContent = StringBuilder()
        createDatabaseContent.append("val database = androidDatabase.databaseWrapper\n")
        createDatabaseContent.append("database.beginTransaction()\n")

        // include database name in base package name
        val databaseBasePackage = createDatabaseBasePackage(database)

        createDatabaseContent.append("\n// Enum Tables\n")
        for (table in database.tables) {
            if (table.isEnumerationTable) {
                createDatabaseContent.append("AndroidBaseManager.createTable(database, ")
                        .append(JavaUtil.createTableImport(databaseBasePackage, table.className) + "Const")
                        .append(".CREATE_TABLE)\n")
            }
        }

        createDatabaseContent.append("\n// Tables\n")
        for (table in database.tables) {
            if (!table.isEnumerationTable) {
                createDatabaseContent.append("AndroidBaseManager.createTable(database, ")
                        .append(JavaUtil.createTableImport(databaseBasePackage, table.className) + "Const")
                        .append(".CREATE_TABLE)\n")
            }
        }

        createDatabaseContent.append("\n")
        createDatabaseContent.append("database.setTransactionSuccessful()\n")
        createDatabaseContent.append("database.endTransaction()\n")

        myClass.addFun(createDatabaseMethodName, parameters = listOf(KotlinVal("androidDatabase", "AndroidDatabase")), content = createDatabaseContent.toString())
    }

    private fun createOnCreateViews(databaseSchema: DatabaseSchema) {
        val createContent = StringBuilder()
        val dropContent = StringBuilder()

        createContent.append("getLogger().i(TAG, \"Creating database views: \$androidDatabase.name\")\n")
        dropContent.append("getLogger().i(TAG, \"Dropping database views: \$androidDatabase.name\")\n")

        for (database in databaseSchema.databases) {
            var databaseName = database.name
            databaseName = databaseName.replace(".", "") // remove any periods (example: "mydb.sqlite")

            val databaseConstName = JavaUtil.nameToJavaConst(databaseName) + "_DATABASE_NAME"
            val databaseMethodName = JavaUtil.nameToJavaConst(databaseName) + "_VIEWS"
            createCreateViews(createContent, databaseConstName, databaseMethodName, database)
            createDropViews(dropContent, databaseConstName, databaseMethodName, database)
        }

        val params = listOf(KotlinVal("androidDatabase", "AndroidDatabase"))
        myClass.addFun("onCreateViews", parameters = params, content = createContent.toString()).apply {
            isOverride = true
        }
        myClass.addFun("onDropViews", parameters = params, content = dropContent.toString()).apply {
            isOverride = true
        }
    }

    private fun createCreateViews(content: StringBuilder, databaseConstName: String, databaseMethodName: String, database: SchemaDatabase) {
        if (database.views.isEmpty()) {
            return
        }

        val varName = JavaUtil.sqlNameToJavaVariableName(databaseMethodName)
        val createDatabaseViewsMethodName = "create" + Character.toUpperCase(varName[0]) + varName.substring(1)

        content.append("if (androidDatabase.name.equals($constClassName.$databaseConstName)) {\n")
        content.append(TAB).append(createDatabaseViewsMethodName).append("(androidDatabase)\n")
        content.append("}\n")

        val createDatabaseViewsContent = StringBuilder()

        createDatabaseViewsContent.append("val database = androidDatabase.databaseWrapper\n")
        createDatabaseViewsContent.append("database.beginTransaction()\n")

        // include database name in base package name
        val databaseBasePackage = createDatabaseBasePackage(database)

        createDatabaseViewsContent.append("\n// Views\n")
        for (view in database.views) {
            createDatabaseViewsContent.append("AndroidBaseManager.createTable(database, ")
                    .append(JavaUtil.createTableImport(databaseBasePackage, view.className))
                    .append(".CREATE_VIEW)\n")
        }

        createDatabaseViewsContent.append("\n")
        createDatabaseViewsContent.append("database.setTransactionSuccessful()\n")
        createDatabaseViewsContent.append("database.endTransaction()\n")

        myClass.addFun(createDatabaseViewsMethodName, parameters = listOf(KotlinVal("androidDatabase", "AndroidDatabase")), content = createDatabaseViewsContent.toString())
    }

    private fun createDropViews(content: StringBuilder, databaseConstName: String, databaseMethodName: String, database: SchemaDatabase) {
        if (database.views.isEmpty()) {
            return
        }

        val varName = JavaUtil.sqlNameToJavaVariableName(databaseMethodName)
        val dropDatabaseViewsMethodName = "drop" + Character.toUpperCase(varName[0]) + varName.substring(1)

        content.append("if (androidDatabase.name.equals($constClassName.$databaseConstName)) {\n")
        content.append(TAB).append(dropDatabaseViewsMethodName).append("(androidDatabase)\n")
        content.append("}\n")

        val dropDatabaseViewsContent = StringBuilder()
        dropDatabaseViewsContent.append("val database = androidDatabase.databaseWrapper\n")
        dropDatabaseViewsContent.append("database.beginTransaction()\n")

        // include database name in base package name
        val databaseBasePackage = createDatabaseBasePackage(database)

        dropDatabaseViewsContent.append("\n// Views\n")
        for (view in database.views) {
            dropDatabaseViewsContent.append("AndroidBaseManager.dropTable(database, ")
                    .append(JavaUtil.createTableImport(databaseBasePackage, view.className))
                    .append(".DROP_VIEW)\n")
        }

        dropDatabaseViewsContent.append("\n")
        dropDatabaseViewsContent.append("database.setTransactionSuccessful()\n")
        dropDatabaseViewsContent.append("database.endTransaction()\n")

        myClass.addFun(dropDatabaseViewsMethodName, parameters = listOf(KotlinVal("androidDatabase", "AndroidDatabase")), content = dropDatabaseViewsContent.toString())
    }

    private fun createDatabaseBasePackage(database: SchemaDatabase): String {
        return packageBase + (if (genConfig.isIncludeDatabaseNameInPackage) "." + database.name.toLowerCase() else "")
    }

    fun setPackageBase(packageBase: String) {
        this.packageBase = packageBase
    }

    companion object {
        private val TAB = KotlinClass.tab

        fun getClassName(table: SchemaEntity): String {
            val recordClassName = AndroidRecordRenderer.createClassName(table)
            return recordClassName + "Manager"
        }
    }
}
