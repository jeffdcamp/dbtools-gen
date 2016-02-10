/*
 * KotlinVar.kt
 *
 * Created on Nov 7, 2015
 *
 * Copyright 2007 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.codegen.kotlin

import java.util.*

class KotlinVal(val name: String, val dataType: String, var defaultValue: String = "") {
    var variableType = KotlinVarType.CLASS_VARIABLE
    var access = KotlinAccess.PUBLIC
    var inline = false
    var lateInit = false
    var const = false // used with "object" singleton classes
    val annotations = ArrayList<String>()

    fun addAnnotation(annotation: String) {
        if (annotation.isEmpty()) {
            throw IllegalArgumentException("annotation cannot be null or empty")
        }

        if (annotation[0] != '@') {
            annotations.add('@' + annotation)
        } else {
            annotations.add(annotation)
        }
    }

    override fun toString(): String {
        var text = ""

        // annotations
        for (annotation in annotations) {
            // add tab if this is a class Variable
            if (variableType == KotlinVarType.CLASS_VARIABLE) {
                text += KotlinClass.tab
            }

            text += annotation
            when (variableType) {
                KotlinVarType.CLASS_VARIABLE -> text += "\n"
                KotlinVarType.METHOD_PARAMETER -> text += " "
            }
        }

        // access
        if (variableType == KotlinVarType.CLASS_VARIABLE) {
            val accessText = KotlinClass.getAccessString(access)
            text += KotlinClass.tab
            text += accessText
        }

        if (inline) {
            text += "inline "
        }

        if (lateInit) {
            text += "lateinit "
        }

        if (const) {
            text += "const "
        }

        if (variableType == KotlinVarType.CLASS_VARIABLE) {
            text += "val "
        }

        // datatype and name
//        if (!text.isEmpty()) {
//            text += " "
//        }

        if (!dataType.isBlank()) {
            text += "$name: $dataType"
        } else {
            text += name
        }

        // set default value
        if (!defaultValue.isBlank()) {
            text += " = " + defaultValue
        }

        return text
    }

    fun setDefaultValue(defaultValue: String, formatDefaultValue: Boolean) {
        if (formatDefaultValue) {
            this.defaultValue = KotlinClass.formatDefaultValue(dataType, defaultValue)
        } else {
            this.defaultValue = defaultValue
        }
    }
}
