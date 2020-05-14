def call(body) {
    def ctx = setUpContext(body)

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
        environment {
            defineMoreContextBasedOnUserInput(ctx)
        }
        stages {
            // https://gist.github.com/m-x-k/15d74f6b5cd1e7531b9b1130710c1546

            stage('notify slack: DEPLOYMENT STARTED') {
                steps {
                    // ...
                }
            }
            stage('generate K8S manifests') {
              steps {
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
    ctx.jenkinsBuildNumber = "${env.JOB_NAME}-${env.BUILD_NUMBER}"
    ctx.podResources = "${params.RESOURCES}"
}

def copyConfigToHelmChart(ctx) {
    sh "cp src/main/resources/application.${ctx.namespace}.properties kubernetes/helm-chart/${ctx.service}/application.properties"
}

def writeHelmValuesYaml(ctx) {
    writeFile file: "kubernetes/helm-chart/${ctx.service}/values.yaml", text: 
        
        """replicaCount: 1
        gitBranch: ${currentGitBranch()}
        image:
          repository: dockerhub-vip.alutech.local
          tag: ${ctx.dockerImage}
          pullPolicy: IfNotPresent
        service:
          externalPort: 80
          internalPort: 8080
        jenkinsBuildNumber: ${ctx.jenkinsBuildNumber}
        host: ${hostName()}
        ${ctx.podResources}"""
}

def currentGitBranch() {
    sh(script: 'echo $(git rev-parse --abbrev-ref HEAD)', returnStdout: true).trim()
}

def hostName() {
    return "${APP_NAME}.${hostByNs(NAMESPACE)}.in.in.alutech24.com"
}

def hostByNs(ns) {
    return ns.contains('-') ? "${ns.split('-')[1]}.${ns.split('-')[0]}" : ns
}