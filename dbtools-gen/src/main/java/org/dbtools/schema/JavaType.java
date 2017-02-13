/**
 * Copyright 2008 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.schema;

/**
 * @author Jeff
 */
public class JavaType {

    private String javaTypeText;
    private Class<?> mainClass;
    private Class<?> matchingNonPrimitiveClass;
    private String matchingNonPrimitiveClassText;
    private String kotlinClassText;
    private String kotlinClassDefaultValueText;
    private String javaClassDefaultValueText;
    private boolean primitive = false;
    private boolean immutable = false;

    public JavaType(String javaTypeText, boolean primitive, boolean immutable, Class<?> mainClass, Class<?> matchingNonPrimitiveClass,
                    String matchingNonPrimitiveClassText, String javaClassDefaultValueText, String kotlinClassText, String kotlinClassDefaultValueText) {
        this.javaTypeText = javaTypeText;
        this.primitive = primitive;
        this.immutable = immutable;
        this.mainClass = mainClass;
        this.matchingNonPrimitiveClass = matchingNonPrimitiveClass;
        this.matchingNonPrimitiveClassText = matchingNonPrimitiveClassText;
        this.javaClassDefaultValueText = javaClassDefaultValueText;
        this.kotlinClassText = kotlinClassText;
        this.kotlinClassDefaultValueText = kotlinClassDefaultValueText;
    }

    public String getJavaTypeText() {
        return javaTypeText;
    }

    public void setJavaTypeText(String javaTypeText) {
        this.javaTypeText = javaTypeText;
    }

    public boolean isPrimitive() {
        return primitive;
    }

    public void setPrimitive(boolean primitive) {
        this.primitive = primitive;
    }

    public boolean isImmutable() {
        return immutable;
    }

    public void setImmutable(boolean immutable) {
        this.immutable = immutable;
    }

    public Class<?> getMainClass() {
        return mainClass;
    }

    public void setMainClass(Class<?> mainClass) {
        this.mainClass = mainClass;
    }

    public Class<?> getMatchingNonPrimitiveClass() {
        return matchingNonPrimitiveClass;
    }

    public String getKotlinClassText() {
        return kotlinClassText;
    }

    public String getKotlinClassDefaultValueText() {
        return kotlinClassDefaultValueText;
    }

    public void setMatchingNonPrimitiveClass(Class<?> matchingNonPrimitiveClass) {
        this.matchingNonPrimitiveClass = matchingNonPrimitiveClass;
    }

    public String getMatchingNonPrimitiveClassText() {
        return matchingNonPrimitiveClassText;
    }

    public void setMatchingNonPrimitiveClassText(String matchingNonPrimitiveClassText) {
        this.matchingNonPrimitiveClassText = matchingNonPrimitiveClassText;
    }

    public String getJavaClassDefaultValueText() {
        return javaClassDefaultValueText;
    }
}
