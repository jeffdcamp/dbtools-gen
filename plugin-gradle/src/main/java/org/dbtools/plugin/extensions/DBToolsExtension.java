package org.dbtools.plugin.extensions;

public class DBToolsExtension extends BaseDBToolsExtension {
    /**
     * Based directory where the source files will be generated.
     */
    private String outputSrcDir;

    /**
     * Type of application: JPA, ANDROID
     */
    private String type = "JPA";

    /**
     * Add JEE/Spring Transactional annotations to CRUD methods in BaseManager
     */
    private boolean javaEESupport = false;

    /**
     * Type of date to use in the database: JAVA-DATE, JODA, JSR-310 (not yet supported)
     */
    private String dateType = "JAVA-DATE";

    /**
     * Use CDI Dependency Injection
     */
    private boolean injectionSupport = false;

    /**
     * Use DBTools Event Bus to subscribe to database changes
     */
    private boolean eventBusSupport = false;

    /**
     * Use RxJava
     */
    private boolean rxJavaSupport = false;

    /**
     * Use jsr 305 (@Nullable, @Notnull, etc)
     */
    private boolean jsr305Support = true;

    /**
     * Use dbtools-query for generated queries
     */
    private boolean sqlQueryBuilderSupport = false;

    /**
     * If using multiple databases, organize domain objects by database name
     */
    private boolean includeDatabaseNameInPackage = true;

    /**
     * Name of the base package that should be used for generated files.  This
     * package name is a base to the packages that will be generated
     * (example: com.company.data will produce the following com.company.data.object1,
     * com.company.data.object2, etc) This package is also used to determine the
     * directories to create in both the
     * src/main/java AND src/test/java directories
     */
    private String basePackageName;

    public String getOutputSrcDir() {
        return outputSrcDir;
    }

    public void outputSrcDir(String outputSrcDir) {
        this.outputSrcDir = outputSrcDir;
    }

    public String getType() {
        return type;
    }

    public void type(String type) {
        this.type = type;
    }

    public boolean isJavaEESupport() {
        return javaEESupport;
    }

    public void javaEESupport(boolean javaEESupport) {
        this.javaEESupport = javaEESupport;
    }

    public String dateType() {
        return dateType;
    }

    public void dateType(String dateType) {
        this.dateType = dateType;
    }

    public boolean isInjectionSupport() {
        return injectionSupport;
    }

    public void injectionSupport(boolean injectionSupport) {
        this.injectionSupport = injectionSupport;
    }

    public boolean isRxJavaSupport() {
        return rxJavaSupport;
    }

    public void rxJavaSupport(boolean rxJavaSupport) {
        this.rxJavaSupport = rxJavaSupport;
    }

    public boolean isIncludeDatabaseNameInPackage() {
        return includeDatabaseNameInPackage;
    }

    public void includeDatabaseNameInPackage(boolean includeDatabaseNameInPackage) {
        this.includeDatabaseNameInPackage = includeDatabaseNameInPackage;
    }

    public String getBasePackageName() {
        return basePackageName;
    }

    public void basePackageName(String basePackageName) {
        this.basePackageName = basePackageName;
    }

    public boolean isJsr305Support() {
        return jsr305Support;
    }

    public void jsr305Support(boolean jsr305Support) {
        this.jsr305Support = jsr305Support;
    }

    public boolean isSqlQueryBuilderSupport() {
        return sqlQueryBuilderSupport;
    }

    public void sqlQueryBuilderSupport(boolean sqlQueryBuilderSupport) {
        this.sqlQueryBuilderSupport = sqlQueryBuilderSupport;
    }
}
