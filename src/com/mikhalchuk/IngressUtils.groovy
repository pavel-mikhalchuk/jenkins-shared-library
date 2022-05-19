package com.mikhalchuk

class IngressUtils {

    static def hostByNs(ns) {
        return ns.contains('-') ? "${ns.split('-')[1]}.${ns.split('-')[0]}" : ns
    }

    static def svcNsInIn(ctx) {
        return "${svcNsInInNoDomain(ctx)}.alutech24.com"
    }

    static def strNsInIn(ctx, str) {
        return "${strNsInInNoDomain(ctx, str)}.alutech24.com"
    }

    static def svcProd(ctx) {
        return "${ctx.service}.alutech24.com"
    }

    static def strProd(ctx, str) {
        return "${str}.alutech24.com"
    }

    static def svcNsInInNoDomain(ctx) {
        return "${ctx.service}.${hostByNs(ctx.namespace)}.in.in"
    }

    static def strNsInInNoDomain(ctx, str) {
        return "${str}.${hostByNs(ctx.namespace)}.in.in"
    }
}