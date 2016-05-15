package org.dbtools.plugin;

import org.dbtools.plugin.extensions.DBToolsExtension;
import org.dbtools.plugin.tasks.GenClassesTask;
import org.dbtools.plugin.tasks.InitTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class DBToolsAndroidPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        // extensions
        DBToolsExtension dbToolsExtension = new DBToolsExtension();
        project.getExtensions().add("dbtools", dbToolsExtension);

        // tasks
        project.getTasks().create("dbtools-init", InitTask.class);
        project.getTasks().create("dbtools-genclasses", GenClassesTask.class);
    }
}
