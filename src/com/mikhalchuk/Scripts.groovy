package com.mikhalchuk

class Scripts {

    static def ASERVICE_PRE_DEPLOY = {
        // Copying config into HELM folder...
        sh "cp -r configuration/src/main/resources/* kubernetes/helm-chart/aservice/"

        // Copying KUBE env-specific base.properties file into HELM folder...
        sh "cp configuration/src/main/resources/env/base.${params.NAMESPACE}.properties kubernetes/helm-chart/aservice/base.properties"
    }

    static def ASERVICE_PRE_DEPLOY_MULTIPLE_DOMAINS = { ctx ->
        // Copying config into HELM folder...
        sh "cp -r configuration/src/main/resources/* kubernetes/helm-chart/aservice/"

        // Copying KUBE env-specific base.properties file into HELM folder...
        sh "cp configuration/src/main/resources/env/base.${params.NAMESPACE}.properties kubernetes/helm-chart/aservice/base.properties"

        ctx.ingressDomains = [".alutech24.com", ".alutech24.by", ".alutech24.eu"]
    }
}