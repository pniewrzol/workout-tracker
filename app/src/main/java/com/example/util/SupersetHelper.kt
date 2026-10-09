package com.example.util

import com.example.data.model.Exercise

/**
 * Informacja o przynależności ćwiczenia do serii łączonej (superserii).
 */
data class SupersetInfo(
    val groupKey: String,          // np. "C.5", "A.5", "1"
    val letter: String,            // np. "a", "b", "c"
    val indexInGroup: Int,         // 0 dla 'a', 1 dla 'b'
    val totalInGroup: Int,         // łączna liczba ćwiczeń w serii łączonej (np. 2)
    val partnerExercises: List<Exercise> // inne ćwiczenia w tej samej serii łączonej
) {
    val isFirstInGroup: Boolean get() = indexInGroup == 0
    val isLastInGroup: Boolean get() = indexInGroup == totalInGroup - 1
    val isFirstInSuperset: Boolean get() = isFirstInGroup
    val isLastInSuperset: Boolean get() = isLastInGroup
    val displayBadge: String get() = "Seria łączona (${letter.uppercase()})"
}

object SupersetHelper {

    /**
     * Wyodrębnia bazowy klucz grupy i literę indeksu z kodu lub nazwy ćwiczenia.
     * Przykłady:
     * - "C.5a" -> ("C.5", "a")
     * - "C.5b" -> ("C.5", "b")
     * - "A.1a" -> ("A.1", "a")
     * - "5a" -> ("5", "a")
     * - "5b" -> ("5", "b")
     * - "1a. Wyciskanie" -> ("1", "a")
     * - "Wyciskanie C.5a" -> ("C.5", "a")
     */
    fun extractGroupAndLetter(exercise: Exercise): Pair<String, String>? {
        val code = exercise.code.trim()
        if (code.isNotEmpty()) {
            val codeRegex = Regex("""^([A-Za-z0-9.]+?)[.\-_]?([a-zA-Z])$""")
            val match = codeRegex.find(code)
            if (match != null) {
                val base = match.groupValues[1]
                val letter = match.groupValues[2].lowercase()
                if (base.any { it.isDigit() }) {
                    return base to letter
                }
            }
        }

        val name = exercise.name.trim()
        val exactRegex = Regex("""^([A-Za-z0-9.]+?)[.\-_]?([a-zA-Z])$""")
        val exactMatch = exactRegex.find(name)
        if (exactMatch != null) {
            val base = exactMatch.groupValues[1]
            val letter = exactMatch.groupValues[2].lowercase()
            if (base.any { it.isDigit() }) {
                return base to letter
            }
        }

        val prefixRegex = Regex("""^([A-Za-z0-9.]+?)[.\-_]?([a-zA-Z])(?:\s|[\s.:\-])""")
        val prefixMatch = prefixRegex.find(name)
        if (prefixMatch != null) {
            val base = prefixMatch.groupValues[1]
            val letter = prefixMatch.groupValues[2].lowercase()
            if (base.any { it.isDigit() }) {
                return base to letter
            }
        }

        val parenRegex = Regex("""\(([A-Za-z0-9.]+?)[.\-_]?([a-zA-Z])\)""")
        val parenMatch = parenRegex.find(name)
        if (parenMatch != null) {
            val base = parenMatch.groupValues[1]
            val letter = parenMatch.groupValues[2].lowercase()
            if (base.any { it.isDigit() }) {
                return base to letter
            }
        }

        val suffixRegex = Regex("""(?:[\s.:\-])([A-Za-z0-9.]+?)[.\-_]?([a-zA-Z])$""")
        val suffixMatch = suffixRegex.find(name)
        if (suffixMatch != null) {
            val base = suffixMatch.groupValues[1]
            val letter = suffixMatch.groupValues[2].lowercase()
            if (base.any { it.isDigit() }) {
                return base to letter
            }
        }

        return null
    }

    /**
     * Buduje mapę Exercise.id -> SupersetInfo dla danej listy ćwiczeń w treningu.
     * Tylko grupy z minimum 2 ćwiczeniami są traktowane jako serie łączone.
     */
    fun detectSupersets(exercises: List<Exercise>): Map<Long, SupersetInfo> {
        val groupedByBase = mutableMapOf<String, MutableList<Pair<Exercise, String>>>()

        for (ex in exercises) {
            val extracted = extractGroupAndLetter(ex)
            if (extracted != null) {
                val (base, letter) = extracted
                groupedByBase.getOrPut(base) { mutableListOf() }.add(ex to letter)
            }
        }

        val result = mutableMapOf<Long, SupersetInfo>()

        for ((baseKey, items) in groupedByBase) {
            if (items.size >= 2) {
                // Sortuj według litery (a, b, c...)
                val sortedItems = items.sortedBy { it.second }
                val allExsInGroup = sortedItems.map { it.first }
                val total = sortedItems.size

                sortedItems.forEachIndexed { idx, (exercise, letter) ->
                    val partners = allExsInGroup.filter { it.id != exercise.id }
                    result[exercise.id] = SupersetInfo(
                        groupKey = baseKey,
                        letter = letter,
                        indexInGroup = idx,
                        totalInGroup = total,
                        partnerExercises = partners
                    )
                }
            }
        }

        return result
    }

    /**
     * Zwraca kolejne ćwiczenie w serii łączonej, jeśli istnieje.
     */
    fun getNextExerciseInSuperset(
        currentExerciseId: Long,
        exercises: List<Exercise>,
        supersetsMap: Map<Long, SupersetInfo>
    ): Exercise? {
        val currentInfo = supersetsMap[currentExerciseId] ?: return null
        if (currentInfo.isLastInGroup) return null

        val currentExIndex = exercises.indexOfFirst { it.id == currentExerciseId }
        if (currentExIndex >= 0 && currentExIndex + 1 < exercises.size) {
            val nextEx = exercises[currentExIndex + 1]
            val nextInfo = supersetsMap[nextEx.id]
            if (nextInfo != null && nextInfo.groupKey == currentInfo.groupKey) {
                return nextEx
            }
        }
        return null
    }
}
