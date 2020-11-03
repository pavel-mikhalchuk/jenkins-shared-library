def call(body) {
    def ctx = setUpContext(body)

    initDockerImageChoiceParameter(ctx)

    pipeline {
        agent { label 'helm-deploy' }
        options { 
            buildDiscarder(logRotator(numToKeepStr: '5'))
            timestamps () 
        }
        parameters { 
            choice(name: 'NAMESPACE', choices: ctx.namespaces, description: 'Kubernetes Namespace') 
            text(name: 'RESOURCES', defaultValue: ctx.podResources, description: 'Kubernetes POD resources requests and limits + JavaOpts')
        }
        stages {
            stage('notify slack') {
                steps {
                    // This step is very important!!! 
                    // Please do not remove it unless you find a better way without introducing "Init" stage because it's ugly :)"
                    // Later stages depend on it.
                    defineMoreContextBasedOnUserInput(ctx)

                    notifySlack(ctx)
                }
            }
            stage('checkout infra repo') {
                steps {
                    checkoutInfraRepo(ctx)
                }
            }
            stage('generate K8S manifests') {
                steps {
                    container('helm') {
                        writeHelmValuesYaml(ctx)
                        generateK8SManifests(ctx)
                    }
                }
            }
            stage('push K8S manifests to infra repo') {
                steps {
                    pushK8SManifests(ctx)
                }
            }
            stage('notify ArgoCD') {
                steps {
                    notifyArgoCD()
                }
            }
        }
    }
}

def setUpContext(body) {
    // client-defined parameters in the body block
    def ctx = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = ctx
    body()
    
    // defining more parameters for ourselves
    return ctx
}

def defineMoreContextBasedOnUserInput(ctx) {
    ctx.namespace = "${params.NAMESPACE}"
    ctx.dockerImage = "${ctx.service}:${params.IMAGE_TAG ?: 'latest'}"
    ctx.jenkinsBuildNumber = "${JOB_NAME}-${BUILD_NUMBER}"
    ctx.currentBranchName = "${BRANCH_NAME}"
    ctx.podResources = "${params.RESOURCES}"

    ctx.infraFolder = sh(script: 'echo infra-$(date +"%d-%m-%Y_%H-%M-%S")', returnStdout: true).trim()
    ctx.helmChartFolder = "kubernetes/helm-chart/${ctx.service}"
    ctx.helmRelease = "${ctx.service}-${ctx.namespace}"
    
    //// env-specific (dev VS prod)
    if (!ctx.env) {
        ctx.env = ctx.namespace == 'prod' ? 'prod' : 'dev'
    }
    ctx.kubeStateFolder = "${ctx.infraFolder}/kube-${ctx.env}/cluster-state/alutech-services/${ctx.namespace}/${ctx.service}/raw-manifests"
    if (!ctx.ingress) {
        ctx.ingress = defaultIngress(ctx)
    }
    ctx.envSpecificHelmValues = [
        environment: ctx.env == 'prod' ? "environment: ${ctx.env}" : "",
        host: ctx.ingress.enabled ? "host: ${resolveIngressHost(ctx)}" : ""
    ]
    ////  ////  //// //// //// ////////  ////  //// //// //// ////
}

def notifySlack(ctx) {
    script {
        wrap([$class: 'BuildUser']) {
            slackSend channel: "java_services", color: "good", message: "*${BUILD_USER}* накатывает ветку *${ctx.currentBranchName}* на *${ctx.service} ${ctx.namespace}*.\n${ctx.dockerImage}\nСохраняйте спокойствие 😌"
        }
    }
}

def checkoutInfraRepo(ctx) {
    sh "mkdir ${ctx.infraFolder}"

    dir("${ctx.infraFolder}") {
        git credentialsId: 'jenkins', url: 'http://bb.alutech-mc.com:8080/scm/as/infra.git'
    }
}

