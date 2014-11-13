package org.dbtools.gen;

public class GenConfig {
    private boolean injectionSupport;
    private boolean jsr305Support;
    private boolean encryptionSupport;
    private boolean dateTimeSupport;
    private boolean javaeeSupport;
    private boolean includeDatabaseNameInPackage;
    private boolean eventBusSupport;

    public GenConfig() {
    }

    public GenConfig(boolean injectionSupport,
                     boolean jsr305Support,
                     boolean encryptionSupport,
                     boolean dateTimeSupport,
                     boolean javaeeSupport,
                     boolean includeDatabaseNameInPackage,
                     boolean eventBusSupport) {
        this.injectionSupport = injectionSupport;
        this.jsr305Support = jsr305Support;
        this.encryptionSupport = encryptionSupport;
        this.dateTimeSupport = dateTimeSupport;
        this.javaeeSupport = javaeeSupport;
        this.includeDatabaseNameInPackage = includeDatabaseNameInPackage;
        this.eventBusSupport = eventBusSupport;
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

    public boolean isEncryptionSupport() {
        return encryptionSupport;
    }

    public void setEncryptionSupport(boolean encryptionSupport) {
        this.encryptionSupport = encryptionSupport;
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

    public boolean isEventBusSupport() {
        return eventBusSupport;
    }

    public void setEventBusSupport(boolean eventBusSupport) {
        this.eventBusSupport = eventBusSupport;
    }
}
