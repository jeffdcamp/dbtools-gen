/*
 * KotlinAndroidRoomDBObjectBuilder.kt
 *
 * Created on Jun 5, 2017
 *
 * Copyright 2010 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.gen.android.kotlinroom


import org.dbtools.gen.DBObjectBuilder
import org.dbtools.gen.GenConfig
import org.dbtools.gen.android.AndroidRecordRenderer
import org.dbtools.renderer.SchemaRenderer
import org.dbtools.schema.schemafile.SchemaDatabase
import org.dbtools.schema.schemafile.SchemaEntity
import org.dbtools.schema.schemafile.SchemaQuery
import org.dbtools.schema.schemafile.SchemaTable
import org.dbtools.schema.schemafile.SchemaView
import java.io.File
import java.util.ArrayList
import java.util.Collections

class KotlinAndroidRoomDBObjectBuilder : DBObjectBuilder {
    private var filesGeneratedCount = 0
    private val filesGenerated = ArrayList<String>()

    override fun getName(): String {
        return "Kotlin Android Room Object Builder"
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
        val entityFilename = workingOutDir + AndroidRecordRenderer.createClassName(entity) + ".kt"
        val entityFile = File(entityFilename)

        // Entity
        val entityClassRenderer = KotlinAndroidRoomEntityRenderer(genConfig)
        val generatedEntityInfo = entityClassRenderer.generate(database, entity, packageName, databaseMapping)
        entityClassRenderer.writeToFile(workingOutDir)

        filesGenerated.add(entityFile.path)
        filesGeneratedCount++

        // Dao
        val daoFilename = workingOutDir + KotlinAndroidRoomDaoRenderer.getClassName(entity) + ".kt"
        val daoFile = File(daoFilename)
        val daoClassRenderer = KotlinAndroidRoomDaoRenderer(genConfig)
        daoClassRenderer.generate(entity, packageName, generatedEntityInfo)
        daoClassRenderer.writeToFile(workingOutDir)
        filesGeneratedCount++

        return true
    }

    override fun getNumberFilesGenerated(): Int {
        return filesGeneratedCount
    }

    override fun getFilesGenerated(): List<String> {
        return Collections.unmodifiableList(filesGenerated)
    }

    override fun buildDatabaseManagersHolder(database: SchemaDatabase, packageBase: String, packageName: String, tables: MutableList<SchemaTable>, views: MutableList<SchemaView>, queries: MutableList<SchemaQuery>, outDir: String) {
        KotlinDatabaseRoomManagersHolderRenderer().generate(database, packageBase, packageName, tables, views, queries, outDir)
    }
}
