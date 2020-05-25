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
                name: 'DockerTags', 
                script: [
                    $class: 'GroovyScript', 
                    fallbackScript: [classpath: [], sandbox: true, script: '["error :("]'], 
                    script: [classpath: [], sandbox: false, script: '''
                        import java.util.logging.Level 
                        import java.util.logging.Logger

                        import groovy.json.JsonSlurper

                        def log = Logger.getLogger("com.alutech.activechoice.dockerhub");

                        // def fetch = {
                        //     // def url = "https://blue.dockerhub.alutech.local/v2/pricing/tags/list"
                        //     def url = "https://10.100.20.33/v2/pricing/tags/list"
                        //     log.info("Openning connection...")
                        //     def httpClient = new URL(url).openConnection() as HttpURLConnection
                        //     log.info("Connection opened!")
                        //     httpClient.setRequestMethod('GET')
                        //     log.info("Method GET!")
                        //     httpClient.connect()
                        //     log.info("Connected!")
                            
                        //     if (httpClient.responseCode == 200) {
                        //         log.info("200!")
                        //         return new JsonSlurper().parseText(httpClient.inputStream.getText('UTF-8'))
                        //     } else {
                        //         log.info("Error non-200");
                        //         throw new Exception("Non 200 response: " + httpClient.responseCode)
                        //     }
                        // }

                        def fetchTags = {
                            log.info("About to fetch!");
                            def response = ["curl", "-H", "Host: blue.dockerhub.alutech.local", "-k", "https://10.100.20.33/v2/pricing/tags/list"].execute().text
                            log.info("Fetched!");
                            log.info("About to parse response: " + response);
                            response?.trim() ? new JsonSlurper().parseText(response).tags : []
                        }

                        try {      
                            log.info("Hello")

                            def tags = []
                            log.info("Fetching...");
                            fetchTags().each { tag -> 
                                if (tag == "latest") {
                                    tags.plus(0, tag)
                                    log.info("Added 'latest' tag to the beginning of the list!);
                                } else {
                                    tags.add(tag)
                                    log.info("Added tag: " + tag);
                                }
                            }
                            log.info("Fetched!");
                            return tags
                        } catch (Exception e) {
                            log.info("Error: " + e)
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
            string(name: 'IMAGE_TAG', defaultValue: 'latest', description: 'Docker Image Tag to deploy')
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