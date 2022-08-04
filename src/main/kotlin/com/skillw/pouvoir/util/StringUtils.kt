package com.skillw.pouvoir.util

import taboolib.common.util.asList
import java.util.*
import java.util.regex.Pattern

object StringUtils {
    private fun String.subStringWithEscape(from: Int, to: Int, escapes: List<Int>): String {
        val builder = StringBuilder()
        if (escapes.isEmpty())
            return substring(from, to)
        val it = escapes.iterator()
        var currentFrom = from
        var currentTo = it.next()
        while (currentTo != to) {
            builder.append(currentFrom, currentTo)
            currentFrom = currentTo + 1
            currentTo = if (it.hasNext())
                it.next()
            else
                to
        }
        if (currentFrom != currentTo)
            builder.append(currentFrom, currentTo)
        return builder.toString()
    }

    @JvmStatic
    fun String.protectedSplit(index: Char, protector: Pair<Char, Char>): ArrayList<String> {
        val list = ArrayList<String>()
        var inner = false
        var startIndex = 0
        val len = this.length
        val escapes = ArrayList<Int>()
        for (endIndex in 0 until len) {
            val c = this[endIndex]
            if (inner) {
                if (c == protector.value && this[endIndex - 1] != '\\') {
                    inner = false
                    escapes.add(endIndex)
                }
            } else {
                when (c) {
                    index -> {
                        list.add(subStringWithEscape(startIndex, endIndex, escapes))
                        escapes.clear()
                        startIndex = endIndex + 1
                    }

                    protector.key -> {
                        if (this[endIndex - 1] != '\\') {
                            inner = true
                            escapes.add(endIndex)
                        }
                    }
                }
            }
        }
        if (startIndex < len)
            list.add(subStringWithEscape(startIndex, len, escapes))
        return list
    }

    @JvmStatic
    fun Any.toStringWithNext(): String {
        if (this is Collection<*>) {
            return this.toStringWithNext()
        }
        return this.toString()
    }

    @JvmStatic
    fun String.toList(): List<String> {
        return if (this.contains("\n")) {
            this.split("\n").asList()
        } else listOf(this)
    }

    @JvmStatic
    fun Collection<*>.toStringWithNext(): String {
        return this.joinToString("\n")
    }

    @JvmStatic
    fun String.toArgs(): Array<String> =
        if (this.contains(","))
            split(",").toTypedArray()
        else
            arrayOf(this)

    @JvmStatic
    fun String.replacement(replaces: Map<String, Any>): String {
        var formulaCopy = this
        for (key in replaces.keys) {
            val value = replaces[key]!!
            formulaCopy = if (value is List<*>) {
                val pattern = Pattern.compile("$key\\((.*)\\)")
                val matcher = pattern.matcher(formulaCopy)
                if (!matcher.find()) return formulaCopy
                val stringBuffer = StringBuffer()
                do {
                    val args = matcher.group(1)?.toArgs() ?: continue
                    matcher.appendReplacement(stringBuffer, value.asList().getMultiple(args))
                } while (matcher.find())
                matcher.appendTail(stringBuffer).toString()
            } else {
                formulaCopy.replace(key, value.toString())
            }
        }
        return formulaCopy
    }

    @JvmStatic
    fun <T> Collection<T>.replacement(replaces: Map<String, Any>): List<String> {
        val list = LinkedList<String>()
        for (it in this) {
            list.add(it.toString().replacement(replaces))
        }
        return list
    }


    @JvmStatic
    fun List<String>.getMultiple(args: Array<String>): String {
        val strList = this
        val indexes: IntArray = when (args.size) {
            1 -> intArrayOf(
                args[0].toInt()
            )

            2 -> intArrayOf(
                args[0].toInt(),
                args[1].toInt()
            )

            else -> intArrayOf()
        }
        return if (indexes.isEmpty()) {
            strList.toStringWithNext()
        } else if (indexes.size == 1) {
            strList[indexes[0]] + "\n"
        } else if (indexes.size == 2) {
            val arrayList = ArrayList<String>()
            for (i in indexes[0]..indexes[1]) {
                arrayList.add(strList[i])
            }
            arrayList.toStringWithNext()
        } else {
            "NULL"
        }
    }

    @JvmName("parse1")
    @JvmStatic
    fun String.parse(leftChar: Char = '(', rightChar: Char = ')'): List<String> {
        val text = this
        val stack = Stack<Int>()
        var left = false
        val list = LinkedList<String>()
        for (index in text.indices) {
            val char = text[index]
            if (char == leftChar) {
                if (left) {
                    stack.pop()
                    stack.push(index)
                } else {
                    left = true
                    stack.push(index)
                }
            }
            if (char == rightChar) {
                if (left) {
                    val start = stack.pop()
                    list.add(text.substring(start + 1 until index))
                    left = false
                }
            }
        }
        return list
    }


    internal fun String.format(): String {
        return this.replace(Regex("\\s+"), " ")
    }

}