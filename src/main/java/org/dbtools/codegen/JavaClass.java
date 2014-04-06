/*
 * JavaClass.java
 *
 * Created on August 11, 2006
 *
 * Copyright 2006 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.codegen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jeff
 */
@SuppressWarnings("PMD.UseStringBufferForStringAppends")
public class JavaClass {
    private ClassType classType = ClassType.CLASS;
    private static String tab = "    ";
    private String fileHeaderComment = "";
    private String classHeaderComment = "";
    private String packageName = "";
    private List<String> imports;
    private Access access = Access.PUBLIC;
    private boolean abstractClass = false;
    private boolean staticClass = false;
    private boolean finalClass = false;
    private List<String> annotations;
    private String name;
    private List<String> implementsInterfaces;
    private String extendsClass;
    private String staticInitializerBlock = "";
    private List<JavaInnerEnum> enums;
    private List<JavaVariable> variables;
    private List<JavaMethod> constructors;
    private List<JavaMethod> methods;
    // vars for generator
    private boolean createDefaultConstructor = true;

    public JavaClass(String name) {
        this.setName(name);

        init();
    }

    public JavaClass(String packageName, String name) {
        this.setName(name);
        this.setPackageName(packageName);

        init();
    }

    private void init() {
        this.imports = new ArrayList<String>();
        this.annotations = new ArrayList<String>();
        this.implementsInterfaces = new ArrayList<String>();
        this.enums = new ArrayList<JavaInnerEnum>();
        this.variables = new ArrayList<JavaVariable>();
        this.constructors = new ArrayList<JavaMethod>();
        this.methods = new ArrayList<JavaMethod>();
    }

    public void setDefaultCVSFileHeaderComment() {
        setFileHeaderComment("/*\n");
        setFileHeaderComment(getFileHeaderComment() + " * $Author: jeff $\n");
        setFileHeaderComment(getFileHeaderComment() + " * $RCSfile$\n");
        setFileHeaderComment(getFileHeaderComment() + " * $Revision: 1717 $\n");
        setFileHeaderComment(getFileHeaderComment() + " * $Date: 2006-08-01 22:43:52 -0600 (Tue, 01 Aug 2006) $\n");
        setFileHeaderComment(getFileHeaderComment() + " */\n\n");
    }

    public void setDefaultClassHeaderComment(String shortDescription, String copyright, String author, String version, boolean useCVSDate) {
        setClassHeaderComment("\n/**\n");
        setClassHeaderComment(getClassHeaderComment() + (" * " + shortDescription + "<br>\n"));

        if (useCVSDate) {
            setClassHeaderComment(getClassHeaderComment() + " * CVS last modified: $Date: 2006-08-01 22:43:52 -0600 (Tue, 01 Aug 2006) $<br>\n");
        }

        setClassHeaderComment(getClassHeaderComment() + (" * " + copyright + "\n"));
        setClassHeaderComment(getClassHeaderComment() + " *\n");
        setClassHeaderComment(getClassHeaderComment() + (" * @author " + author + "\n"));
        setClassHeaderComment(getClassHeaderComment() + (" * @version " + version + "\n"));
        setClassHeaderComment(getClassHeaderComment() + " */\n\n");
    }

    public void addImport(String newImport) {
        if (!imports.contains(newImport)) {
            imports.add(newImport);
        }
    }

    public void addAnnotation(String annotation) {
        if (annotation == null || annotation.length() == 0) {
            throw new IllegalArgumentException("annotation cannot be null or empty");
        }

        if (annotation.charAt(0) != '@') {
            annotations.add('@' + annotation);
        } else {
            annotations.add(annotation);
        }
    }

    public void addImplements(String className) {
        if (className == null || className.length() == 0) {
            throw new IllegalArgumentException("className for Implements cannot be null or empty");
        }

        implementsInterfaces.add(className);
    }

    public JavaInnerEnum addEnum(String enumName, List<String> enumValues) {
        return addEnum(new JavaInnerEnum(enumName, enumValues));
    }

    public JavaInnerEnum addEnum(JavaInnerEnum newEnum) {
        if (newEnum == null) {
            throw new IllegalArgumentException("newEnum cannot be null");
        }

        enums.add(newEnum);

        return newEnum;
    }

    public JavaVariable addVariable(JavaVariable newVariable) {
        if (newVariable == null) {
            throw new IllegalArgumentException("newVariable cannot be null");
        }

        variables.add(newVariable);
        return newVariable;
    }

