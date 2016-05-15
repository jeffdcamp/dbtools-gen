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

class KotlinVar(val name: String, val dataType: String, var defaultValue: String = "") {
    var variableType = KotlinVarType.CLASS_VARIABLE
    var access = KotlinAccess.PUBLIC
    var inline = false
    var lateInit = false
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
            val accessText = KotlinClass.getAccessString(access) + " "
            text += KotlinClass.tab
            text += accessText
        }

        if (inline) {
            text += "inline "
        }

        if (lateInit) {
            text += "lateinit "
        }

        if (variableType == KotlinVarType.CLASS_VARIABLE) {
            text += "var "
        }

        // datatype and name
//        if (!text.isEmpty()) {
//            text += " "
//        }

        text += "$name: $dataType"

        // set default value
        if (!defaultValue.isEmpty()) {
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

    fun getGetterMethodName(): String {
        val methodVarName = createBeanMethodName(name)
        if ((dataType == "boolean" || dataType == "Boolean")) {
            return "is" + methodVarName
        } else {
            return "get" + methodVarName
        }
    }

    fun createBeanMethodName(varName: String): String {
        return varName.substring(0, 1).toUpperCase() + varName.substring(1)
    }

}
