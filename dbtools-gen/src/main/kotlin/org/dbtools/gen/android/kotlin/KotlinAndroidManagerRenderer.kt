/*
 * KotlinAndroidRecordManager.kt
 *
 * Created on Sep 9, 2010
 *
 * Copyright 2010 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.gen.android.kotlin

import org.dbtools.codegen.kotlin.KotlinClass
import org.dbtools.codegen.kotlin.KotlinVal
import org.dbtools.gen.GenConfig
import org.dbtools.gen.android.AndroidRecordRenderer
import org.dbtools.schema.schemafile.SchemaEntity
import org.dbtools.schema.schemafile.SchemaEntityType
import java.text.SimpleDateFormat
import java.util.*

class KotlinAndroidManagerRenderer(val genConfig: GenConfig) {

    private val myClass = KotlinClass()

    /**
     * Creates a new instance of AndroidManagerRenderer.
     */
    fun generate(entity: SchemaEntity, packageName: String) {
        val className = getClassName(entity)
        myClass.apply {
            this.packageName = packageName
            name = className
            extends = KotlinAndroidBaseManagerRenderer.getClassName(entity)
        }

        // header comment
        val now = Date()
        val dateFormat = SimpleDateFormat("MM/dd/yyyy hh:mm:ss")
        var fileHeaderComment: String
        fileHeaderComment = "/*\n"
        fileHeaderComment += " * $className.kt\n"
        fileHeaderComment += " *\n"
        fileHeaderComment += " * Generated on: " + dateFormat.format(now) + "\n"
        fileHeaderComment += " *\n"
        fileHeaderComment += " */\n"
        myClass.fileHeaderComment = fileHeaderComment

        // Injection support
        if (genConfig.isInjectionSupport) {
            myClass.addAnnotation("javax.inject.Singleton")
        }

        // constructor
        var databaseManagerPackage = packageName.substring(0, packageName.lastIndexOf('.'))
        if (genConfig.isIncludeDatabaseNameInPackage) {
            databaseManagerPackage = databaseManagerPackage.substring(0, databaseManagerPackage.lastIndexOf('.'))
        }
        myClass.addImport(databaseManagerPackage + ".DatabaseManager")
        val defaultConstructor = myClass.addConstructor(listOf(KotlinVal("databaseManager", "DatabaseManager"))).apply {
            constructorDelegate = "super(databaseManager)"
        }
        if (genConfig.isInjectionSupport) {
            defaultConstructor.addAnnotation("javax.inject.Inject")
        }

        if (entity.type == SchemaEntityType.QUERY) {
            val recordClassName = AndroidRecordRenderer.createClassName(entity)
            myClass.addFun("getQuery", "String", content = "return $recordClassName.QUERY").apply {
                isOverride = true
            }
        }
    }

    fun writeToFile(outDir: String) {
        myClass.writeToDisk(outDir)
    }

    companion object {
        fun getClassName(entity: SchemaEntity): String {
            val recordClassName = AndroidRecordRenderer.createClassName(entity)
            return recordClassName + "Manager"
        }
    }
}
