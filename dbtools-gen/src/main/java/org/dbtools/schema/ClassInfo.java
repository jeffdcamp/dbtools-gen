/*
 * ClassInfo.java
 *
 * Created on March 27, 2008
 *
 * Copyright 2008 Jeff Campbell. All rights reserved. Unauthorized reproduction 
 * is a violation of applicable law. This material contains certain 
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */

package org.dbtools.schema;

/**
 * @author Jeff
 */
public class ClassInfo {
    private String className;
    private String packageName;

    public ClassInfo(String className, String packageName) {
        this.className = className;
        this.packageName = packageName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageName(String otherEntityPackage) {
        String packageNameToUse;

        if (packageName == null) {
            packageNameToUse = transformPackageName(otherEntityPackage, getClassName());
        } else {
            packageNameToUse = packageName + "." + className.toLowerCase();
        }

        return packageNameToUse;
    }

    public static String transformPackageName(String otherEntityPackage, String newClassname) {
        String newPackagename = "";
        //newPackagename = otherEntityPackage.substring(0, otherEntityPackage.length() - otherEntityClassname.length() - 1) + "." + newClassname.toLowerCase();
        String[] packageElements = otherEntityPackage.split("\\.");
        for (int i = 0; i < packageElements.length - 1; i++) {
            newPackagename += packageElements[i] + ".";
        }

        return newPackagename + newClassname.toLowerCase();
    }

    @Override
    public String toString() {
        String out = "Classname: " + className + "\n";
        out += "Package: " + packageName + "\n";

        return out;
    }

    public static String createJavaStyleName(String tableName) {
        String javaClassnameStyleName = "";
        // check to see if all letters are uppercase
        boolean isAllUppercase = false;
        for (char currentChar : tableName.toCharArray()) {
            if (Character.isUpperCase(currentChar) && Character.isLetter(currentChar)) {
                isAllUppercase = true;
            } else if (Character.isLetter(currentChar)) {
                isAllUppercase = false;
                break;
            }
        }

        String nameToConvert;
        // if all uppercase force lowercase on all letter
        if (isAllUppercase) {
            nameToConvert = tableName.toLowerCase();
        } else {
            nameToConvert = tableName;
        }

        for (int i = 0; i < nameToConvert.length(); i++) {
            char currentChar = nameToConvert.charAt(i);

            // ALWAYS Upper-case the first letter
            if (i == 0) {
                javaClassnameStyleName += Character.toString(currentChar).toUpperCase();
            } else {
                // REMOVE _ and replace next letter with an uppercase letter
                switch (currentChar) {
                    case '_':
                        // move to the next letter
                        i++;
                        currentChar = nameToConvert.charAt(i);

                        javaClassnameStyleName += Character.toString(currentChar).toUpperCase();
                        break;
                    default:
                        javaClassnameStyleName += currentChar;
                }
            }
        }
        return javaClassnameStyleName;
    }
}
