package org.dbtools.plugin.tasks;

import org.dbtools.gen.DBToolsInit;
import org.dbtools.plugin.extensions.DBToolsExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class InitTask extends DefaultTask {
    @TaskAction
    public void init() {
        DBToolsExtension dbExt = (DBToolsExtension) getProject().getExtensions().findByName("dbtools");

        DBToolsInit dbToolsInit = new DBToolsInit();
        dbToolsInit.initDBTools(dbExt.getSchemaDir());
    }
}
