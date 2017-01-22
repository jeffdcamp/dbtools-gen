/*
 * KotlinClass.kt
 *
 * Created on Nov 7, 2015
 *
 * Copyright 2007 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.codegen.kotlin

import org.dbtools.codegen.java.JavaClass
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.PrintStream
import java.util.*

@SuppressWarnings("PMD.UseStringBufferForStringAppends")
open class KotlinClass(var name: String = "", var packageName:String = "", val classType: KotlinClassType = KotlinClassType.CLASS) {
    var fileHeaderComment = ""
    var classHeaderComment = ""
    var access = KotlinAccess.PUBLIC
    var abstract = false
    var isFinal = false
    var extends = ""
    var staticInit = ""
    val annotations = ArrayList<String>()
    val implementsInterfaces = ArrayList<String>()
    val imports = ArrayList<String>()
    private val enums = ArrayList<KotlinInnerEnum>()
    private val vars = ArrayList<KotlinVar>()
    private val vals = ArrayList<KotlinVal>()
    private val constantVals = ArrayList<KotlinVal>()
    private val constantFuns = ArrayList<KotlinFun>()
    private val constructors = ArrayList<KotlinFun>()
    private val functions = ArrayList<KotlinFun>()
    // vars for generator
    var createDefaultConstructor = false

    fun addImport(newImport: String) {
        if (!imports.contains(newImport)) {
            imports.add(newImport)
        }
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

    fun addImplements(className: String) {
        if (className.isEmpty()) {
            throw IllegalArgumentException("className for Implements cannot be null or empty")
        }

        implementsInterfaces.add(className)
    }

    fun addEnum(enumName: String, enumValues: List<String>): KotlinInnerEnum {
        return addEnum(KotlinInnerEnum(enumName, enumValues))
    }

    fun addEnum(newEnum: KotlinInnerEnum): KotlinInnerEnum {
        enums.add(newEnum)

        return newEnum
    }

    fun addVar(newVariable: KotlinVar): KotlinVar {
        vars.add(newVariable)
        return newVariable
    }

    fun addVar(name: String, datatype: String, defaultValue: String = ""): KotlinVar {
        val newVariable = KotlinVar(name, datatype)
        newVariable.defaultValue = defaultValue

        return addVar(newVariable)
    }

    fun addVal(newVal: KotlinVal): KotlinVal {
        vals.add(newVal)
        return newVal
    }

    fun addVal(name: String, datatype: String = "", defaultValue: String = ""): KotlinVal {
        val newVal = KotlinVal(name, datatype)
        newVal.defaultValue = defaultValue

        return addVal(newVal)
    }

    fun addConstant(name: String, defaultValue: String = "", dataType: String = "", formatDefaultValue: Boolean = true): KotlinVal {
        val constant = KotlinVal(name, dataType)
        constant.access = KotlinAccess.PUBLIC
        constant.setDefaultValue(defaultValue, formatDefaultValue)

        constantVals.add(constant)

        return constant
    }

    fun isPrimitive(dataType: String) =
            dataType == "Int" ||
            dataType == "Boolean" ||
            dataType == "Long" ||
            dataType == "Float" ||
            dataType == "Double" ||
            dataType == "Char" ||
            dataType == "Short" ||
            dataType == "Byte"

    fun addStaticFun(function: KotlinFun) {
        function.startEndTab = tab
//        function.tab = tab + tab
        constantFuns.add(function)
    }

    fun addConstructor(parameters: List<KotlinVal> = ArrayList<KotlinVal>(), content: String = "", access: KotlinAccess = KotlinAccess.PUBLIC, returnType: String = ""): KotlinFun {
        if (isInterface()) {
            throw IllegalStateException("Cannot add a constructor to an Interface")
        }

        return addFun(name, returnType, parameters, content, access, KotlinFunType.CONSTRUCTOR)
    }

    fun addFun(newFun: KotlinFun): KotlinFun {
        when (newFun.funType) {
            KotlinFunType.CONSTRUCTOR -> constructors.add(newFun)
            KotlinFunType.STANDARD -> functions.add(newFun)
        }

        return newFun
    }

    fun addFun(name: String,
                       returnType: String = "",
                       parameters: List<KotlinVal> = ArrayList<KotlinVal>(),
                       content: String = "",
                       access: KotlinAccess = KotlinAccess.PUBLIC,
                       funType: KotlinFunType = KotlinFunType.STANDARD): KotlinFun {
        val newFun = KotlinFun(name, parameters, returnType, content, access, funType)
        addFun(newFun)

        return newFun
    }

    // ===================  BUILD METHODS  ================
    private fun buildPackage(): String {
        return "package $packageName\n\n"
    }

    private fun buildImports(): String {
        if (imports.isEmpty()) {
            return ""
        } else {
            val out = StringBuilder()
            for (newImport in imports) {
                out.append("import ").append(newImport).append("\n")
            }

            out.append("\n")

            return out.toString()
        }
    }

    private fun buildClassHeader(genericsTypeVar: String?): String {
        val classHeader = StringBuilder()
        classHeader.append("\n")

        // annotations
        for (annotation in annotations) {
            classHeader.append(annotation).append("\n")
        }

        // generics
        val genericsVar = (if (genericsTypeVar == null) "" else "<$genericsTypeVar>")

        // extends
        var extendsNameString = ""
        if (!extends.isEmpty()) {
            extendsNameString = " : " + extends
        }

        // implements
        var implementsNamesString = ""
        if (!implementsInterfaces.isEmpty()) {
            if (extendsNameString.isEmpty()) {
                implementsNamesString = " : "
            } else {
                implementsNamesString = ", "
            }

            for (i in implementsInterfaces.indices) {
                if (i > 0) {
                    implementsNamesString += ", " // nopmd - generator
                }

                implementsNamesString += implementsInterfaces[i] // nopmd - generator
            }
        }

        // generate header
        val accessText = getAccessString(access)
        classHeader.append(accessText)
        if (!accessText.isEmpty()) {
            classHeader.append(" ")
        }

        if (abstract) {
            classHeader.append("abstract ")
        }

        if (isFinal) {
            classHeader.append("final ")
        }

        when (classType) {
            KotlinClassType.CLASS -> classHeader.append("class ").append(name).append(genericsVar)
            KotlinClassType.INTERFACE -> classHeader.append("interface ").append(name).append(genericsVar)
            KotlinClassType.ENUM -> classHeader.append("enum class ").append(name).append(genericsVar)
            KotlinClassType.OBJECT -> classHeader.append("object ").append(name).append(genericsVar)
            KotlinClassType.DATA -> classHeader.append("data class ").append(name).append(genericsVar)
        }

        classHeader.append(extendsNameString)
        classHeader.append(implementsNamesString)
        classHeader.append(" {\n")

        return classHeader.toString()
    }

    private fun buildEnums(): String {
        var enumsText = ""
        val TAB = tab

        for (enumItem in enums) {
            enumsText += TAB + enumItem.toString() + "\n"
        }

        enumsText += "\n"

        return enumsText
    }

    private fun buildConstants(out: StringBuilder) {
        if (constantVals.isEmpty() && constantFuns.isEmpty() && staticInit.isBlank()) {
            return
        }

        if (classType == KotlinClassType.OBJECT) {
            for (constantVal in constantVals) {
                out.append(constantVal.toString()).append("\n")
            }
            for (constantFun in constantFuns) {
                out.append(constantFun.toString()).append("\n")
            }

            // static initializer
            if (staticInit.isNotBlank()) {
                out.append(tab).append(tab)
                        .append("init {\n")
                        .append("$tab$tab")
                        .append(staticInit)
                        .append("\n$tab$tab}\n")
            }
        } else {
            // standard class
            out.append(tab).append("companion object {\n")
            for (constantVal in constantVals) {
                out.append(tab).append(constantVal.toString()).append("\n")
            }
            for (constantFun in constantFuns) {
                out.append(constantFun.toString()).append("\n")
            }

            // static initializer
            if (staticInit.isNotBlank()) {
                out.append(tab).append(tab)
                        .append("init {\n")
                        .append("$tab$tab")
                        .append(staticInit)
                        .append("\n$tab$tab}\n")
            }
            out.append(tab).append("}").append("\n")
        }

    }

    private fun buildVariables(): String {
        var variablesText = ""

        for (variable in vals) {
            variablesText += variable.toString() + "\n"
        }

        for (variable in vars) {
            variablesText += variable.toString() + "\n"
        }

        variablesText += "\n"

        return variablesText
    }

    override fun toString(): String {
        val out = StringBuilder()

        if (fileHeaderComment.length > 0) {
            out.append(fileHeaderComment).append("\n\n")
        }

        out.append("\n")
        out.append(buildPackage())
        out.append(buildImports())

        if (classHeaderComment.length > 0) {
            out.append(classHeaderComment).append("\n\n")
        }

        out.append(buildClassHeader(null)) // TODO... get rid of first parameter
        out.append(buildPostClassHeader()) // Support for ENUM type
        out.append(buildEnums())
        buildConstants(out)
        out.append(buildVariables())
        buildMethods(out)


        // end of class
        out.append("\n}")

        return out.toString()
    }

    private fun buildMethods(out: StringBuilder) {
        // constructor methods
        if (!isInterface()) {
            if (createDefaultConstructor) {
                addConstructor()
            }

            for (constructor in constructors) {
                out.append(constructor.toString())
                out.append("\n")
            }
        }

        // regular functions
        for (functions in functions) {
            out.append(functions.toString(isInterface()))
            out.append("\n")
        }
    }

    val filename: String
        get() = name + ".kt"

    @JvmOverloads fun writeToDisk(directoryname: String, overwrite: Boolean = true) {
        val directory = File(directoryname)
        directory.mkdirs()

        try {
            val outFile = File(directoryname + "/" + filename)

            if (!overwrite && outFile.exists()) {
                return
            }

            val fps = PrintStream(FileOutputStream(outFile))
            fps.print(this.toString())
            fps.close()
        } catch (ex: FileNotFoundException) {
            ex.printStackTrace()
        }

    }

    fun isInterface() = classType == KotlinClassType.INTERFACE
    fun isEnum() = classType == KotlinClassType.ENUM

    protected open fun buildPostClassHeader(): String {
        return ""
    }

    fun appendStaticInitializer(code: String) {
        staticInit += code
    }

    companion object {
        var tab = "    "

        fun getAccessString(access: KotlinAccess): String {
            when (access) {
                KotlinAccess.DEFAULT_NONE -> return ""
                KotlinAccess.PUBLIC -> return ""
                KotlinAccess.PRIVATE -> return "private"
                KotlinAccess.PROTECTED -> return "protected"
                else -> throw IllegalArgumentException("Illegal Access type: " + access.toString())
            }
        }

        fun formatConstant(constant: String): String {
            val newConst = StringBuilder()

            for (i in 0..constant.length - 1) {
                // add the current character in UPPERCASE
                newConst.append(Character.toUpperCase(constant[i]))

                // check for need of _
                val current = constant[i]
                var nextChar = ' '
                if ((i + 1) < constant.length) {
                    nextChar = constant[i + 1]
                }

                if (!Character.isUpperCase(current) && nextChar != ' ' && Character.isUpperCase(nextChar)) {
                    newConst.append('_')
                }
            }

            return newConst.toString()
        }

        fun formatDefaultValue(fieldType: String?, defaultValue: String?): String {
            var defaultValue = defaultValue
            var newDefaultValue = ""

            if (defaultValue == null || defaultValue.equals("NULL", ignoreCase = true)) {
                defaultValue = null // nopmd
            }

            if (fieldType == null) {
                return "null"
            }

            if (fieldType == "String") {
                if (defaultValue == null) {
                    newDefaultValue = "\"\""
                } else {
                    newDefaultValue = "\"" + defaultValue + "\""
                }
            } else if (fieldType == "int" || fieldType == "long" || fieldType == "float" || fieldType == "double") {
                if (defaultValue == null || defaultValue == "") {
                    newDefaultValue = "0"
                } else {
                    newDefaultValue = defaultValue
                }
            } else if (fieldType == "Integer" || fieldType == "Long" || fieldType == "Float" || fieldType == "Double") {
                if (defaultValue == null || defaultValue == "") {
                    newDefaultValue = "null"
                } else {
                    newDefaultValue = defaultValue
                }
            } else if (fieldType == "char" || fieldType == "Character") {
                if (defaultValue == null || defaultValue == "") {
                    newDefaultValue = "''"
                } else {
                    defaultValue = "'$defaultValue'"
                }
            } else if (fieldType == "boolean" || fieldType == "Boolean") {
                if (defaultValue == null || defaultValue == "") {
                    newDefaultValue = "false"
                } else {
                    if (defaultValue == "1") {
                        newDefaultValue = "true"
                    } else if (defaultValue == "0") {
                        newDefaultValue = "false"
                    } else {
                        newDefaultValue = defaultValue
                    }
                }
            } else if (fieldType == "Date" && defaultValue != null && defaultValue.equals("NOW", ignoreCase = true)) {
                newDefaultValue = "new Date()"
            } else if (fieldType == "BigInteger") {
                if (defaultValue != null) {
                    newDefaultValue = "new java.math.BigInteger(\"$defaultValue\")"
                } else {
                    newDefaultValue = "new java.math.BigInteger(\"0\")"
                }
            } else if (fieldType == "BigDecimal") {
                if (defaultValue != null) {
                    newDefaultValue = "new java.math.BigDecimal($defaultValue)"
                } else {
                    newDefaultValue = "new java.math.BigDecimal(0)"
                }
            } else if (defaultValue != null && defaultValue.length > 0) {
                newDefaultValue = defaultValue
            } else {
                newDefaultValue = "null"
            }

            return newDefaultValue
        }

        /**
         * Determines package name for specified path.
         */
        fun createPackageFromFilePath(filepath: String): String {
            // if source path contains "kotlin" then assume this is the source path (for maven projects)
            if (filepath.contains("src/main/kotlin")) {
                return createPackageFromFilePath(filepath, "src.main.kotlin")
            } else if (filepath.contains("src\\main\\kotlin")) {
                return createPackageFromFilePath(filepath, "src.main.kotlin")
            }
            if (filepath.contains("src/kotlin")) {
                return createPackageFromFilePath(filepath, "src.kotlin")
            } else if (filepath.contains("src\\kotlin")) {
                return createPackageFromFilePath(filepath, "src.kotlin")
            } else if (filepath.contains("src")) {
                return createPackageFromFilePath(filepath, "src")
            } else {
                //if (filepath.indexOf("source") != -1)
                return createPackageFromFilePath(filepath, "source")
            }
        }

        fun createPackageFromFilePath(filepath: String?, srcDirName: String): String {
            var dotFilepath = ""
            var packageName = ""

            if (filepath == null || filepath == "") {
                return ""
            }

            // change / or \\ to .
            for (i in 0..filepath.length - 1) {
                val c = filepath[i]

                if (c == '\\' || c == '/') {
                    dotFilepath += '.'
                } else {
                    dotFilepath += c
                }
            }

            // find source or src part of directory
            val start = dotFilepath.indexOf(srcDirName)
            if (start > 0) {
                packageName = dotFilepath.substring(start + srcDirName.length)
            } else {
                packageName = dotFilepath
            }

            // on windows... get rid of drive letter and :
            if (packageName.length >= 2 && packageName[1] == ':') {
                packageName = packageName.substring(2)
            }

            // remove any starting  .'s
            if (packageName.length > 0 && packageName[0] == '.') {
                packageName = packageName.substring(1)
            }

            // remove any leading .'s
            if (packageName.length > 0 && packageName[packageName.length - 1] == '.') {
                packageName = packageName.substring(0, packageName.length - 1)
            }

            return packageName
        }

        /**
         * Method that makes sure that a string is kotlin bean compliant.  Makes
         * sure that first letter is lowercase

         * @param items Incoming String(s)
         * *
         * @return String that is adjusted to a proper kotlin variable standards
         */
        fun formatToKotlinVar(vararg items: String): String {
            var formattedName = ""

            var firstItem = true
            for (item in items) {
                if (firstItem) {
                    formattedName += item.substring(0, 1).toLowerCase() + item.substring(1)
                    firstItem = false
                } else {
                    formattedName += item.substring(0, 1).toUpperCase() + item.substring(1)
                }
            }

            return formattedName
        }

        /**
         * Method that makes sure that a string is kotlin bean compliant.  Makes
         * sure that first letter is upper case

         * @param items Incoming String(s)
         * *
         * @return String that is adjusted to a proper kotlin variable standards
         */
        fun formatToJavaMethod(vararg items: String): String {
            var formattedName = ""

            var firstItem = true
            for (item in items) {
                if (firstItem) {
                    formattedName += item.substring(0, 1).toLowerCase() + item.substring(1)
                    firstItem = false
                } else {
                    formattedName += item.substring(0, 1).toUpperCase() + item.substring(1)
                }
            }

            return formattedName
        }

        fun formatToJavaVariable(tableClassName: String): String {
            return JavaClass.formatToJavaVariable(tableClassName)
        }
    }
}
