/*
 * KotlinAndroidRoomDaoRenderer.kt
 *
 * Created on Jun 5, 2017
 *
 * Copyright 2017 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.gen.android.kotlinroom


import org.dbtools.codegen.kotlin.KotlinClass
import org.dbtools.gen.GenConfig
import org.dbtools.gen.android.AndroidGeneratedEntityInfo
import org.dbtools.gen.android.AndroidRecordRenderer
import org.dbtools.schema.schemafile.SchemaEntity

class KotlinAndroidRoomDaoRenderer(val genConfig: GenConfig) {
    private val myClass = KotlinClass()

    fun generate(entity: SchemaEntity, packageName: String, generatedEntityInfo: AndroidGeneratedEntityInfo) {
        val entityClassname = AndroidRecordRenderer.createClassName(entity)
        val className = getClassName(entity)
        myClass.apply {
            name = className
            this.packageName = packageName
            abstract = true
        }

        // generate all of the main methods
        createDao(entity, packageName, entityClassname, generatedEntityInfo)
    }

    private fun createDao(entity: SchemaEntity, packageName: String, recordClassName: String, generatedEntityInfo: AndroidGeneratedEntityInfo) {
        myClass.addImport("android.arch.persistence.room.Dao")
        myClass.addAnnotation("@Dao")
    }


    fun writeToFile(outDir: String) {
        myClass.writeToDisk(outDir)
    }

    companion object {
        fun getClassName(table: SchemaEntity): String {
            val recordClassName = AndroidRecordRenderer.createClassName(table)
            return recordClassName + "Dao"
        }
    }
}
