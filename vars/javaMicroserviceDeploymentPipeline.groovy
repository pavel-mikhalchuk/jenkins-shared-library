def call(body) {
    def ctx = setUpContext(body)

    // def image = "IMAGE"
    // def url = "https://blue.dockerhub.alutech.local/v2/${image}/tags/list"
    // def list = getDockerImageTags(url)
    // list = sortReverse(list)
    // def versions = list.join("\n")
    // def userInput = input(
    //     id: 'userInput', message: 'Promote:', parameters: [
    //         [$class: 'ChoiceParameterDefinition', choices: versions, description: 'Versions', name: 'version']
    //     ]
    // )        


    properties([
        parameters([
            [
                $class: 'ChoiceParameter', 
                choiceType: 'PT_SINGLE_SELECT', 
                description: '', 
                filterLength: 1, 
                filterable: false, 
                name: 'DockerTags', 
                randomName: 'choice-parameter-18690860397501990', 
                script: [
                    $class: 'GroovyScript', 
                    fallbackScript: [classpath: [], sandbox: true, script: '["error :("]'], 
                    script: [classpath: [], sandbox: true, script: '''
                        import java.util.logging.Level; import java.util.logging.Logger;

                        def LOGGER = Logger.getLogger("org.biouno.myscript");
                        LOGGER.info("Hello");
                        LOGGER.log(Level.INFO, "Hello", exception);
                        
                        try {
                            ["123123123123", "dsa"]
                            //return getDockerImageTags("")
                        } catch (Exception e) {
                            print "There was a problem running the script. " + e
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


@NonCPS
def sortReverse(list) {
    list.reverse()
}

def getDockerImageTags(url) {
    // def myjson = getUrl(url)
    // def json = jsonParse(myjson);
    // def tags = json.tags

    echo tags
    echo "yeah!!!"

    ["asd", "dsa"]
}

def jsonParse(json) {
    new groovy.json.JsonSlurper().parseText(json)
}

def getUrl(url) {
    sh(returnStdout: true, script: "curl -s ${url} 2>&1 | tee result.json")
    def data = readFile('result.json').trim()
    data
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