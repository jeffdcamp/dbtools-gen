/*
 * CustomRenderer.java
 *
 * Created on May 7, 2002
 *
 * Copyright 2006 Jeff Campbell. All rights reserved. Unauthorized reproduction 
 * is a violation of applicable law. This material contains certain 
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */

package org.dbtools.gen;


/**
 * @author jeff
 */
public class CustomRenderer {

    private String jarFileName;
    private String rendererClassName;

    /**
     * Creates a new instance of CustomRenderer
     */
    public CustomRenderer() {
    }

    /**
     * Getter for property jarFileName.
     *
     * @return Value of property jarFileName.
     */
    public String getJarFileName() {
        return jarFileName;
    }

    /**
     * Setter for property jarFileName.
     *
     * @param jarFileName New value of property jarFileName.
     */
    public void setJarFileName(String jarFileName) {
        this.jarFileName = jarFileName;
    }

    /**
     * Getter for property rendererClassName.
     *
     * @return Value of property rendererClassName.
     */
    public String getRendererClassName() {
        return rendererClassName;
    }

    /**
     * Setter for property rendererClassName.
     *
     * @param rendererClassName New value of property rendererClassName.
     */
    public void setRendererClassName(String rendererClassName) {
        this.rendererClassName = rendererClassName;
    }

}
