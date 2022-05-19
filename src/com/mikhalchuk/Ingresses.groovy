package com.mikhalchuk

class Ingresses {

    static def SVC_NS_IN_IN = [
        enabled: true,
        host: { ingUtils -> "${ingUtils.svc_ns_inin()}" }
    ]

    static def STR_NS_IN_IN(str) {[
        enabled: true,
        host: { ingUtils -> "${ingUtils.str_ns_inin(str)}" }
    ]}

    static def SVC_PROD = [
        enabled: true,
        host: { ingUtils -> "${ingUtils.svc_prod()}" }
    ]

    static def STR_PROD(str) {[
        enabled: true,
        host: { ingUtils -> "${ingUtils.str_prod(str)}" }
    ]}

    static def DISABLED = [
        enabled: false
    ]
}