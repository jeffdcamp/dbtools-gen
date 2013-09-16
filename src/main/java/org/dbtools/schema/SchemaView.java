/*
 * View.java
 *
 * Created on May 29, 2003
 *
 * Copyright 2006 Jeff Campbell. All rights reserved. Unauthorized reproduction 
 * is a violation of applicable law. This material contains certain 
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */

package org.dbtools.schema;

import org.dom4j.Element;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Jeff Campbell
 */
public class SchemaView {

    private String name;
    private String viewPostSelectClause;

    private List<SchemaViewField> viewFields = new ArrayList<SchemaViewField>();

    /**
     * Creates a new instance of SchemaView
     */
    public SchemaView(String name) {
        this.name = name;
    }

    public SchemaView(Element viewElement) {
        name = viewElement.attribute("name").getValue();
        viewPostSelectClause = viewElement.element("viewPostSelectClause").getText();

        // get viewFields
        Iterator viewFieldsItr = viewElement.elementIterator("viewField");
        Element viewFieldElement;

        while (viewFieldsItr.hasNext()) {
            viewFieldElement = (Element) viewFieldsItr.next();
            addViewField(viewFieldElement);
        }
    }

    public boolean addViewField(SchemaViewField newViewField) {
        viewFields.add(newViewField);
        return true;
    }

    private boolean addViewField(Element viewFieldElement) {
        viewFields.add(new SchemaViewField(viewFieldElement));
        return true;
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
     * Getter for property viewFields.
     *
     * @return Value of property viewFields.
     */
    public List<SchemaViewField> getViewFields() {
        return viewFields;
    }

    /**
     * Setter for property viewFields.
     *
     * @param viewFields New value of property viewFields.
     */
    public void setViewFields(List<SchemaViewField> viewFields) {
        this.viewFields = viewFields;
    }

    /**
     * Getter for property viewPostSelectClause.
     *
     * @return Value of property viewPostSelectClause.
     */
    public java.lang.String getViewPostSelectClause() {
        return viewPostSelectClause;
    }

    /**
     * Setter for property viewPostSelectClause.
     *
     * @param viewPostSelectClause New value of property viewPostSelectClause.
     */
    public void setViewPostSelectClause(java.lang.String viewPostSelectClause) {
        this.viewPostSelectClause = viewPostSelectClause;
    }

}