def writeHelmValuesYaml(ctx) {
    writeFile file: "${ctx.helmChartFolder}/values.yaml", text: 
        
        """replicaCount: 1
gitBranch: ${ctx.currentBranchName}
image:
  repository: dockerhub-vip.alutech.local
  tag: ${ctx.dockerImage}
  pullPolicy: IfNotPresent
service:
  externalPort: 80
  internalPort: 8080
jenkinsBuildNumber: ${ctx.jenkinsBuildNumber}
${ctx.envSpecificHelmValues.host}
${ctx.envSpecificHelmValues.environment}
${ctx.podResources}"""
}

def generateK8SManifests(ctx) {
    sh "rm -rf ${ctx.kubeStateFolder}"
    sh "mkdir -p ${ctx.kubeStateFolder}"

    sh "helm template --namespace ${ctx.namespace} --name ${ctx.helmRelease} ${ctx.helmChartFolder} > '${ctx.kubeStateFolder}/kube-state.yaml'"
}

def pushK8SManifests(ctx) {
    dir("${ctx.infraFolder}") {
        withCredentials([usernamePassword(credentialsId: 'jenkins', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
            sh 'git config user.email "jenkins@infra.tower"'
            sh 'git config user.name "Jenkins"'

            sh 'git add *'
            sh 'git commit -m "[jenkins]: ${JOB_NAME} - ${BUILD_NUMBER}"'
            sh 'git push http://${GIT_USERNAME}:${GIT_PASSWORD}@bb.alutech-mc.com:8080/scm/as/infra.git HEAD:master'
        }                        
    }
}

def notifyArgoCD() {
    sh 'curl -k -X POST https://git-events-publisher.in.in.alutech24.com/push'
}

def resolveIngressHost(ctx) {
    if (ctx.ingress.host instanceof Closure) {
        def hostByNs = { ns->
            return ns.contains('-') ? "${ns.split('-')[1]}.${ns.split('-')[0]}" : ns
        }

        def ingUtils = [
            svc_ns_inin: {
                return "${ctx.service}.${hostByNs(ctx.namespace)}.in.in.alutech24.com"
            },
            svc_prod: {
                return "${ctx.service}.alutech24.com"
            }
        ]
        return ctx.ingress.host.call(ingUtils)
    } else {
        return ctx.ingress.host
    }
}

def initDockerImageChoiceParameter(ctx) {
    properties([
        parameters([
            [
                $class: 'ChoiceParameter', 
                choiceType: 'PT_SINGLE_SELECT', 
                description: 'Docker image tags', 
                filterLength: 1,
                filterable: true,
                name: 'IMAGE_TAG', 
                script: [
                    $class: 'GroovyScript', 
                    fallbackScript: [classpath: [], sandbox: true, script: '["error :("]'], 
                    script: [classpath: [], sandbox: true, script: """
                        import java.util.logging.Level 
                        import java.util.logging.Logger

                        import groovy.json.JsonSlurper

                        def log = Logger.getLogger("com.alutech.activechoice.dockerhub")

                        def parse = { response ->
                            log.info("Parsing response...")

                            def tags = response?.trim() ? new JsonSlurper().parseText(response).tags : []
                            
                            log.info("Parsed!")
                            
                            def hasLatest = tags.remove("latest")
                            return hasLatest ? tags.plus(0, ["latest"]) : tags
                        }

                        try {      
                            log.info("Start fetching tags...")
                            
                            def response = ["curl", "-k", "https://dockerhub-vip.alutech.local/v2/${ctx.service}/tags/list"].execute().text
                            
                            log.info("Response from docker hub: " + response)

                            return parse(response)
                        } catch (Exception e) {
                            e.printStackTrace()
                        }
                    """]
                ]
            ]
        ])
    ])
}

def defaultIngress(ctx) {
    return ctx.env == 'dev'
            ? [ enabled: true, host: { ingUtils-> "${ingUtils.svc_ns_inin()}" } ]
            : [ enabled: false ]
}