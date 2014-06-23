package org.dbtools.schema;

/**
 * Created by hansenji on 6/3/14.
 */
public enum OnConflict {
    ROLLBACK,
    ABORT,
    FAIL,
    IGNORE,
    REPLACE,
    NONE
}
