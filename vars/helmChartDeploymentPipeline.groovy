import com.mikhalchuk.*

def call(body) {
    def ctx = setUpContext(body)

    def helper = new DeploymentPipelineHelper(this)

    helper.initDockerImageChoiceParameter(ctx)

    pipeline {
        agent { label 'helm-deploy' }
        options {
            buildDiscarder(logRotator(numToKeepStr: '5'))
            timestamps ()
        }
        parameters {
            choice(name: 'NAMESPACE', choices: ctx.namespaces, description: 'Kubernetes Namespace')
            text(name: 'RESOURCES', defaultValue: ctx.resources, description: 'Kubernetes POD resources requests and limits')
        }
        stages {
            stage('notify slack') {
                steps {
                    script {
                        // This step is very important!!!
                        // Please do not remove it unless you find a better way without introducing "Init" stage because it's ugly :)"
                        // Later stages depend on it.
                        helper.defineMoreContextBasedOnUserInput(ctx)

                        helper.notifySlack(ctx)
                    }
                }
            }
            stage('checkout infra repo') {
                steps {
                    script {
                        helper.checkoutInfraRepo(ctx)
                    }
                }
            }
            stage('generate K8S manifests') {
                steps {
                    container('helm') {
                        script {
                            helper.writeHelmValuesYaml(ctx)
                            helper.generateK8SManifests(ctx)
                        }
                    }
                }
            }
            stage('push K8S manifests to infra repo') {
                steps {
                    script {
                        helper.pushK8SManifests(ctx)
                    }
                }
            }
            stage('notify ArgoCD') {
                steps {
                    script {
                        helper.notifyArgoCD()
                    }
                }
            }
        }
    }
}

def setUpContext(body) {
    // client-defined parameters in the body block
    def ctx = HelmChartDeployPipelineContracts.resolve(ObjUtils.closureToMap(body))

    ctx.slackChannel = ctx.slackChannel ?: 'java_services'
    ctx.resources = Yaml.write([resources: ctx.helmValues.resources])

    // defining more parameters for ourselves
    return ctx
}