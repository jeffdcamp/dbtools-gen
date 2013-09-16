/*
 * JavaMethod.java
 *
 * Created on August 11, 2006
 *
 * Copyright 2006 Jeff Campbell. All rights reserved. Unauthorized reproduction 
 * is a violation of applicable law. This material contains certain 
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.codegen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Jeff
 */
@SuppressWarnings("PMD.UseStringBufferForStringAppends")
public class JavaMethod {

    private MethodType methodType = MethodType.STANDARD;
    private String returnType = "void";
    private String name;
    private Access access = Access.PRIVATE;
    private boolean abstractMethod = false;
    private boolean staticMethod = false;
    private boolean constMethod = false;
    private List<String> annotations;
    private List<JavaVariable> parameters;
    private List<String> exceptions;
    private String content = "";

    public JavaMethod(String name) {
        this.name = name;
        init();
    }

    public JavaMethod(Access access, String returnType, String name) {
        this.access = access;
        this.returnType = returnType;
        this.name = name;
        init();
    }

    public JavaMethod(Access access, MethodType type, String returnType, String name) {
        this.access = access;
        this.setMethodType(type);
        this.returnType = returnType;
        this.name = name;
        init();
    }

    private void init() {
        setParameters(new ArrayList<JavaVariable>());
        annotations = new ArrayList<String>();
        exceptions = new ArrayList<String>();
    }

    public MethodType getMethodType() {
        return methodType;
    }

    public void setMethodType(MethodType methodType) {
        this.methodType = methodType;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
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
        return staticMethod;
    }

    public void setStatic(boolean staticMethod) {
        this.staticMethod = staticMethod;
    }

    public boolean isFinal() {
        return constMethod;
    }

    public void setFinal(boolean constMethod) {
        this.constMethod = constMethod;
    }

    @Override
    public String toString() {
        return toString(false);
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

    public String toString(boolean interfaceOnly) {
        String methodString = "";
        String TAB = JavaClass.getTab();

        for (String annotation : annotations) {
            methodString += TAB + annotation + "\n";
        }

        // access
        methodString += TAB + JavaClass.getAccessString(getAccess());

        // modifiers
        if (isAbstract()) {
            methodString += " abstract";
        }

        if (isStatic()) {
            methodString += " static";
        }

        if (isFinal()) {
            methodString += " final";
        }

        // method name
        if (getMethodType() != MethodType.CONSTRUCTOR) {
            methodString += " " + getReturnType();
        }
        methodString += " " + getName();

        // parameters
        methodString += "(";
        int paramCounter = 0;
        for (JavaVariable parameter : parameters) {
            if (paramCounter > 0) {
                methodString += ", ";
            }
            methodString += parameter.toString();

            paramCounter++;
        }
        methodString += ")";

        // exceptions
        if (!exceptions.isEmpty()) {
            methodString += " throws";

            int expCount = 0;
            for (String exception : exceptions) {
                if (expCount == 0) {
                    methodString += " ";
                } else {
                    methodString += ", ";
                }

                methodString += exception;
                expCount++;
            }
        }

        if (interfaceOnly) {
            methodString += ";\n";
            return methodString;
        } else {
            // content
            methodString += " {\n";

            if (content != null && content.length() > 0) {
                String[] splitContent = content.split("\n");
                for (String aSplitContent : splitContent) {
                    methodString += TAB + TAB + aSplitContent + "\n";
                }
            }

            methodString += TAB + "}\n";
        }

        return methodString;
    }

    public boolean isAbstract() {
        return abstractMethod;
    }

    public void setAbstract(boolean abstractMethod) {
        this.abstractMethod = abstractMethod;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void addParameter(JavaVariable parameter) {
        if (parameter == null) {
            throw new IllegalArgumentException("parameter cannot be null");
        }

        parameter.setVariableType(VariableType.METHOD_PARAMETER);
        parameters.add(parameter);
    }

    public List<JavaVariable> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    public void setParameters(List<JavaVariable> parameters) {
        for (JavaVariable parameter : parameters) {
            parameter.setVariableType(VariableType.METHOD_PARAMETER);
        }

        this.parameters = parameters;
    }

    public void addThrowsException(String exceptionClass) {
        if (exceptionClass == null || exceptionClass.length() == 0) {
            throw new IllegalArgumentException("exceptionClass cannot be empty or null");
        }

        exceptions.add(exceptionClass);
    }
}
