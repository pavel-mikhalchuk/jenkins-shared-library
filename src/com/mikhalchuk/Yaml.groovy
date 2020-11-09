package com.mikhalchuk

class Yaml {

    static def indentOf(line) {
        def result = ''
        for (c in line.chars()) {
            if (c == ' ') result += ' '
            else return result
        }
        return result
    }

    static def quote(str) {
        str ? "\"${str}\"" : null
    }

    static def unquote(str) {
        if (str.charAt(0) == '"' && str.charAt(str.length() - 1) == '"') {
            return str.substring(1, str.length() - 1)
        }
        return str
    }

    static def nextLineIsNewRef(lines, currIdx, indent, value) {
        if (!value && (currIdx + 1) < lines.size()) {
            def nextLine = lines[currIdx + 1]
            def nextIndent = indent + '  '
            return nextLine.startsWith(nextIndent) && nextLine.charAt(nextIndent.length()) != ' '
        }
        return false
    }

    static def newRefEntry(refMap, indent, key) {
        def newMap = [:]
        refMap[indent][key] = newMap
        refMap[indent + '  '] = newMap
    }

    static def parse(str) {
        def lines = str.split('\n')
        def map = [:]
        def refMap = [:]
        refMap[''] = map

        lines.eachWithIndex {line, i ->
            def indent = indentOf(line)
            def keyValue = line.split(':')

            if (keyValue.size() == 1) {

                newRefEntry(refMap, indent, keyValue[0].trim())

            } else if (keyValue.size() == 2 && nextLineIsNewRef(lines, i, indent, keyValue[1].trim())) {

                newRefEntry(refMap, indent, keyValue[0].trim())

            } else {
                refMap[indent][keyValue[0].trim()] = unquote(keyValue[1].trim())
            }
        }
        return map
    }

    static def write(values) {
        doWrite(values, '')
    }

    private static def doWrite(values, indent = '') {
        def result = ''
        values.each {
            if (it.value instanceof Map) {
                result += "${indent}${it.key}:\n"
                result += doWrite(it.value, indent + '  ')
            } else if (it.value instanceof String) {
                if (it.value?.trim()) {
                    result += "${indent}${it.key}:${it.value.startsWith('\n') ? '' : ' '}${quote(it.value)}\n"
                }
            } else {
                result += "${indent}${it.key}: ${quote(it.value)}\n"
            }
        }
        if (indent == '' && result.endsWith("\n")) {
            result = result.substring(0, result.length() - 1)
        }
        result
    }
}