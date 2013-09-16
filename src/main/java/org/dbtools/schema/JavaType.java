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
    private Class<?> matchingNonPrimativeClass;
    private String matchingNonPrimativeClassText;
    private boolean primative = false;
    private boolean immutable = false;

    public JavaType(String javaTypeText, boolean primative, boolean immutable, Class<?> mainClass, Class<?> matchingNonPrimativeClass,
                    String matchingNonPrimativeClassText) {
        this.javaTypeText = javaTypeText;
        this.primative = primative;
        this.immutable = immutable;
        this.mainClass = mainClass;
        this.matchingNonPrimativeClass = matchingNonPrimativeClass;
        this.matchingNonPrimativeClassText = matchingNonPrimativeClassText;
    }

    public String getJavaTypeText() {
        return javaTypeText;
    }

    public void setJavaTypeText(String javaTypeText) {
        this.javaTypeText = javaTypeText;
    }

    public boolean isPrimative() {
        return primative;
    }

    public void setPrimative(boolean primative) {
        this.primative = primative;
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

    public Class<?> getMatchingNonPrimativeClass() {
        return matchingNonPrimativeClass;
    }

    public void setMatchingNonPrimativeClass(Class<?> matchingNonPrimativeClass) {
        this.matchingNonPrimativeClass = matchingNonPrimativeClass;
    }

    public String getMatchingNonPrimativeClassText() {
        return matchingNonPrimativeClassText;
    }

    public void setMatchingNonPrimativeClassText(String matchingNonPrimativeClassText) {
        this.matchingNonPrimativeClassText = matchingNonPrimativeClassText;
    }
}
