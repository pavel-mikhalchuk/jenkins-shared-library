package com.mikhalchuk

class Ingresses {

    static def SVC_NS_IN_IN = [
        enabled: true,
        host: { ingUtils-> "${ingUtils.svc_ns_inin()}" }
    ]

    static def SVC_NS_IN_IN_NO_DOMAIN = [
        enabled: true,
        host: { ingUtils-> "${ingUtils.svc_ns_inin_no_domain()}" }
    ]

    static def STR_NS_IN_IN(str) {[
        enabled: true,
        host: { ingUtils-> "${ingUtils.str_ns_inin(str)}" }
    ]}

    static def STR_NS_IN_IN_NO_DOMAIN(str) {[
        enabled: true,
        host: { ingUtils-> "${ingUtils.str_ns_inin_no_domain(str)}" }
    ]}

    static def DISABLED = [
        enabled: false
    ]

    static def SVC_PROD = [
        enabled: true,
        host: { ingUtils-> "${ingUtils.svc_prod()}" }
    ]
}