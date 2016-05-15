/*
 * SourceUtil.java
 *
 * Created on April 16, 2007
 *
 * Copyright 2007 Jeff Campbell. All rights reserved. Unauthorized reproduction 
 * is a violation of applicable law. This material contains certain 
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.codegen.java;

/**
 * @author Jeff
 */
@SuppressWarnings("PMD.UseStringBufferForStringAppends")
public class SourceUtil {

    private SourceUtil() {
    }

    public static String getTestBaseDir(String filepath) {
        String testBaseDir = "";
        if (filepath.contains("src/main/java")) {
            testBaseDir = replaceText(filepath, "src/main/java", "src/test/java");
        } else if (filepath.contains("src\\main\\java")) {
            testBaseDir = replaceText(filepath, "src\\main\\java", "src\\test\\java");
        } else if (filepath.contains("src/java")) {
            testBaseDir = replaceText(filepath, "src/java", "src/test");
        } else if (filepath.contains("src\\java")) {
            testBaseDir = replaceText(filepath, "src\\\\java", "src\\test");
        } else if (filepath.contains("src")) {
            testBaseDir = replaceText(filepath, "src", "test");
        }

        return testBaseDir;
    }

    private static String replaceText(String sourceText, String search, String replace) {
        //String replacedText = Matcher.quoteReplacement(sourceText).replaceAll(search, replace);
        String replaceText = "";
        int pos = sourceText.indexOf(search);
        int len = search.length();
        replaceText = sourceText.substring(0, pos);
        replaceText += replace;
        replaceText += sourceText.substring(pos + len);

        return replaceText;
    }
}
