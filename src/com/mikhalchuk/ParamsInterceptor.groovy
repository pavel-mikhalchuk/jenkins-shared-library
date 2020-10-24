package com.mikhalchuk

class ParamsInterceptor {

    def delegate
    def params = []
    
    public ParamsInterceptor(delegate) {
        this.delegate = delegate
    }

    def invokeMethod(String name, Object args) {
        params.add(delegate."${name}"(args[0]))
    }
}