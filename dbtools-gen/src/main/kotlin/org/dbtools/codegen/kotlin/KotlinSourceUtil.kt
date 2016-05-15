/*
 * KotlinSourceUtil.kt
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
@SuppressWarnings("PMD.UseStringBufferForStringAppends")
object KotlinSourceUtil {

    fun getTestBaseDir(filepath: String = ""): String {
        var testBaseDir = ""
        if (filepath.contains("src/main/kotlin")) {
            testBaseDir = replaceText(filepath, "src/main/kotlin", "src/test/kotlin")
        } else if (filepath.contains("src\\main\\kotlin")) {
            testBaseDir = replaceText(filepath, "src\\main\\kotlin", "src\\test\\kotlin")
        } else if (filepath.contains("src/kotlin")) {
            testBaseDir = replaceText(filepath, "src/kotlin", "src/test")
        } else if (filepath.contains("src\\kotlin")) {
            testBaseDir = replaceText(filepath, "src\\\\kotlin", "src\\kotlin")
        } else if (filepath.contains("src")) {
            testBaseDir = replaceText(filepath, "src", "test")
        }

        return testBaseDir
    }

    private fun replaceText(sourceText: String, search: String, replace: String): String {
        //String replacedText = Matcher.quoteReplacement(sourceText).replaceAll(search, replace);
        var replaceText = ""
        val pos = sourceText.indexOf(search)
        val len = search.length
        replaceText = sourceText.substring(0, pos)
        replaceText += replace
        replaceText += sourceText.substring(pos + len)

        return replaceText
    }
}
