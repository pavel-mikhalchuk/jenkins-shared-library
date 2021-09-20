import com.mikhalchuk.*

def call(body) {
    def ctx = setUpContext(body)

    def builder = new BuildPipelineHelper(this)
    def deployer = new DeploymentPipelineHelper(this)

    pipeline {
        agent none
        options {
            buildDiscarder(logRotator(numToKeepStr: '5'))
            timestamps ()
        }
        stages {
            stage('docker-build') {
                agent { label 'docker-build' }
                steps {
                    container('docker') {
                        script {
                            builder.dockerBuild(ctx)
                            builder.dockerPush(ctx)
                        }
                    }
                }
            }
            stage('deploy-to-dev-dev') {
                agent { label 'helm-deploy' }
                steps {
                    deploy(ctx)
                }
            }
        }
    }
}

def setUpContext(body) {
    // client-defined parameters in the body block
    def ctx = JavaMicroservicePipelineContracts.resolve(ObjUtils.closureToMap(body))

    // defining more parameters for ourselves
    ctx.dockerImages = []
    return ctx
}

def deploy(ctx) {
    script {
        deployer.defineJavaMsDeploymentContext('dev-dev', ctx.dockerImageTag, ctx)
        deployer.checkoutInfraRepo(ctx)

        deployer.copyConfigToHelmChart(ctx)
        deployer.writeHelmValuesYaml(ctx)
    }
    container('helm') {
        script {
            deployer.generateK8SManifests(ctx)
        }
    }
    script {
        deployer.pushK8SManifests(ctx)
    }
}