/*
 * KotlinDataType.kt
 *
 * Created on Nov 7, 2015
 *
 * Copyright 2007 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.codegen.kotlin

/**
 * @author Jeff
 */
enum class KotlinDataType private constructor() {

    STRING, INT, BOOLEAN, FLOAT, DOUBLE, CHAR, DATE, OBJECT;


    companion object {
        fun getDataType(type: String): KotlinDataType {
            if (type.equals("String", ignoreCase = true)) {
                return STRING
            } else if (type.equals("Int", ignoreCase = true)) {
                return INT
            } else if (type.equals("Boolean", ignoreCase = true)) {
                return BOOLEAN
            } else if (type.equals("Float", ignoreCase = true)) {
                return FLOAT
            } else if (type.equals("Double", ignoreCase = true)) {
                return DOUBLE
            } else if (type.equals("Char", ignoreCase = true)) {
                return CHAR
            } else if (type.equals("java.util.Date", ignoreCase = true)) {
                return DATE
            } else {
                return OBJECT
            }
        }
    }
}
