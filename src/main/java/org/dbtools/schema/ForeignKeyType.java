/*
 * Field.java
 *
 * Created on August 3, 2007
 *
 * Copyright 2007 Jeff Campbell. All rights reserved. Unauthorized reproduction
 * is a violation of applicable law. This material contains certain
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */

package org.dbtools.schema;

/**
 * @author Jeff
 */
public enum ForeignKeyType {
    IGNORE,
    ONETOONE,
    MANYTOONE,
    ONETOMANY,
    ENUM
}
