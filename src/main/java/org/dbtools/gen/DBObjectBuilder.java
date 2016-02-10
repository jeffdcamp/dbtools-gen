/*
 * DBObjectBuilder.java
 *
 * Created on April 13, 2002
 *
 * Copyright 2006 Jeff Campbell. All rights reserved. Unauthorized reproduction 
 * is a violation of applicable law. This material contains certain 
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */

package org.dbtools.gen;


import org.dbtools.schema.schemafile.SchemaDatabase;
import org.dbtools.schema.schemafile.SchemaEntity;

import java.util.List;

public interface DBObjectBuilder {
    String getName();
    boolean build(SchemaDatabase database, SchemaEntity entity, String packageName, String outDir, GenConfig genConfig);
    int getNumberFilesGenerated();
    List<String> getFilesGenerated();
//    void setDatabase(SchemaDatabase schemaDatabase);
//    void setEntity(SchemaEntity table);
//    void setPackageName(String packageName);
//    void setSourceOutputDir(String outDir);

}