    public JavaVariable addVariable(JavaVariable newVariable, boolean generateSetterGetter) {
        if (newVariable == null) {
            throw new IllegalArgumentException("newVariable cannot be null");
        }

        newVariable.setGenerateSetterGetter(generateSetterGetter);

        return addVariable(newVariable);
    }

    public JavaVariable addVariable(String datatype, String name) {
        JavaVariable newVariable = new JavaVariable(datatype, name);

        return addVariable(newVariable);
    }

    public JavaVariable addVariable(String datatype, String name, String defaultValue) {
        JavaVariable newVariable = new JavaVariable(datatype, name);
        newVariable.setDefaultValue(defaultValue);

        return addVariable(newVariable);
    }

    public JavaVariable addVariable(String datatype, String name, boolean generateSetterGetter) {
        JavaVariable newVariable = new JavaVariable(datatype, name);
        newVariable.setGenerateSetterGetter(generateSetterGetter);

        return addVariable(newVariable);
    }

    /**
     * Creates a public static final variable in the class.
     */
    public JavaVariable addConstant(String datatype, String name, String defaultValue) {
        return addConstant(datatype, name, defaultValue, true);
    }

    public JavaVariable addConstant(String datatype, String name, String defaultValue, boolean formatDefaultValue) {
        JavaVariable constant = new JavaVariable(datatype, name);
        constant.setAccess(Access.PUBLIC);
        constant.setStatic(true);
        constant.setFinal(true);
        constant.setDefaultValue(defaultValue, formatDefaultValue);
        addVariable(constant);

        return constant;
    }

    public JavaMethod addConstructor(Access access, List<JavaVariable> parameters, String content) {
        if (isInterface()) {
            throw new IllegalStateException("Cannot add a constructor to an Interface");
        }

        return addMethod(MethodType.CONSTRUCTOR, access, "", name, parameters, content);
    }

    public JavaMethod addMethod(JavaMethod newMethod) {
        if (newMethod == null) {
            throw new IllegalArgumentException("newMethod cannot be null");
        }

        switch (newMethod.getMethodType()) {
            case CONSTRUCTOR:
                constructors.add(newMethod);
                break;
            case STANDARD:
            default:
                methods.add(newMethod);
                break;
        }

        return newMethod;
    }

    public JavaMethod addMethod(Access access, String returnType, String name, String content) {
        JavaMethod newMethod = new JavaMethod(access, returnType, name);
        newMethod.setContent(content);
        addMethod(newMethod);

        return newMethod;
    }

    public JavaMethod addMethod(Access access, String returnType, String name, List<JavaVariable> parameters, String content) {
        return addMethod(MethodType.STANDARD, access, returnType, name, parameters, content);
    }

    private JavaMethod addMethod(MethodType methodType, Access access, String returnType, String name, List<JavaVariable> parameters, String content) {
        JavaMethod newMethod = new JavaMethod(access, returnType, name);
        newMethod.setMethodType(methodType);

        if (parameters != null) {
            newMethod.setParameters(parameters);
        }

        newMethod.setContent(content);
        addMethod(newMethod);

        return newMethod;
    }

    // ===================  BUILD METHODS  ================
    private String buildPackage() {
        return "package " + packageName + ";\n\n";
    }

    private String buildImports() {
        if (imports.isEmpty()) {
            return "";
        } else {
            StringBuilder out = new StringBuilder();
            for (String newImport : imports) {
                out.append("import ").append(newImport).append(";\n");
            }

            out.append("\n");

            return out.toString();
        }
    }

    private String buildClassHeader(String genericsTypeVar) {
        StringBuilder classHeader = new StringBuilder();
        classHeader.append("\n");

        // annotations
        for (String annotation : annotations) {
            classHeader.append(annotation).append("\n");
        }

        // generics
        String genericsVar = (genericsTypeVar == null ? "" : "<" + genericsTypeVar + ">");

        // extends
        String extendsNameString = "";
        if (getExtends() != null && getExtends().length() > 0) {
            extendsNameString = " extends " + getExtends();
        }

        // implements
        String implementsNamesString = "";
        if (implementsInterfaces != null && !implementsInterfaces.isEmpty()) {
            implementsNamesString = " implements ";

            for (int i = 0; i < implementsInterfaces.size(); i++) {
                if (i > 0) {
                    implementsNamesString += ", "; // nopmd - generator
                }

                implementsNamesString += implementsInterfaces.get(i); // nopmd - generator
            }
        }

        // generate header
        classHeader.append(getAccessString(getAccess())).append(" ");

        if (isAbstract()) {
            classHeader.append("abstract ");
        }

        if (isStatic()) {
            classHeader.append("static ");
        }

        if (isFinal()) {
            classHeader.append("final ");
        }

        switch (classType) {
            case CLASS:
                classHeader.append("class ").append(getName()).append(genericsVar);
                break;
            case INTERFACE:
                classHeader.append("interface ").append(getName()).append(genericsVar);
                break;
            case ENUM:
                classHeader.append("enum ").append(getName()).append(genericsVar);
                break;
            default:
        }


        classHeader.append(extendsNameString);
        classHeader.append(implementsNamesString);
        classHeader.append(" {\n");

        return classHeader.toString();
    }

