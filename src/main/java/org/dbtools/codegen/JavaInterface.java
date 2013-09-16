/*
 * JavaInterface.java
 *
 * Created on March 7, 2007, 4:50 PM
 *
 * Copyright 2007 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.codegen;

/**
 * @author Jeff
 */
public class JavaInterface extends JavaClass {

    public JavaInterface(String name) {
        super(name);
        setClassType(ClassType.INTERFACE);
    }

    public JavaInterface(String packageName, String name) {
        super(packageName, name);
        setClassType(ClassType.INTERFACE);
    }
}
