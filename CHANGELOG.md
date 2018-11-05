Change Log
==========

Version 11.0.0 *(2018-11)*
---------------------------
* Kotlin 1.3.0

Version 10.7.0 *(2018-03)*
---------------------------
* Match version with dbtools-android

Version 10.6.0 *(2018-03)*
---------------------------
* Kotlin 1.2.30

Version 10.5.0 *(2018-01)*
---------------------------
* Kotlin 1.2.10

Version 10.4.0 *(2017-12)*
---------------------------
* Kotlin 1.2.0

Version 10.3.0 *(2017-11)*
---------------------------
* Match version with dbtools-android

Version 10.2.0 *(2017-10)*
---------------------------
* Match version with dbtools-android

Version 10.1.4 *(2017-09)*
---------------------------
* Match version with dbtools-android

Version 10.1.3 *(2017-09)*
---------------------------
* Match version with dbtools-android

Version 10.0.2 *(2017-09)*
---------------------------
* Minor fixes to dbtools-android

Version 10.0.0 *(2017-07)*
---------------------------
* Added initial support for generating ROOM Entities, Daos, and Databases

Version 9.0.0 *(2017-04)*
---------------------------
* Added support text enums (use VARCHAR as field jdbcDataType)

Version 8.2.1 *(2017-03)*
---------------------------
* Added <index/> section to schema.xsd to support multiple column indexes
* Fix for Issue #15 (Database name cannot contain a `.`)


Version 8.2.0 *(2017-03)*
---------------------------
* Improved table notifications and subscriptions across managers and multiple databases
* Changed the variable name for versions to a proper const naming (in DatabaseManager)


Version 8.1.1 *(2017-03)*
---------------------------
* Fixed generated statement binding for REAL and DECIMAL
* Change Kotlin AndroidBaseRecord.tableName to AndroidBaseRecord.getTableName()
* Fixed issues with dbtools-gen with sub projects (ex: ./gradlew app:dbtools-gen)


Version 8.1.0 *(2017-02)*
---------------------------
* Removed default constructors of Record classes
* Improved rendering of Kotlin Manager classes 


Version 8.0.0 *(2017-01)*
---------------------------
* Removed default constructors of Record classes
* More improvements to Kotlin rendered code
* Fixed issues with defaultValues on fields that are nullable/notnull
* Fixed issues with nullable Double and Byte[]


Version 7.2.2 *(2017-01)*
---------------------------
* More improvements to Kotlin rendered code


Version 7.2.1 *(2016-12)*
---------------------------
* Improvements to Kotlin rendered code


Version 7.2.0 *(2016-11)*
---------------------------
* Added support for no primary key on a table


Version 7.1.1 *(2016-11)*
---------------------------
* Updated versions of dependencies 


Version 7.1.0 *(2016-10)*
---------------------------
* Changed all generated Kotlin vars to be open
* Fixed issue with generated kotlin logging


Version 7.0.11 *(2016-09)*
----------------------------
* Updated versions of dependencies 


Version 7.0.10 *(2016-07)*
----------------------------
* Fixed UniqueConstraint issues with JPA renderer
* Improvements to Kotlin renderer


Version 7.0.9 *(2016-07)*
---------------------------
* Added copy constructor to XXXBaseRecord


Version 7.0.8 *(2016-06)*
---------------------------
* Fixed dbtools-gen issues with Kotlin and Date type


Version 7.0.7 *(2016-06)*
---------------------------
* Fixed issues with schema rendering with Derby, FireBird, HSQLDB, iAnywhere, MySQL, Oracle, PostgreSQL
* Added improvements to JPA and Unique constraints
 
 
Version 7.0.5 *(2016-05)*
---------------------------
* Support for dbtools-android 7.0.5


Version 7.0.3 *(2016-05)*
---------------------------
* Moved project from Maven to Gradle
* Multi-project: Merged in gradle-dbtools-plugin
* Split Java and Kotlin sources
* Improved schema.xml XSD documentation


Version 7.0.2 *(2016-05)*
---------------------------
* Support for dbtools-android 7.0.2


Version 5.0.3 *(2016-03)*
----------------------------
* Kotlin generated files (better handling of nullable fields)
* Remove generated setters for readonly tables / queries / views
 
 
Version 5.0.1 *(2016-02)*
----------------------------
* Added Support for Kotlin
* Added Support for JSR-310 (use dateType 'JSR-310' in the dbtools-gen plugin)
* NEW XXRecordConst file that contains all the static fields and methods from the BaseRecord
 
Version 4.0.1 *(2015-10)*
----------------------------
* Added rxJavaSupport (generator will use a RxJava BaseManager) (Currently Android ONLY)
* Remove generated setters for readonly tables / queries / views


Version 3.3.0 *(2015-07)*
----------------------------
* Added ability to annotate generated domain setters/getters with jsr305 annotations (@Nullable, @Nonnull)
 

Version 3.1.1 *(2015-02)*
----------------------------
* Improved support for JPA (added support for dbtools-jpa.jar add a feature rich JpaBaseManager)
* Bug fixes and performance improvements

