package com.mikhalchuk

class IngressUtils {

    static defaultIngressDomains = ['.alutech24.com']

    static def hostByNs(ns) {
        return ns.contains('-') ? "${ns.split('-')[1]}.${ns.split('-')[0]}" : ns
    }

    static def resolveNsInIn(ctx, subDomain) {
        return resolve(ctx, "${subDomain}.${hostByNs(ctx.namespace)}.in.in")
    }

    static def resolve(ctx, subDomain) {
        def domains = resolveIngressDomains(ctx)

        return domains.size() > 1
                ? domains.collect { "${subDomain}${it}" }
                : "${subDomain}${domains[0]}"
    }

    static def resolveIngressDomains(ctx) {
        return ctx.ingressDomains ?: defaultIngressDomains
    }
}