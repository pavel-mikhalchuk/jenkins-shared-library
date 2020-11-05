package com.mikhalchuk

class ObjUtils {

    static def closureToMap(closure, mapPrefill = [:]) {
        def map = mapPrefill
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = map
        closure()
        return map
    }

    static def walk(map, callback) {
        map.inject([:], { res, entry ->
            if (entry.value instanceof Map) {
                res[entry.key] = walk(entry.value, callback)
            } else {
                callback(res, entry.key, entry.value)
            }
            return res
        })
    }
}