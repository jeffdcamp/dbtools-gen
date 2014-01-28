package org.dbtools.schema.xmlfile;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * User: jcampbell
 * Date: 1/25/14
 */
@Root
public class SchemaUniqueField {
    @Element
    private String name;
}
