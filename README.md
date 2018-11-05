
**NOTE**:  Because of the GREAT addition of Android Room (https://developer.android.com/topic/libraries/architecture/room), I'm deprecating this project.  I have also created a conversion from DBTools to Room by using the dbtools-gen type: ANDROID-KOTLIN-ROOM (this will generate Room Entities and Daos from the DBTools schema.xml file).  Also, there is a new dbtools-room project that is an library that makes it even easier to work with Google Room Library and SQLite Databases. (https://github.com/jeffdcamp/dbtools-room)

DBTools Generator
=================

DBTools Gen is a ORM Java class file generator and SQL Schema file generator library.
DBTools Gen makes it easy to create databases schema files and create JPA ORM Mapping or Android ORM Mapping files.

For platform specific DBTools usage see

 * [DBTools JPA](https://github.com/jeffdcamp/dbtools-jpa)
 * [DBTools Android](https://github.com/jeffdcamp/dbtools-android)

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.dbtools/dbtools-gen/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.dbtools/dbtools-gen)

Usage
-----

```gradle
buildscript {
    repositories {
        mavenCentral();
        // or
        jcenter();
    }
    
    dependencies {
        classpath 'org.dbtools:gradle-dbtools-plugin:<VERSION>'
    }
}

// ...
apply plugin: 'dbtools'

// ...
dbtools {
    type 'ANDROID-JAVA' // see options below 

    basePackageName 'com.domain.package.database'
    outputSrcDir 'src/main/java/com/domain/package/database'
}

```

After setting up gradle you can call the following methods

| Method | Description |
| -----  | ----------- |
| `dbtools-init` | Uses the `schemaDir` specified above to create a blank database schema file and xml xsd for auto-completion |
| `dbtools-genclasses` | Uses the options specified above to generate the actual ORM objects in the `outputSrcDir` |


Plugin Options
------

| Key | Values | Description |
| ---  | ----- | ----------- |
| type | `'ANDROID-JAVA'`, `'ANDROID-KOTLIN'`, `'JPA'`, `'ANDROID-KOTLIN-ROOM'` | The type of DAO and ORM files to generate.  Defaults to `'JPA'` |
| schemaDir | e.x. `'src/main/database'` | The directory the database schema file resides (or will be created with `dbtools-init`).  Defaults to `'src/main/database'` |
| schemaXMLFilename | e.x. `'schema.xml'` | The name of the database schema file.  Defaults to `'schema.xml'` |
| basePackageDir | e.x. `'com.dbtools.demo.database'` | The root package the generated DAO and ORM files have |
| outputSrcDir | e.x. `'src/main/java/com/dbtools/demo/database'` | The directory associated with the `basePackageDir` |
| dateType | `'JAVA-DATE'`, `'JODA'`, `'JSR-310'` | Specifies the format the Date fields should have.  Defaults to `'JAVA-DATE'` |
| injectionSupport | `true` or `false` | Adds annotated Injection (`@Inject`) to the generated files.  Defaults to `false` |
| jsr305Support | `true` or `false` | Adds annotated nullable (`@Nullable`) and nonnull (`@NonNull`) fields to the generated files.  Defaults to `false` |
| sqlQueryBuilderSupport | `true` or `false` | Use [DBTools-Query](https://github.com/jeffdcamp/dbtools-query) for generated queries and views.  Defaults to `false` |
| includeDatabaseNameInPackage | `true` or `false` | Include the database name in the generated package and directories.  Defaults to `true` |
| rxJavaSupport | `true` or `false` | Adds RxJava integration support.  Defaults to `false` |
| javaEESupport | `true` or `false` | Adds JEE/Spring Transactional annotations to CRUD methods in BaseManager.  Defaults to `false` |


License
-------

    Copyright 2015 Jeff Campbell

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
