/*
 * KotlinInnerEnum.kt
 *
 * Created on Nov 7, 2015
 *
 * Copyright 2007 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.codegen.kotlin

import org.dbtools.codegen.java.Access
import org.dbtools.codegen.java.JavaClass

import java.util.Collections

/**
 * @author Jeff
 */
@SuppressWarnings("PMD.UseStringBufferForStringAppends")
class KotlinInnerEnum(enumName: String, enumValues: List<String>?) {

    var access = Access.PUBLIC
    var name: String? = null
    var values: List<String>? = null
        get() = Collections.unmodifiableList(values)

    init {
        this.name = enumName

        if (enumValues == null) {
            throw IllegalArgumentException("enumValues cannot be null")
        }
        this.values = enumValues
    }

    override fun toString(): String {
        var enumStr = ""

        val accessText = JavaClass.getAccessString(access)
        enumStr += accessText

        enumStr += " static enum $name {\n        " // includes 2 TABS for the next line

        var numItems = 0
        for (enumItem in values!!) {
            numItems++
            if (numItems > 1) {
                enumStr += ", "
            }
            enumStr += enumItem
        }

        enumStr += "\n    }"

        return enumStr
    }
}
