/*
 * Copyright 2008 Jeff Campbell. All rights reserved. Unauthorized reproduction 
 * is a violation of applicable law. This material contains certain 
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */

package org.dbtools.schema;

import java.util.StringTokenizer;

/**
 * @author Jeff
 */
public class SQLStatement {
    public enum SQLType {UNKNOWN, SELECT, INSERT, UPDATE, DELETE, ALTER}

    private String statement;
    private StringTokenizer t;

    private SQLType type = SQLType.UNKNOWN;
    private String table;

    public SQLStatement(String statement) {
        this.statement = statement;
        initialAnalyze();
    }

    private void initialAnalyze() {
        t = new StringTokenizer(statement, " ", false);

        while (t.hasMoreTokens()) {
            String token = t.nextToken().toUpperCase();

            if (token.equals("SELECT")) {
                parseSelect();
            } else if (token.equals("INSERT")) {
                parseInsert();
            } else if (token.equals("UPDATE")) {
                parseUpdate();
            } else if (token.equals("DELETE")) {
                parseDelete();
            }
        }
    }

    private void parseSelect() {
        while (t.hasMoreTokens()) {
            if (t.nextToken().equalsIgnoreCase("FROM")) {
                table = t.nextToken();
                type = SQLType.SELECT;
                break;
            }
        }
    }

    private void parseInsert() {
        if (t.nextToken().equalsIgnoreCase("INTO")) {
            type = SQLType.INSERT;
            table = t.nextToken();
        }
    }

    private void parseUpdate() {
        type = SQLType.UPDATE;
        table = t.nextToken();
    }

    private void parseDelete() {
        while (t.hasMoreTokens()) {
            if (t.nextToken().equalsIgnoreCase("FROM")) {
                table = t.nextToken();
                type = SQLType.DELETE;
                break;
            }
        }
    }

    public SQLType getType() {
        return type;
    }

    public String getTable() {
        return table;
    }

    public boolean isSelect() {
        return type == SQLType.SELECT;
    }

    public boolean isInsert() {
        return type == SQLType.INSERT;
    }

    public boolean isUpdate() {
        return type == SQLType.UPDATE;
    }

    public boolean isDelete() {
        return type == SQLType.DELETE;
    }

}
