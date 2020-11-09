package com.mikhalchuk

class Scripts {

    static def ASERVICE_PRE_DEPLOY = {
        // Copying config into HELM folder...
        sh "cp -r configuration/src/main/resources/* kubernetes/helm-chart/aservice/"

        // Copying KUBE env-specific base.properties file into HELM folder...
        sh "cp configuration/src/main/resources/env/base.${params.NAMESPACE}.properties kubernetes/helm-chart/aservice/base.properties"
    }
}