package org.dbtools.schema.schemafile;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

import java.io.File;

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

    public String getPreparedFilepath() {
        return prepareFilepath("", pathname, false, relativePath);
    }

    private String prepareFilepath(String schemaFilename, String filePathToPrepare, boolean schemaXMLFilenameIsAResource, boolean relativePath) {
        String preparedFilepath;

        // append current directory to path if this is a relative path
        if (relativePath) {
            if (schemaXMLFilenameIsAResource) {
                // inserts are also a resource
                // strip off the filename from the classpath of the
                // xmlschema filename
                String resourcePath = "";
                char SEPERATOR = '/';

                String pathSegment = "";
                for (int i = 0; i < schemaFilename.length(); i++) {
                    char nextChar = schemaFilename.charAt(i);

                    if (nextChar == SEPERATOR) {
                        resourcePath += pathSegment + SEPERATOR;

                        // reset
                        pathSegment = "";
                    } else {
                        pathSegment += nextChar;
                    }
                }

                preparedFilepath = resourcePath + filePathToPrepare;
            } else {
                // inserts are on the file system
                File schemaFile = new File(schemaFilename);
                int filenameSize = schemaFile.getName().length();
                String path = schemaFile.getPath();
                preparedFilepath = path.substring(0, path.length() - filenameSize) + filePathToPrepare;
            }
        } else {
            preparedFilepath = filePathToPrepare;
        }

        return preparedFilepath;
    }
}
