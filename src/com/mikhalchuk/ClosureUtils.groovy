package com.mikhalchuk

class ClosureUtils {

    static def invoke(ctx, closure, delegate) {
        closure.resolveStrategy = Closure.DELEGATE_ONLY
        closure.delegate = delegate
        closure(ctx)
    }
}
