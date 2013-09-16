/*
 * JavaEnum.java
 *
 * Created on March 17, 2007, 9:27 PM
 *
 * Copyright 2007 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.codegen;

import java.util.Collections;
import java.util.List;

/**
 * @author Jeff
 */
@SuppressWarnings("PMD.UseStringBufferForStringAppends")
public class JavaInnerEnum {

    private Access access = Access.PUBLIC;
    private String name;
    private List<String> values;

    public JavaInnerEnum(String enumName, List<String> enumValues) {
        this.setName(enumName);

        if (enumValues == null) {
            throw new IllegalArgumentException("enumValues cannot be null");
        }
        this.setValues(enumValues);
    }

    @Override
    public String toString() {
        String enumStr = "";

        String accessText = JavaClass.getAccessString(getAccess());
        enumStr += accessText;

        enumStr += " static enum " + getName() + "{";

        int numItems = 0;
        for (String enumItem : values) {
            numItems++;
            if (numItems > 1) {
                enumStr += ",";
            }
            enumStr += enumItem;
        }

        enumStr += "};";

        return enumStr;
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

    public List<String> getValues() {
        return Collections.unmodifiableList(values);
    }

    public void setValues(List<String> values) {
        this.values = values;
    }
}
