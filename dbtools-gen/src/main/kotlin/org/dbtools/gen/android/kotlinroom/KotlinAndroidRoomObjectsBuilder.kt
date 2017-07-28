package org.dbtools.gen.android.kotlinroom

import org.dbtools.gen.DBObjectBuilder
import org.dbtools.gen.DBObjectsBuilder
import org.dbtools.gen.GenConfig
import org.dbtools.gen.android.kotlin.KotlinAppDatabaseConfigRenderer
import org.dbtools.gen.android.kotlin.KotlinDatabaseBaseManagerRenderer
import org.dbtools.gen.android.kotlin.KotlinDatabaseManagerRenderer
import org.dbtools.schema.schemafile.DatabaseSchema

class KotlinAndroidRoomObjectsBuilder(genConfig: GenConfig) : DBObjectsBuilder(genConfig) {
    val builder = KotlinAndroidRoomDBObjectBuilder()

    override fun getObjectBuilder(): DBObjectBuilder? {
        return builder
    }

    fun buildAll(schemaFilename: String, baseOutputDir: String, basePackageName: String) {
        println("schmaFilename: $schemaFilename")
        println("baseOutputDir: $baseOutputDir")
        println("basePackageName: $basePackageName")

        xmlFilename = schemaFilename
        outputBaseDir = baseOutputDir
        packageBase = basePackageName

        build()
        println("Generated [" + builder.numberFilesGenerated + "] files.")
    }

    override fun onPostBuild(databaseSchema: DatabaseSchema, packageBase: String, outputBaseDir: String, genConfig: GenConfig) {
        val databaseBaseManager = KotlinRoomDatabaseRenderer(genConfig, outputBaseDir)
        databaseBaseManager.generate(databaseSchema)
    }
}
