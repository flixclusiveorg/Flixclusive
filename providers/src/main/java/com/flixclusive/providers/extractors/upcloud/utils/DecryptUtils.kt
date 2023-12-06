package com.flixclusive.providers.extractors.upcloud.utils

typealias Stops = List<List<Int>>

object DecryptUtils {
    fun getKeyStops(script: String): Stops {
        val startOfSwitch = script.lastIndexOf("switch")
        val endOfCases = script.indexOf("=partKey")
        val switchBody = script.substring(startOfSwitch, endOfCases)

        val stops = mutableListOf<List<Int>>()
        val regex = Regex(":[a-zA-Z0-9]+=([a-zA-Z0-9]+),[a-zA-Z0-9]+=([a-zA-Z0-9]+);")
        val matches = regex.findAll(switchBody)

        for (match in matches) {
            val innerNumbers = mutableListOf<Int>()
            for (varMatch in listOf(match.groupValues[1], match.groupValues[2])) {
                val regexStr = Regex("$varMatch=([a-zA-Z0-9]+)")
                val varMatches = regexStr.findAll(script)
                val lastMatch = varMatches.lastOrNull()?.groupValues?.lastOrNull() ?: return listOf()
                val number = if (lastMatch.startsWith("0x")) {
                    lastMatch.substring(2).toInt(16)
                } else {
                    lastMatch.toInt()
                }
                innerNumbers.add(number)
            }
            stops.add(innerNumbers)
        }

        return stops
    }

    fun extractEmbedDecryptionDetails(encryptedString: String, stops: Stops): Pair<String, String> {
        var decryptedKey = ""
        var offset = 0
        var secondStopSum = 0;
        var newEncryptedString = encryptedString

        for(stop in stops) {
            val start = stop[0] + secondStopSum
            val end = start + stop[1]
            secondStopSum += stop[1]

            decryptedKey += newEncryptedString.substring(
                start - offset,
                end - offset
            )

            newEncryptedString = newEncryptedString.replaceRange(
                start - offset,
                end - offset,
                ""
            )

            offset += end - start
        }

        return Pair(decryptedKey, newEncryptedString)
    }
}