import com.mikhalchuk.*

def call(body) {
    def ctx = setUpContext(body)

    def pipeline = new PipelineHelper(this)
    def builder = new BuildPipelineHelper(this)
    def deployer = new DeploymentPipelineHelper(this)

    pipeline {
        agent none
        options {
            buildDiscarder(logRotator(numToKeepStr: '5'))
            timestamps ()
        }
        stages {
            stage('abort-previous-builds') {
                steps {
                    pipeline.abortPreviousBuilds()
                }
            }
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
                input {
                    message "Deploy to 'dev-dev'?"
                }
                agent { label 'helm-deploy' }
                steps {
                    deploy('dev-dev', deployer, ctx)
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

def deploy(namespace, deployer, ctx) {
    script {
        deployer.defineJavaMsDeploymentContext(namespace, ctx.dockerImageTag, ctx)
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