/*
 * AndroidDBObjectBuilder.kt
 *
 * Created on Sep 9, 2010
 *
 * Copyright 2010 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.gen.android.kotlin


import org.dbtools.gen.DBObjectBuilder
import org.dbtools.gen.GenConfig
import org.dbtools.gen.android.AndroidManagerRenderer
import org.dbtools.gen.android.AndroidRecordRenderer
import org.dbtools.renderer.SchemaRenderer
import org.dbtools.schema.schemafile.*
import java.io.File
import java.util.*

class KotlinAndroidDBObjectBuilder() : DBObjectBuilder {
    private var filesGeneratedCount = 0
    private val filesGenerated = ArrayList<String>()

    override fun getName(): String {
        return "Kotlin Android Object Builder"
    }

    override fun build(database: SchemaDatabase, entity: SchemaEntity, packageName: String, outDir: String, genConfig: GenConfig): Boolean {
        val lastDirChar = outDir[outDir.length - 1]
        val workingOutDir: String
        if (lastDirChar != File.separatorChar) {
            workingOutDir = outDir + File.separatorChar
        } else {
            workingOutDir = outDir
        }

        val databaseMapping = SchemaRenderer.readXMLTypes(this.javaClass, SchemaRenderer.DEFAULT_TYPE_MAPPING_FILENAME, "sqlite")

        // Entities
        val baseRecordFileName = workingOutDir + AndroidRecordRenderer.createClassName(entity) + ".kt"
        val recordFileName = workingOutDir + AndroidRecordRenderer.createClassName(entity) + ".kt"
        val baseRecordFile = File(baseRecordFileName)
        val recordFile = File(recordFileName)

        // BaseRecord
        val baseRecordClass = KotlinAndroidBaseRecordRenderer(genConfig)
        val generatedEntityInfo = baseRecordClass.generate(database, entity, packageName, databaseMapping)
        baseRecordClass.writeToFile(workingOutDir)

        filesGenerated.add(baseRecordFile.path)
        filesGeneratedCount++

        // Record
        if (!entity.isEnumerationTable) {
            if (!recordFile.exists()) {
                val recordClass = KotlinAndroidRecordRenderer(genConfig)
                recordClass.generate(entity, packageName)
                recordClass.writeToFile(workingOutDir)

                filesGenerated.add(recordFile.path)
                filesGeneratedCount++
            }
        }

        // Managers
        if (!entity.isEnumerationTable) {
            val managerFileName = workingOutDir + AndroidManagerRenderer.getClassName(entity) + ".kt"
            val managerFile = File(managerFileName)

            // Base Manager
            val baseManagerClass = KotlinAndroidBaseManagerRenderer(genConfig)
            baseManagerClass.generate(entity, packageName, generatedEntityInfo)
            baseManagerClass.writeToFile(workingOutDir)
            filesGeneratedCount++

            // Manager
            if (!managerFile.exists()) {
                val managerClass = KotlinAndroidManagerRenderer(genConfig)
                managerClass.generate(entity, packageName)
                managerClass.writeToFile(workingOutDir)
                filesGeneratedCount++
            }
        }
        return true
    }

    override fun getNumberFilesGenerated(): Int {
        return filesGeneratedCount
    }

    override fun getFilesGenerated(): List<String> {
        return Collections.unmodifiableList(filesGenerated)
    }

    override fun buildDatabaseManagersHolder(database: SchemaDatabase, packageBase: String, packageName: String, tables: MutableList<SchemaTable>, views: MutableList<SchemaView>, queries: MutableList<SchemaQuery>, outDir: String) {
        KotlinDatabaseManagersHolderRenderer().generate(database, packageBase, packageName, tables, views, queries, outDir)
    }
}
