/*
 * PackageUtil.java
 *
 * Created on April 16, 2007
 *
 * Copyright 2007 Jeff Campbell. All rights reserved. Unauthorized reproduction 
 * is a violation of applicable law. This material contains certain 
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.util;

import java.util.regex.Matcher;

/**
 * @author Jeff
 */
@SuppressWarnings("PMD.UseStringBufferForStringAppends")
public final class PackageUtil {

    private PackageUtil() {
    }

    /**
     * Determines package name for specified path.
     *
     * @param filepath
     * @return
     */
    @SuppressWarnings("PMD.UseStringBufferForStringAppends")
    public static String getPackageFromFilePath(String filepath) {
        String packageName;
        // if source path contains "java" then assume this is the source path (for maven projects)
        if (filepath.contains("src/main/java")) {
            packageName = getPackageFromFilePath(filepath, "src.main.java");
        } else if (filepath.contains("src\\main\\java")) {
            packageName = getPackageFromFilePath(filepath, "src.main.java");
        } else if (filepath.contains("src/java")) {
            packageName = getPackageFromFilePath(filepath, "src.java");
        } else if (filepath.contains("src\\java")) {
            packageName = getPackageFromFilePath(filepath, "src.java");
        } else if (filepath.contains("src")) {
            packageName = getPackageFromFilePath(filepath, "src");
        } else { //if (filepath.indexOf("source") != -1) 
            packageName = getPackageFromFilePath(filepath, "source");
        }

        return packageName;
    }

    public static String getTestBaseDir(String filepath) {
        String testBaseDir = "";
        if (filepath.contains("src/main/java")) {
            testBaseDir = replaceText(filepath, "src/main/java", "src/test/java");
        } else if (filepath.contains("src\\main\\java")) {
            testBaseDir = replaceText(filepath, "src\\\\main\\\\java", "src\\test\\java");
        }
        if (filepath.contains("src/java")) {
            testBaseDir = replaceText(filepath, "src/java", "src/test");
        } else if (filepath.contains("src\\java")) {
            testBaseDir = replaceText(filepath, "src\\\\java", "src\\test");
        } else if (filepath.contains("src")) {
            testBaseDir = replaceText(filepath, "src", "test");
        }

        return testBaseDir;
    }

    private static String replaceText(String sourceText, String search, String replace) {
        String replacedText = Matcher.quoteReplacement(sourceText).replaceAll(search, replace);

        return replacedText;
    }

    public static String getPackageFromFilePath(String filepath, String srcDirName) {
        String dotFilepath = "";
        String packageName = "";

        if (filepath == null || filepath.equals("")) {
            return "";
        }

        // change / or \\ to .
        for (int i = 0; i < filepath.length(); i++) {
            char c = filepath.charAt(i);

            if (c == '\\' || c == '/') {
                dotFilepath += '.';
            } else {
                dotFilepath += c;
            }
        }

        // find source or src part of directory
        int start = dotFilepath.indexOf(srcDirName);
        if (start > 0) {
            packageName = dotFilepath.substring(start + srcDirName.length());
        } else {
            packageName = dotFilepath;
        }

        // on windows... get rid of drive letter and :
        if (packageName.length() >= 2 && packageName.charAt(1) == ':') {
            packageName = packageName.substring(2);
        }

        // remove any starting  .'s
        if (packageName.length() > 0 && packageName.charAt(0) == '.') {
            packageName = packageName.substring(1);
        }

        // remove any leading .'s
        if (packageName.length() > 0 && packageName.charAt(packageName.length() - 1) == '.') {
            packageName = packageName.substring(0, packageName.length() - 1);
        }

        return packageName;
    }
}