    public static String getAccessString(Access access) {
        switch (access) {
            case DEFAULT_NONE:
                return "";
            case PUBLIC:
                return "public";
            case PRIVATE:
                return "private";
            case PROTECTED:
                return "protected";
            default:
                throw new IllegalArgumentException("Illegal Access type: " + access.toString());
        }
    }

    public static String getTab() {
        return tab;
    }

    public static void setTab(String newTab) {
        tab = newTab;
    }

    private String buildEnums() {
        String enumsText = "";
        String TAB = getTab();

        for (JavaInnerEnum enumItem : enums) {
            enumsText += TAB + enumItem.toString() + "\n";
        }

        enumsText += "\n";

        return enumsText;
    }

    private String buildVariables() {
        String variablesText = "";

        for (JavaVariable variable : variables) {
            variablesText += variable.toString() + ";\n";

            // add setters and getters if needed
            addAccessorMethods(variable);
        }

        variablesText += "\n";

        return variablesText;
    }

    private void addAccessorMethods(JavaVariable variable) {
        if (isInterface()) {
            // cannot add implementation to a Interface
            return;
        }

        String TAB = getTab();

        String type = variable.getDataType();
        String varName = variable.getName();

        // getter
        if (variable.isGenerateGetter()) {
            String getterContent;
            if (variable.isGetterReturnsClone()) {
                getterContent = "if (" + varName + " != null) {\n";
                getterContent += TAB + "return (" + variable.getDataType() + ")" + varName + ".clone();\n";
                getterContent += "} else {\n";
                getterContent += TAB + "return null;\n";
                getterContent += "}\n";
            } else {
                getterContent = "return " + varName + ";\n";
            }

            JavaMethod getterMethod = addMethod(variable.getGenerateGetterAccess(), type, variable.getGetterMethodName(), getterContent);
            getterMethod.setStatic(variable.isStatic());
        }

        // setter
        if (variable.isGenerateSetter()) {
            JavaMethod setterMethod = new JavaMethod(variable.getGenerateSetterAccess(), "void", variable.getSetterMethodName());
            setterMethod.addParameter(new JavaVariable(type, varName));

            setterMethod.setStatic(variable.isStatic());
            String thisText = "this";
            if (variable.isStatic()) {
                thisText = this.getName();
            }

            String setterContent = "";
            if (type != null && type.equals("String") && variable.isForceStringLength()) {
                int strLength = variable.getForcedStringLength();
                setterContent += "if (" + varName + "!= null && " + varName + ".length() > " + strLength + ") {\n";
                setterContent += TAB + thisText + "." + varName + " = " + varName + ".substring(0," + strLength + ");\n";
                setterContent += "} else {\n";
                setterContent += TAB + thisText + "." + varName + " = " + varName + ";\n";
                setterContent += "}\n";
            } else {
                if (variable.isSetterClonesParam()) {
                    setterContent += "if (" + varName + " != null) {\n";
                    setterContent += TAB + thisText + "." + varName + " = (" + variable.getDataType() + ") " + varName + ".clone();\n";
                    setterContent += "} else {\n";
                    setterContent += TAB + thisText + "." + varName + " = null;\n";
                    setterContent += "}\n";
                } else {
                    setterContent += thisText + "." + varName + " = " + varName + ";";
                }
            }


            String postSetterCode = variable.getPostSetterCode();
            if (postSetterCode.length() > 0) {
                setterContent += postSetterCode;
            }

            setterMethod.setContent(setterContent);
            addMethod(setterMethod);
        }
    }

    public static String createGetterMethodName(Class<?> type, String varName) {
        String methodVarName = varName.substring(0, 1).toUpperCase() + varName.substring(1);

        if (type == Boolean.class || type == boolean.class) {
            return "is" + methodVarName;
        } else {
            return "get" + methodVarName;
        }
    }

