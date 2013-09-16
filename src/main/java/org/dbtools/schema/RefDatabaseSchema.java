/*
 * RefDatabaseSchema.java
 *
 * Created on March 27, 2008
 *
 * Copyright 2008 Jeff Campbell. All rights reserved. Unauthorized reproduction 
 * is a violation of applicable law. This material contains certain 
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */
package org.dbtools.schema;

/**
 * @author Jeff
 */
public class RefDatabaseSchema {
    private String filepath;
    private DatabaseSchema dbSchema;
    private String baseJavaPackage;

    public RefDatabaseSchema(String filepath, DatabaseSchema dbSchema, String baseJavaPackage) {
        this.filepath = filepath;
        this.dbSchema = dbSchema;
        this.baseJavaPackage = baseJavaPackage;
    }

    public String getBaseJavaPackage() {
        return baseJavaPackage;
    }

    public void setBaseJavaPackage(String baseJavaPackage) {
        this.baseJavaPackage = baseJavaPackage;
    }

    public DatabaseSchema getDbSchema() {
        return dbSchema;
    }

    public void setDbSchema(DatabaseSchema dbSchema) {
        this.dbSchema = dbSchema;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }


}
