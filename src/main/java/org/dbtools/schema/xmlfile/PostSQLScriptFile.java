package org.dbtools.schema.xmlfile;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

/**
 * User: jcampbell
 * Date: 1/25/14
 */
@Root
public class PostSQLScriptFile {
    @Attribute
    private boolean relativePath;

    @Attribute
    private String pathname;

    public boolean isRelativePath() {
        return relativePath;
    }

    public void setRelativePath(boolean relativePath) {
        this.relativePath = relativePath;
    }

    public String getPathname() {
        return pathname;
    }

    public void setPathname(String pathname) {
        this.pathname = pathname;
    }
}
