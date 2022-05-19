package com.mikhalchuk

class ClosureUtils {

    static def invoke(ctx, closure, delegate) {
        closure.resolveStrategy = Closure.DELEGATE_ONLY
        closure.delegate = delegate
        closure(ctx)
    }

    static Object maybeResolve(ctx, closure) {
        return (closure instanceof Closure) ? closure(ctx) : closure
    }
}
