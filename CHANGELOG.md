Change Log
==========

  Version 7.0.6 *(2016-06)*
---------------------------
* Fixed issues with schema rendering with Derby, FireBird, HSQLDB, iAnywhere, MySQL, Oracle, PostgreSQL
 
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

