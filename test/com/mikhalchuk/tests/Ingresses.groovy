package com.mikhalchuk.tests

class Ingresses {

    static def SVC_NS_IN_IN = [
        enabled: true,
        host: { ingUtils-> "${ingUtils.svc_ns_inin()}" }
    ]

    static def DISABLED = [
        enabled: false
    ]

    static def SVC_PROD = [
        enabled: true,
        host: { ingUtils-> "${ingUtils.svc_prod()}" }
    ]
}