/*
 * ViewField.java
 *
 * Created on May 29, 2003
 *
 * Copyright 2006 Jeff Campbell. All rights reserved. Unauthorized reproduction 
 * is a violation of applicable law. This material contains certain 
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */

package org.dbtools.schema;

import org.dom4j.Element;


/**
 * @author Jeff Campbell
 */
public class SchemaViewField {

    private String name;
    private String expression;

    public SchemaViewField(String name, String expression) {
        this.name = name;
        this.expression = expression;
    }

    /**
     * Creates a new instance of SchemaField
     */
    public SchemaViewField(Element fieldElement) {
        try {
            name = fieldElement.attribute("name").getValue();
            expression = fieldElement.attribute("expression").getValue();
        } catch (Exception e) {
            System.out.println("Error converting view field attribute.");
            e.printStackTrace();
        }
    }

    /**
     * Getter for property name.
     *
     * @return Value of property name.
     */
    public java.lang.String getName() {
        return name;
    }

    /**
     * Setter for property name.
     *
     * @param name New value of property name.
     */
    public void setName(java.lang.String name) {
        this.name = name;
    }

    /**
     * Getter for property expression.
     *
     * @return Value of property expression.
     */
    public java.lang.String getExpression() {
        return expression;
    }

    /**
     * Setter for property expression.
     *
     * @param expression New value of property expression.
     */
    public void setExpression(java.lang.String expression) {
        this.expression = expression;
    }

}
