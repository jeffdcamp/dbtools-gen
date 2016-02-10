/*
 * DataType.java
 *
 * Created on April 16, 2007
 *
 * Copyright 2007 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.codegen.java;

/**
 * @author Jeff
 */
public enum DataType {

    STRING, INT, BOOLEAN, FLOAT, DOUBLE, CHAR, DATE, OBJECT;

    private DataType() {
    }

    public static DataType getDataType(String type) {
        if (type.equalsIgnoreCase("String")) {
            return STRING;
        } else if (type.equalsIgnoreCase("int")) {
            return INT;
        } else if (type.equalsIgnoreCase("boolean")) {
            return BOOLEAN;
        } else if (type.equalsIgnoreCase("float")) {
            return FLOAT;
        } else if (type.equalsIgnoreCase("double")) {
            return DOUBLE;
        } else if (type.equalsIgnoreCase("char")) {
            return CHAR;
        } else if (type.equalsIgnoreCase("java.util.Date")) {
            return DATE;
        } else {
            return OBJECT;
        }
    }
}
