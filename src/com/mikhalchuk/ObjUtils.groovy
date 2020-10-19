package com.mikhalchuk

class ObjUtils {

    def static closureToMap(closure, mapPrefill = [:]) {
        def map = mapPrefill
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = map
        closure()
        return map
    }
}