/*
 * KotlinFun.kt
 *
 * Created on Nov 7, 2015
 *
 * Copyright 2007 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.codegen.kotlin

import java.util.*

class KotlinFun {

    var funType = KotlinFunType.STANDARD
    var returnType = "" // void / Unit
    var name = ""
    var funAccess = KotlinAccess.PRIVATE
    var isAbstract = false
    var isInline = false
    var isLateInit = false
    var isOverride = false
    var isOpen = false
    val parameters = ArrayList<KotlinVal>()
    val annotations = ArrayList<String>()
    val exceptions = ArrayList<String>()
    var content = ""
    var tab = KotlinClass.tab
    var startEndTab = ""; // some cases there needs to be an extra tab (such as companion object)
    var constructorDelegate = "";

    constructor(name: String, parameters: List<KotlinVal> = ArrayList<KotlinVal>(), returnType: String = "", content: String = "", funAccess: KotlinAccess = KotlinAccess.PUBLIC, funType: KotlinFunType = KotlinFunType.STANDARD) {
        for (parameter in parameters) {
            parameter.variableType = KotlinVarType.METHOD_PARAMETER
        }
        this.parameters.addAll(parameters)

        this.funAccess = funAccess
        this.funType = funType
        this.returnType = returnType
        this.name = name
        this.content = content
    }

    override fun toString(): String {
        return toString(false)
    }

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

    fun toString(interfaceOnly: Boolean): String {
        var text = ""

        for (annotation in annotations) {
            text += tab + annotation + "\n"
        }

        // access
        val access = KotlinClass.getAccessString(funAccess)
        if (!access.isEmpty()) {
            text += tab + access
        } else {
            text += tab
        }

        // modifiers
        if (isOpen) {
            text += "open "
        }

        if (isOverride) {
            text += "override "
        }

        if (isAbstract) {
            text += "abstract "
        }

        if (isInline) {
            text += "inline "
        }

        if (isLateInit) {
            text += "isLateinit "
        }

        // method name
        if (funType == KotlinFunType.STANDARD) {
            text += startEndTab + "fun $name"
        } else if (funType == KotlinFunType.CONSTRUCTOR) {
            text += "constructor"
        }


        // parameters
        text += "("
        var paramCounter = 0
        for (parameter in parameters) {
            if (paramCounter > 0) {
                text += ", "
            }
            text += parameter.toString()

            paramCounter++
        }
        text += ")"

        // constructor delegate
        if (constructorDelegate.isNotEmpty()) {
            text += " : $constructorDelegate"
        }

        // return
        if (returnType.isNotEmpty()) {
            text += " : $returnType"
        }

        // exceptions
        if (!exceptions.isEmpty()) {
            text += " throws"

            var expCount = 0
            for (exception in exceptions) {
                if (expCount == 0) {
                    text += " "
                } else {
                    text += ", "
                }

                text += exception
                expCount++
            }
        }

        if (interfaceOnly || isAbstract) {
            text += ";\n"
            return text
        } else {
            // content
            text += " {\n"

            if (!content.isEmpty()) {
                val splitContent = content.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (aSplitContent in splitContent) {
                    text += startEndTab + tab + tab + aSplitContent + "\n"
                }
            }

            text += startEndTab + tab + "}\n"
        }

        return text
    }

    fun addParameter(parameter: KotlinVal) {
        parameter.variableType = KotlinVarType.CLASS_VARIABLE
        parameters.add(parameter)
    }

    fun setParameters(parameters: ArrayList<KotlinVal>) {
        for (parameter in parameters) {
            parameter.variableType = KotlinVarType.METHOD_PARAMETER
        }

        this.parameters.clear()
        this.parameters.addAll(parameters)
    }

    fun addThrowsException(exceptionClass: String) {
        if (exceptionClass.isEmpty()) {
            throw IllegalArgumentException("exceptionClass cannot be empty or null")
        }

        exceptions.add(exceptionClass)
    }
}
