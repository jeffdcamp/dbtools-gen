package org.dbtools.plugin.tasks;

import org.dbtools.gen.DBObjectsBuilder;
import org.dbtools.gen.DateType;
import org.dbtools.gen.android.AndroidObjectsBuilder;
import org.dbtools.gen.android.kotlin.KotlinAndroidObjectsBuilder;
import org.dbtools.gen.jpa.JPAObjectsBuilder;
import org.dbtools.plugin.extensions.DBToolsExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class GenClassesTask extends DefaultTask {
    @TaskAction
    public void genclasses() {
        DBToolsExtension dbExt = (DBToolsExtension) getProject().getExtensions().findByName("dbtools");


        org.dbtools.gen.GenConfig genConfig = new org.dbtools.gen.GenConfig();
        genConfig.setInjectionSupport(dbExt.isInjectionSupport());
        genConfig.setJsr305Support(dbExt.isJsr305Support());
        genConfig.setIncludeDatabaseNameInPackage(dbExt.isIncludeDatabaseNameInPackage());
        genConfig.setJavaeeSupport(dbExt.isJavaEESupport());
        genConfig.setSqlQueryBuilderSupport(dbExt.isSqlQueryBuilderSupport());
        genConfig.setRxJavaSupport(dbExt.isRxJavaSupport());

        switch (dbExt.dateType()) {
            default:
            case "JAVA-DATE":
                genConfig.setDateType(DateType.JAVA_DATE);
                break;
            case "JODA":
                genConfig.setDateType(DateType.JODA);
                break;
            case "JSR-310":
                genConfig.setDateType(DateType.JSR_310);
                break;
        }

        DBObjectsBuilder builder;
        System.out.println("Using Builder: [" + dbExt.getType() + "]...");
        switch (dbExt.getType()) {
            case "ANDROID": // deprecated
            case "ANDROID-JAVA":
                builder = new AndroidObjectsBuilder(genConfig);
                break;
            case "ANDROID-KOTLIN":
                builder = new KotlinAndroidObjectsBuilder(genConfig);
                break;
            default:
            case "JPA":
                builder = new JPAObjectsBuilder(genConfig);
        }

        builder.setXmlFilename(dbExt.getSchemaFullFilename());
        builder.setOutputBaseDir(dbExt.getOutputSrcDir());
        builder.setPackageBase(dbExt.getBasePackageName());
        builder.setGenConfig(genConfig);
        builder.build();
    }
}
