/*
 * JavaEnum.java
 *
 * Created on March 19, 2007
 *
 * Copyright 2007 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.codegen.java;

import java.util.Collections;
import java.util.List;

/**
 * @author Jeff
 */
@SuppressWarnings("PMD.UseStringBufferForStringAppends")
public class JavaEnum extends JavaClass {

    private List<String> enums;

    public JavaEnum(String name, List<String> enums) {
        super(name);
        setClassType(ClassType.ENUM);

        setEnums(enums);
    }

    public JavaEnum(String packageName, String name, List<String> enums) {
        super(packageName, name);
        setClassType(ClassType.ENUM);

        setEnums(enums);
    }

    public List<String> getEnums() {
        return Collections.unmodifiableList(enums);
    }

    public void setEnums(List<String> enums) {
        if (enums == null || enums.isEmpty()) {
            throw new IllegalArgumentException("enums cannot be null or empty");
        }

        this.enums = enums;
    }

    @Override
    protected String buildPostClassHeader() {
        String enumStr = "";

        int count = 0;
        for (String enumItem : enums) {
            if (count > 0) {
                enumStr += ", ";
            }

            enumStr += enumItem;

            count++;
        }

        enumStr += ";\n";

        return enumStr;
    }
}
