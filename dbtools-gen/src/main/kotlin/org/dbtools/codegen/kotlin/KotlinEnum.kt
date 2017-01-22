/*
 * KotlinEnum.kt
 *
 * Created on Nov 7, 2015
 *
 * Copyright 2007 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */

package org.dbtools.codegen.kotlin

import java.util.*

@SuppressWarnings("PMD.UseStringBufferForStringAppends")
class KotlinEnum : KotlinClass {

    private val enums = ArrayList<String>()

    constructor(name: String, enums: List<String>) : super(name, classType = KotlinClassType.ENUM) {
        setEnums(enums)
    }

    constructor(name: String, packageName: String, enums: List<String>) : super(name, packageName, KotlinClassType.ENUM) {
        setEnums(enums)
    }

    fun getEnums(): List<String> {
        return Collections.unmodifiableList(enums)
    }

    fun setEnums(enums: List<String>) {
        if (enums.isEmpty()) {
            throw IllegalArgumentException("enums cannot be null or empty")
        }

        this.enums.clear()
        this.enums.addAll(enums)
    }

    override fun buildPostClassHeader(): String {
        var enumStr = ""

        var count = 0
        for (enumItem in enums) {
            if (count > 0) {
                enumStr += ", "
            }

            enumStr += enumItem

            count++
        }

        enumStr += ";\n"

        return enumStr
    }
}
