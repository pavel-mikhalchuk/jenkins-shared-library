def call(body) {
    def ctx = setUpContext(body)

    initDockerImageChoiceParameter()

    pipeline {
        agent { label 'java-deploy' }
        options { 
            buildDiscarder(logRotator(numToKeepStr: '5'))
            timestamps () 
        }
        parameters { 
            choice(name: 'NAMESPACE', choices: ctx.namespaces, description: 'Kubernetes Namespace') 
            text(name: 'RESOURCES', defaultValue: ctx.podResources, description: 'Kubernetes POD resources requests and limits + JavaOpts')
        }
        stages {
            stage('notify slack: DEPLOYMENT STARTED') {
                steps {
                    notifySlack(ctx)
                }
            }
            stage('Checkout infra repo') {
                steps {
                    // This step is very important!!! 
                    // Please do not remove it unless you find a better way without introducing "Init" stage because it's ugly :)"
                    // Later stages depend on it.
                    defineMoreContextBasedOnUserInput(ctx)

                    sh "ls -al"
                    sh "mkdir ${ctx.infraFolder}"
                    sh "ls -al"

                    dir("${ctx.infraFolder}") {
                        git credentialsId: 'jenkins', url: 'http://bb.alutech-mc.com:8080/scm/as/infra.git'
                    }

                }
            }
            stage('generate K8S manifests') {
                steps {
                    copyConfigToHelmChart(ctx)
                    writeHelmValuesYaml(ctx)
                    
                    sh 'rm -rf ${ctx.kubeStateFolder}'
                    sh 'mkdir -p ${ctx.kubeStateFolder}'
                    dir ('kubernetes/helm-chart/pricing') {
                        sh 'helm template --namespace ${ctx.namespace} --name ${ctx.helmRelease} . > "${ctx.kubeStateFolder}/kube-state.yaml"'
                    }
                }
            }
            stage('push K8S manifests to infra repo') {
                steps {
                    dir("${ctx.infraFolder}") {
                        withCredentials([usernamePassword(credentialsId: 'jenkins', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                            sh 'git config user.email "jenkins@kube.com"'
                            sh 'git config user.name "Jenkins"'

                            sh 'git add *'
                            sh 'git commit -m "[jenkins]: ${JOB_NAME} - ${BUILD_NUMBER}"'
                            sh 'git push http://${GIT_USERNAME}:${GIT_PASSWORD}@bb.alutech-mc.com:8080/scm/as/infra.git HEAD:master'
                        }                        
                    }
                }
            }
            stage('notify ARGOCD') {
                steps {
                    echo 'notify ARGOCD'
                }
            }
        }
        post {
            always {
                deleteDir() /* clean up our workspace */
            }
        }
    }
}

def initDockerImageChoiceParameter() {
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
                    script: [classpath: [], sandbox: true, script: '''
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
                            
                            def response = ["curl", "-H", "Host: dockerhub-vip.alutech.local", "-k", "https://10.100.20.33/v2/pricing/tags/list"].execute().text
                            
                            log.info("Response from docker hub: " + response)

                            return parse(response)
                        } catch (Exception e) {
                            e.printStackTrace()
                        }
                    ''']
                ]
            ]
        ])
    ])
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

    ctx.infraFolder = sh(script: 'infra-$(date +"%d-%m-%Y_%H-%M-%S")', returnStdout: true).trim()
    ctx.kubeStateFolder = "${ctx.infraFolder}/kube-dev/cluster-state/alutech-services/${ctx.namespace}/${ctx.service}/raw-manifests"
    ctx.helmRelease = "${ctx.service}-${ctx.namespace}"
}

def notifySlack(ctx) {
    echo 'Slack'
    // def causeJson = sh(script: 'echo $(curl -u krakhotkin:11607d902e7c73644a54ab39a83743db95 --silent ${BUILD_URL}/api/json | tr "{}" "\n" | grep "Started by")', returnStdout: true).trim()
    // def cause = new groovy.json.JsonSlurper().parseText("{" + causeJson + "}")
    
    // sh(script: "curl -X POST --data-urlencode 'payload={\"channel\": \"#java_services\", \"username\": \"Jenkins\", \"text\": \"*{{cause.userName}}* накатывает ветку *{{ctx.currentBranchName}}* на *{{ctx.service}} {{ctx.namespace}}*.\n{{ctx.dockerImage}}\nСохраняйте спокойствие 😌\", \"icon_emoji\": \":jenkins:\"}' https://hooks.slack.com/services/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", , returnStdout: true).trim()
}

def copyConfigToHelmChart(ctx) {
    sh "cp src/main/resources/application.${ctx.namespace}.properties kubernetes/helm-chart/${ctx.service}/application.properties"
}

def writeHelmValuesYaml(ctx) {
    writeFile file: "kubernetes/helm-chart/${ctx.service}/values.yaml", text: 
        
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
        host: ${hostName(ctx)}
        ${ctx.podResources}"""
}

def hostName(ctx) {
    return "${ctx.service}.${hostByNs(NAMESPACE)}.in.in.alutech24.com"
}

def hostByNs(ns) {
    return ns.contains('-') ? "${ns.split('-')[1]}.${ns.split('-')[0]}" : ns
}