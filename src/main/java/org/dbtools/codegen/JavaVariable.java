/*
 * JavaVariable.java
 *
 * Created on August 11, 2006
 *
 * Copyright 2006 Jeff Campbell. All rights reserved. Unauthorized reproduction 
 * is a violation of applicable law. This material contains certain 
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.codegen;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jeff
 */
@SuppressWarnings("PMD.UseStringBufferForStringAppends")
public class JavaVariable {
    private VariableType variableType = VariableType.CLASS_VARIABLE;
    private String datatype;
    private String name;
    private Access access = Access.PRIVATE;
    private boolean staticVar = false;
    private boolean finalVar = false;
    private boolean volatileVar = false;
    private List<String> annotations = new ArrayList<>();
    private String defaultValue;
    // vars used by code generator
    private boolean generateSetter = false;
    private boolean generateGetter = false;
    private boolean getterReturnsClone = false;
    private boolean setterClonesParam = false;
    private Access generateSetterAccess = Access.PUBLIC;
    private Access generateGetterAccess = Access.PUBLIC;
    private String postSetterCode = "";
    private boolean forceStringLength = false;
    private int forcedStringLength = 255;

    public JavaVariable(String datatype, String name) {
        this.datatype = datatype;
        this.name = name;
    }

    public String getDataType() {
        return datatype;
    }

    public void setDataType(String datatype) {
        this.datatype = datatype;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Access getAccess() {
        return access;
    }

    public void setAccess(Access access) {
        this.access = access;
    }

    public boolean isStatic() {
        return staticVar;
    }

    public void setStatic(boolean staticVar) {
        this.staticVar = staticVar;
    }

    public boolean isFinal() {
        return finalVar;
    }

    public void setFinal(boolean finalVar) {
        this.finalVar = finalVar;
    }

    public boolean isVolatile() {
        return volatileVar;
    }

    public void setVolatile(boolean volatileVar) {
        this.volatileVar = volatileVar;
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

    @Override
    public String toString() {
        String varText = "";

        // annotations
        for (String annotation : annotations) {
            // add tab if this is a class Variable
            if (getVariableType() == VariableType.CLASS_VARIABLE) {
                varText += JavaClass.getTab();
            }

            varText += annotation + "\n";
        }

        // access
        if (getVariableType() == VariableType.CLASS_VARIABLE) {
            String accessText = JavaClass.getAccessString(access);
            varText += JavaClass.getTab();
            varText += accessText;
        }

        if (staticVar) {
            varText += " static";
        }

        if (finalVar) {
            varText += " final";
        }

        if (volatileVar) {
            varText += " volatile";
        }

        // datatype and name
        switch (getVariableType()) {
            case METHOD_PARAMETER:
                varText += datatype + " " + name;
                break;
            case CLASS_VARIABLE:
            default:
                varText += " " + datatype + " " + name;

                // set default value
                if (defaultValue != null && defaultValue.length() > 0) {
                    varText += " = " + defaultValue;
                }
                break;
        }


        return varText;
    }

    public boolean isGenerateSetterGetter() {
        return isGenerateSetter() && isGenerateGetter();
    }

    public void setGenerateSetterGetter(boolean generateSetterGetter) {
        this.setGenerateSetter(generateSetterGetter);
        this.setGenerateGetter(generateSetterGetter);
    }

    public boolean isForceStringLength() {
        return forceStringLength;
    }

    public void setForceStringLength(boolean forceStringLength) {
        this.forceStringLength = forceStringLength;
    }

    public int getForcedStringLength() {
        return forcedStringLength;
    }

    public void setForcedStringLength(int forcedStringLength) {
        this.forcedStringLength = forcedStringLength;
    }

    protected VariableType getVariableType() {
        return variableType;
    }

    protected void setVariableType(VariableType variableType) {
        this.variableType = variableType;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        setDefaultValue(defaultValue, true);
    }

    public void setDefaultValue(String defaultValue, boolean formatDefaultValue) {
        if (this.getVariableType() == VariableType.METHOD_PARAMETER) {
            throw new IllegalArgumentException("Cannot set a default value for a method parameter for variable [" + this.getName() + "]");
        }

        if (formatDefaultValue) {
            this.defaultValue = JavaClass.formatDefaultValue(getDataType(), defaultValue);
        } else {
            this.defaultValue = defaultValue;
        }
    }

    public String getPostSetterCode() {
        return postSetterCode;
    }

    public void setPostSetterCode(String postSetterCode) {
        this.postSetterCode = postSetterCode;
    }

    public String getSetterMethodName() {
        return getSetterMethodName(getName());
    }

    public static String getSetterMethodName(String varName) {
        return "set" + createBeanMethodName(varName);
    }

    public String getGetterMethodName() {
        return getGetterMethodName(getDataType(), getName());
    }

    public static String getGetterMethodName(String type, String varName) {
        String methodVarName = createBeanMethodName(varName);
        if (type != null && (type.equals("boolean") || type.equals("Boolean"))) {
            return "is" + methodVarName;
        } else {
            return "get" + methodVarName;
        }
    }

    public static String createBeanMethodName(String varName) {
        return varName.substring(0, 1).toUpperCase() + varName.substring(1);
    }

    public boolean isGenerateSetter() {
        return generateSetter;
    }

    public void setGenerateSetter(boolean generateSetter) {
        this.generateSetter = generateSetter;
    }

    public boolean isGenerateGetter() {
        return generateGetter;
    }

    public void setGenerateGetter(boolean generateGetter) {
        this.generateGetter = generateGetter;
    }

    public Access getGenerateSetterAccess() {
        return generateSetterAccess;
    }

    public void setGenerateSetterAccess(Access generateSetterAccess) {
        this.generateSetterAccess = generateSetterAccess;
    }

    public Access getGenerateGetterAccess() {
        return generateGetterAccess;
    }

    public void setGenerateGetterAccess(Access generateGetterAccess) {
        this.generateGetterAccess = generateGetterAccess;
    }

    public boolean isGetterReturnsClone() {
        return getterReturnsClone;
    }

    public void setGetterReturnsClone(boolean getterReturnsClone) {
        this.getterReturnsClone = getterReturnsClone;
    }

    public boolean isSetterClonesParam() {
        return setterClonesParam;
    }

    public void setSetterClonesParam(boolean setterClonesParam) {
        this.setterClonesParam = setterClonesParam;
    }

    public void setCloneSetterGetterVar(boolean clonesParam) {
        setGetterReturnsClone(clonesParam);
        setSetterClonesParam(clonesParam);
    }
}
