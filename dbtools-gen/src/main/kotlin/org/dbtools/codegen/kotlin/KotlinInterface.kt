/*
 * KotlinInterface.kt
 *
 * Created on Nov 7, 2015
 *
 * Copyright 2007 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */

package org.dbtools.codegen.kotlin

class KotlinInterface : KotlinClass {

    constructor(name: String) : super(name, classType = KotlinClassType.INTERFACE) {
    }

    constructor(name: String, packageName: String) : super(name, packageName, KotlinClassType.INTERFACE) {
    }
}