    public static String createSetterMethodName(String varName) {
        String methodVarName = varName.substring(0, 1).toUpperCase() + varName.substring(1);
        return "set" + methodVarName;
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();

        if (getFileHeaderComment().length() > 0) {
            out.append(getFileHeaderComment()).append("\n\n");
        }

        out.append("\n");
        out.append(buildPackage());
        out.append(buildImports());

        if (getClassHeaderComment().length() > 0) {
            out.append(getClassHeaderComment()).append("\n\n");
        }

        out.append(buildClassHeader(null)); // TODO... get rid of first parameter
        out.append(buildPostClassHeader()); // Support for ENUM type
//        out.append(constants);  // do not need this.... just use variables
        out.append(buildEnums());
        out.append(buildVariables());

        buildStaticInitializer(out);
        buildMethods(out);

        // end of class
        out.append("\n}");

        return out.toString();
    }

    private void buildStaticInitializer(final StringBuilder out) {
        if (staticInitializerBlock != null && staticInitializerBlock.length() > 0) {
            out.append(getTab()).append("static {\n");

            out.append(staticInitializerBlock).append("\n");

            out.append(getTab()).append("}\n\n");
        }
    }

    private void buildMethods(final StringBuilder out) {
        // constructor methods
        if (!isInterface()) {
            if (createDefaultConstructor) {
                addConstructor(Access.PUBLIC, null, "");
            }

            for (JavaMethod constructor : constructors) {
                out.append(constructor.toString());
                out.append("\n");
            }
        }

        // regular methods
        for (JavaMethod method : methods) {
            out.append(method.toString(isInterface()));
            out.append("\n");
        }
    }

    public String getFilename() {
        return getName() + ".java";
    }

    public void writeToDisk(String directoryname) {
        writeToDisk(directoryname, true);
    }

