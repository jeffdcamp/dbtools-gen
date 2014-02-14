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
import org.dbtools.schema.schemafile.SchemaTable;

import java.util.List;

/**
 * @author Jeff
 */
public interface DBTableObjectBuilder {
    public String getName();

    public boolean build();

    public int getNumberFilesGenerated();

    public List<String> getFilesGenerated();

    public void setDateTimeSupport(boolean b);

    public void setInjectionSupport(boolean b);

    public void setSpringSupport(boolean b);

    public void setDatabase(SchemaDatabase schemaDatabase);

    public void setTable(SchemaTable table);

    public void setPackageName(String packageName);

    public void setSourceOutputDir(String outDir);
}
