/*
 * IndividualType.java
 *
 * GENERATED FILE - DO NOT EDIT
 * CHECKSTYLE:OFF
 * 
 */



package org.dbtools.codegen.kotlin

import java.util.*


@SuppressWarnings("all")
enum class IndividualType {
    HEAD, SPOUSE, CHILD;


    companion object {

        val enumStringMap = EnumMap<IndividualType, String>(IndividualType::class.java)
        val stringList = ArrayList<String>()

        init {
            enumStringMap.put(HEAD, "Head")
            stringList.add("Head")

            enumStringMap.put(SPOUSE, "Spouse")
            stringList.add("Spouse")

            enumStringMap.put(CHILD, "Child")
            stringList.add("Child")


        }

        fun getString(key: IndividualType): String? {
            return enumStringMap[key]
        }

        val list: List<String>
            get() = Collections.unmodifiableList(stringList)
    }


}