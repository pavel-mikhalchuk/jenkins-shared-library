package com.mikhalchuk

class ParamsInterceptor {

    def delegate
    def params = []
    
    public ParamsInterceptor(delegate) {
        this.delegate = delegate
    }

    def invokeMethod(String name, Object args) {
        println args
        println args.toString()
        params.add(delegate."${name}"(args))
    }
}