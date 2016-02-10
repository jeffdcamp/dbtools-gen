package org.dbtools.gen.android.kotlin

import org.dbtools.gen.DBObjectBuilder
import org.dbtools.gen.DBObjectsBuilder
import org.dbtools.gen.GenConfig
import org.dbtools.schema.schemafile.DatabaseSchema

class KotlinAndroidObjectsBuilder() : DBObjectsBuilder() {
    val builder = KotlinAndroidDBObjectBuilder()

    override fun getObjectBuilder(): DBObjectBuilder? {
        return builder
    }

    fun buildAll(schemaFilename: String, baseOutputDir: String, basePackageName: String, genConfig: GenConfig) {
        println("schmaFilename: $schemaFilename")
        println("baseOutputDir: $baseOutputDir")
        println("basePackageName: $basePackageName")

        xmlFilename = schemaFilename
        outputBaseDir = baseOutputDir
        packageBase = basePackageName
        setGenConfig(genConfig)

        build()
        println("Generated [" + builder.numberFilesGenerated + "] files.")
    }

    override fun onPostBuild(databaseSchema: DatabaseSchema, packageBase: String, outputBaseDir: String, genConfig: GenConfig) {
        val databaseBaseManager = KotlinDatabaseBaseManagerRenderer(genConfig, outputBaseDir)
        databaseBaseManager.generate(databaseSchema, packageBase)

        val databaseManager = KotlinDatabaseManagerRenderer(genConfig, outputBaseDir)
        databaseManager.setPackageBase(packageBase)
        databaseManager.generate(databaseSchema) // this file will only be created if it does not already exist
    }
}
