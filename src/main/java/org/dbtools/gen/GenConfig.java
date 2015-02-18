package org.dbtools.gen;

public class GenConfig {
    private boolean injectionSupport = false;
    private boolean jsr305Support = false; // @Nullable / @Nonnull
    private boolean dateTimeSupport = false;
    private boolean javaeeSupport = false;
    private boolean includeDatabaseNameInPackage = false;
    private boolean ottoSupport = false; // Google/Square EventBus Support

    public GenConfig() {
    }

    public GenConfig(boolean injectionSupport,
                     boolean jsr305Support,
                     boolean dateTimeSupport,
                     boolean javaeeSupport,
                     boolean includeDatabaseNameInPackage,
                     boolean ottoSupport) {
        this.injectionSupport = injectionSupport;
        this.jsr305Support = jsr305Support;
        this.dateTimeSupport = dateTimeSupport;
        this.javaeeSupport = javaeeSupport;
        this.includeDatabaseNameInPackage = includeDatabaseNameInPackage;
        this.ottoSupport = ottoSupport;
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

    public boolean isDateTimeSupport() {
        return dateTimeSupport;
    }

    public void setDateTimeSupport(boolean dateTimeSupport) {
        this.dateTimeSupport = dateTimeSupport;
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

    public boolean isOttoSupport() {
        return ottoSupport;
    }

    public void setOttoSupport(boolean ottoSupport) {
        this.ottoSupport = ottoSupport;
    }
}
