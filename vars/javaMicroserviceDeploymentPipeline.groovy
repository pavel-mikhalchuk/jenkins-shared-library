def call(body) {
    def ctx = setUpContext(body)

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
                    script: [classpath: [], sandbox: false, script: '''
                        import java.util.logging.Level 
                        import java.util.logging.Logger

                        import groovy.json.JsonSlurper

                        def log = Logger.getLogger("com.alutech.activechoice.dockerhub")

                        def parse = { response ->
                            log.info("Parsing response...")

                            def tags = response?.trim() ? new JsonSlurper().parseText(response).tags : []
                            
                            log.info("Parsed!")
                            
                            def hasLatest = tags.remove("latest")
                            return hasLatest ? tags.plus(0, "latest") : tags
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

    pipeline {
        agent any
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
                    // TODO
                }
            }
            stage('generate K8S manifests') {
              steps {
                  // This step is very important!!! 
                  // Please do not remove it unless you find a better way without introducing "Init" stage because it's ugly :)"
                  // Later stages depend on it.
                  defineMoreContextBasedOnUserInput(ctx)

                  copyConfigToHelmChart(ctx)
                  writeHelmValuesYaml(ctx)
              }
            }
            stage('push K8S manifests to git') {
              steps {
                  // ...
              }
            }
            stage('notify ARGOCD') {
              steps {
                  // ...
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