    public void writeToDisk(String directoryname, boolean overwrite) {
        File directory = new File(directoryname);
        directory.mkdirs();

        try {
            File outFile = new File(directoryname + "/" + getFilename());

            if (!overwrite && outFile.exists()) {
                return;
            }

            PrintStream fps = new PrintStream(new FileOutputStream(outFile));
            fps.print(this.toString());
            fps.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Access getAccess() {
        return access;
    }

    public void setAccess(Access access) {
        this.access = access;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExtends() {
        return extendsClass;
    }

    public void setExtends(String extendsClass) {
        this.extendsClass = extendsClass;
    }

    public boolean isAbstract() {
        return abstractClass;
    }

    public void setAbstract(boolean abstractClass) {
        this.abstractClass = abstractClass;
    }

    public boolean isStatic() {
        return staticClass;
    }

    public void setStatic(boolean staticClass) {
        this.staticClass = staticClass;
    }

    public boolean isFinal() {
        return finalClass;
    }

    public void setFinal(boolean finalClass) {
        this.finalClass = finalClass;
    }

    public boolean isCreateDefaultConstructor() {
        return createDefaultConstructor;
    }

    public void setCreateDefaultConstructor(boolean createDefaultConstructor) {
        this.createDefaultConstructor = createDefaultConstructor;
    }

    public static String formatConstant(String constant) {
        StringBuilder newConst = new StringBuilder();

        for (int i = 0; i < constant.length(); i++) {
            // add the current character in UPPERCASE
            newConst.append(Character.toUpperCase(constant.charAt(i)));

            // check for need of _
            char current = constant.charAt(i);
            char nextChar = ' ';
            if ((i + 1) < constant.length()) {
                nextChar = constant.charAt(i + 1);
            }

            if (!Character.isUpperCase(current) && nextChar != ' ' && Character.isUpperCase(nextChar)) {
                newConst.append('_');
            }
        }

        return newConst.toString();
    }

    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public static String formatDefaultValue(String fieldType, String defaultValue) {
        String newDefaultValue = "";

        if (defaultValue == null || defaultValue.equalsIgnoreCase("NULL")) {
            defaultValue = null; // nopmd
        }

        if (fieldType == null) {
            return "null";
        }

        if (fieldType.equals("String")) {
            if (defaultValue == null) {
                newDefaultValue = "\"\"";
            } else {
                newDefaultValue = "\"" + defaultValue + "\"";
            }
        } else if (fieldType.equals("int") || fieldType.equals("long") || fieldType.equals("float") || fieldType.equals("double")) {
            if (defaultValue == null || defaultValue.equals("")) {
                newDefaultValue = "0";
            } else {
                newDefaultValue = defaultValue;
            }
        } else if (fieldType.equals("Integer") || fieldType.equals("Long") || fieldType.equals("Float") || fieldType.equals("Double")) {
            if (defaultValue == null || defaultValue.equals("")) {
                newDefaultValue = "null";
            } else {
                newDefaultValue = defaultValue;
            }
        } else if (fieldType.equals("char") || fieldType.equals("Character")) {
            if (defaultValue == null || defaultValue.equals("")) {
                newDefaultValue = "''";
            } else {
                defaultValue = "'" + defaultValue + "'";
            }
        } else if (fieldType.equals("boolean") || fieldType.equals("Boolean")) {
            if (defaultValue == null || defaultValue.equals("")) {
                newDefaultValue = "false";
            } else {
                if (defaultValue.equals("1")) {
                    newDefaultValue = "true";
                } else if (defaultValue.equals("0")) {
                    newDefaultValue = "false";
                } else {
                    newDefaultValue = defaultValue;
                }
            }
        } else if (fieldType.equals("Date") && defaultValue != null && defaultValue.equalsIgnoreCase("NOW")) {
            newDefaultValue = "new Date()";
        } else if (fieldType.equals("BigInteger")) {
            if (defaultValue != null) {
                newDefaultValue = "new java.math.BigInteger(\"" + defaultValue + "\")";
            } else {
                newDefaultValue = "new java.math.BigInteger(\"0\")";
            }
        } else if (fieldType.equals("BigDecimal")) {
            if (defaultValue != null) {
                newDefaultValue = "new java.math.BigDecimal(" + defaultValue + ")";
            } else {
                newDefaultValue = "new java.math.BigDecimal(0)";
            }
        } else if (defaultValue != null && defaultValue.length() > 0) {
            newDefaultValue = defaultValue;
        } else {
            newDefaultValue = "null";
        }

        return newDefaultValue;
    }

    /**
     * Determines package name for specified path.
     */
    public static String createPackageFromFilePath(String filepath) {
        // if source path contains "java" then assume this is the source path (for maven projects)
        if (filepath.contains("src/main/java")) {
            return createPackageFromFilePath(filepath, "src.main.java");
        } else if (filepath.contains("src\\main\\java")) {
            return createPackageFromFilePath(filepath, "src.main.java");
        }
        if (filepath.contains("src/java")) {
            return createPackageFromFilePath(filepath, "src.java");
        } else if (filepath.contains("src\\java")) {
            return createPackageFromFilePath(filepath, "src.java");
        } else if (filepath.contains("src")) {
            return createPackageFromFilePath(filepath, "src");
        } else {//if (filepath.indexOf("source") != -1)
            return createPackageFromFilePath(filepath, "source");
        }
    }

    public static String createPackageFromFilePath(String filepath, String srcDirName) {
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

    /**
     * Method that makes sure that a string is java bean compliant.  Makes
     * sure that first letter is lowercase
     *
     * @param items Incoming String(s)
     * @return String that is adjusted to a proper java variable standards
     */
    public static String formatToJavaVariable(String... items) {
        String formattedName = "";

        boolean firstItem = true;
        for (String item : items) {
            if (firstItem) {
                formattedName += item.substring(0, 1).toLowerCase() + item.substring(1);
                firstItem = false;
            } else {
                formattedName += item.substring(0, 1).toUpperCase() + item.substring(1);
            }
        }

        return formattedName;
    }

    /**
     * Method that makes sure that a string is java bean compliant.  Makes
     * sure that first letter is upper case
     *
     * @param items Incoming String(s)
     * @return String that is adjusted to a proper java variable standards
     */
    public static String formatToJavaMethod(String... items) {
        String formattedName = "";

        boolean firstItem = true;
        for (String item : items) {
            if (firstItem) {
                formattedName += item.substring(0, 1).toLowerCase() + item.substring(1);
                firstItem = false;
            } else {
                formattedName += item.substring(0, 1).toUpperCase() + item.substring(1);
            }
        }

        return formattedName;
    }

    public String getFileHeaderComment() {
        return fileHeaderComment;
    }

    public void setFileHeaderComment(String fileHeaderComment) {
        this.fileHeaderComment = fileHeaderComment;
    }

    public String getClassHeaderComment() {
        return classHeaderComment;
    }

    public void setClassHeaderComment(String classHeaderComment) {
        this.classHeaderComment = classHeaderComment;
    }

    protected ClassType getClassType() {
        return classType;
    }

    protected void setClassType(ClassType classType) {
        this.classType = classType;
    }

    public boolean isInterface() {
        return classType == ClassType.INTERFACE;
    }

    public boolean isEnum() {
        return classType == ClassType.ENUM;
    }

    protected String buildPostClassHeader() {
        return "";
    }

    public void appendStaticInitializer(String code) {
        String TAB = getTab();
        staticInitializerBlock += TAB + TAB + code + "\n";
    }

    public void setStaticInitializer(String code) {
        String TAB = getTab();
        staticInitializerBlock = TAB + TAB + code + "\n";
    }
}
