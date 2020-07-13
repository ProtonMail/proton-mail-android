package ch.protonmail.android.uitests.testsHelper

import java.util.*

class ParametersUtils {

    fun getParameters(): Map<String, String>? {
        val myMap: MutableMap<String, String> = HashMap()
        val pairs = System.getenv("PARAMS").split(";".toRegex()).toTypedArray()
        for (i in pairs.indices) {
            val pair = pairs[i]
            val keyValue = pair.split(":".toRegex()).toTypedArray()
            myMap[keyValue[0]] = keyValue[1]
        }
        return myMap
    }

}