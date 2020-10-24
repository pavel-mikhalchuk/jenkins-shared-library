package com.mikhalchuk

class Test {

    class BoolParam {
        private def map;

        public BoolParam(map) {
            this.map = map
        }
    }

    class StringParam {
        private def map;

        public StringParam(map) {
            this.map = map
        }
    }

    def booleanParam(map) {
        println map
        return new BoolParam(map)
    }

    def string(map) {
        println map
        return new StringParam(map)
    }

    def static params = {
        booleanParam(name: 'bool', defaultValue: true)
        string(name: 'string', defaultValue: "str")
    }

    static void main(String[] args) {
        def interceptor = new ParamsInterceptor(this)

        params.resolveStrategy = Closure.DELEGATE_ONLY
        params.delegate = interceptor
        params()
    }
}
