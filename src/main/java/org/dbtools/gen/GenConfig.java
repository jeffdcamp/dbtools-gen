package org.dbtools.gen;

public class GenConfig {
    private boolean injectionSupport = false;
    private boolean jsr305Support = false; // @Nullable / @Nonnull
    private DateType dateType = DateType.JAVA_DATE;
    private boolean javaeeSupport = false;
    private boolean includeDatabaseNameInPackage = false;
    private boolean eventBusSupport = false; // EventBus Support
    private boolean sqlQueryBuilderSupport = false; // when creating queries, use SQLBuilder (from dbtools-query)
    private boolean rxJavaSupport = false; // when creating queries, use SQLBuilder (from dbtools-query)

    public GenConfig() {
    }

    public boolean isInjectionSupport() {
        return injectionSupport;
    }

    public void setInjectionSupport(boolean injectionSupport) {
        this.injectionSupport = injectionSupport;
    }

    public boolean isJsr305Support() {
        return jsr305Support;
    }

    public void setJsr305Support(boolean jsr305Support) {
        this.jsr305Support = jsr305Support;
    }

    public DateType getDateType() {
        return dateType;
    }

    public void setDateType(DateType dateType) {
        this.dateType = dateType;
    }

    public boolean isJavaeeSupport() {
        return javaeeSupport;
    }

    public void setJavaeeSupport(boolean javaeeSupport) {
        this.javaeeSupport = javaeeSupport;
    }

    public boolean isIncludeDatabaseNameInPackage() {
        return includeDatabaseNameInPackage;
    }

    public void setIncludeDatabaseNameInPackage(boolean includeDatabaseNameInPackage) {
        this.includeDatabaseNameInPackage = includeDatabaseNameInPackage;
    }

    public boolean isEventBusSupport() {
        return eventBusSupport;
    }

    public void setEventBusSupport(boolean eventBusSupport) {
        this.eventBusSupport = eventBusSupport;
    }

    public boolean isRxJavaSupport() {
        return rxJavaSupport;
    }

    public void setRxJavaSupport(boolean rxJavaSupport) {
        this.rxJavaSupport = rxJavaSupport;
    }

    public boolean isSqlQueryBuilderSupport() {
        return sqlQueryBuilderSupport;
    }

    public void setSqlQueryBuilderSupport(boolean sqlQueryBuilderSupport) {
        this.sqlQueryBuilderSupport = sqlQueryBuilderSupport;
    }
}
