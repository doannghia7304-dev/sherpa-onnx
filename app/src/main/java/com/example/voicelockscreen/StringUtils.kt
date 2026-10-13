package com.example.voicelockscreen

import java.text.Normalizer
import java.util.regex.Pattern

object StringUtils {
    fun normalizeForComparison(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFC)
            .uppercase()
            .replace("\\s+".toRegex(), "")
            .trim()
    }

    fun removeAccents(text: String): String {
        val nfd = Normalizer.normalize(text, Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(nfd)
            .replaceAll("")
            .uppercase()
            .replace("Đ", "D")
            .replace("đ", "d")
            .replace("\\s+".toRegex(), "")
            .trim()
    }

    fun isPassphraseMatch(spoken: String, target: String): Boolean {
        if (spoken.isBlank() || target.isBlank()) return false

        val cleanSpokenNfc = normalizeForComparison(spoken)
        val cleanTargetNfc = normalizeForComparison(target)

        if (cleanSpokenNfc.contains(cleanTargetNfc) || cleanTargetNfc.contains(cleanSpokenNfc)) {
            return true
        }

        val spokenNoAccent = removeAccents(spoken)
        val targetNoAccent = removeAccents(target)

        return spokenNoAccent.contains(targetNoAccent) || targetNoAccent.contains(spokenNoAccent)
    }
}
