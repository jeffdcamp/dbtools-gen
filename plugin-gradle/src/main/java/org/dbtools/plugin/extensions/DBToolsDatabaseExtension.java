package org.dbtools.plugin.extensions;

public class DBToolsDatabaseExtension extends BaseDBToolsExtension {
    /**
     * Vendor to generate sql to.
     *
     * Example values: InterBase, Firebird, DB2, Oracle, Sybase, PostgreSQL, InstantDB,
     * HSQLDB, iAnywhere, Derby, PointBase, mySQL, MS SQLSERVER, MS SQLSERVER2000,
     * Cloudscape, InformixDB,
     */
    private String dbVendor;

    /**
     * Name of alias of database connection that will be added in DBTools
     */
    private String dbAlias;

    /**
     * Classpath of JDBC Driver
     */
    private String dbDriverClasspath;

    /**
     * Database URL
     */
    private String dbUrl;

    /**
     * Database Username
     */
    private String dbUsername;
    /**
     * Database Password
     */
    private String dbPassword;

    public String getDbVendor() {
        return dbVendor;
    }

    public void dbVendor(String dbVendor) {
        this.dbVendor = dbVendor;
    }

    public String getDbAlias() {
        return dbAlias;
    }

    public void dbAlias(String dbAlias) {
        this.dbAlias = dbAlias;
    }

    public String getDbDriverClasspath() {
        return dbDriverClasspath;
    }

    public void dbDriverClasspath(String dbDriverClasspath) {
        this.dbDriverClasspath = dbDriverClasspath;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public void dbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public void dbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void dbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }
}
