/*
 * KotlinAndroidRecordClassRenderer.kt
 *
 * Created on Sep 9, 2010
 *
 * Copyright 2010 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.gen.android.kotlin

import org.dbtools.codegen.kotlin.KotlinClass
import org.dbtools.gen.GenConfig
import org.dbtools.gen.android.AndroidBaseRecordRenderer
import org.dbtools.schema.schemafile.SchemaEntity
import java.text.SimpleDateFormat
import java.util.Date

class KotlinAndroidRecordRenderer(val genConfig: GenConfig) {

    private val myClass = KotlinClass()

    fun generate(entity: SchemaEntity, packageName: String) {
        val baseClassName = AndroidBaseRecordRenderer.createClassName(false, entity.className)
        val className = createClassName(entity)
        myClass.apply {
            name = className
            this.packageName = packageName
            extends = baseClassName + "()" // call super default constructor
        }
        // header comment
        val now = Date()
        val dateFormat = SimpleDateFormat("MM/dd/yyyy hh:mm:ss")
        var fileHeaderComment: String
        fileHeaderComment = "/*\n"
        fileHeaderComment += " * $className.kt\n"
        fileHeaderComment += " *\n"
        fileHeaderComment += " * Created: " + dateFormat.format(now) + "\n"
        fileHeaderComment += " */\n"
        myClass.fileHeaderComment = fileHeaderComment
    }



    fun writeToFile(directoryName: String) {
        myClass.writeToDisk(directoryName)
    }

    companion object {
        fun createClassName(entity: SchemaEntity): String {
            return entity.className
        }
    }
}
