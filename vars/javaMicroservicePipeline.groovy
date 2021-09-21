import com.mikhalchuk.*

def call(body) {
    def ctx = setUpContext(body)

    def helper = new PipelineHelper(this)
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
                    script {
                        helper.abortPreviousBuilds()
                    }
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
                agent { label 'helm-deploy' }
                steps {
                    deploy('dev', 'dev-dev', deployer, ctx)
                }
            }
            stage('deploy-to-tst-test') {
                input {
                    message "Deploy to 'tst-test'?"
                }
                agent { label 'helm-deploy' }
                steps {
                    deploy('dev', 'tst-test', deployer, ctx)
                }
            }
//            stage('deploy-to-prod') {
//                input {
//                    message "Deploy to 'prod'?"
//                }
//                agent { label 'helm-deploy' }
//                steps {
//                    deploy('prod', 'prod', deployer, ctx)
//                }
//            }
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

def deploy(env, namespace, deployer, ctx) {
    script {
        deployer.defineJavaMsDeploymentContext(env, namespace, ctx.dockerImageTag, ctx)
        deployer.checkoutInfraRepo(ctx)

        deployer.getHelmChart(ctx)
        defineHelmValues(ctx)
        deployer.copyConfigToHelmChart(ctx)
        deployer.writeHelmValuesYaml(ctx, false)
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

def defineHelmValues(ctx) {
    ctx.helmValues = [
        name: ctx.service,

        deployment: [
            image: [
                registry: 'dockerhub-vip.alutech.local',
                repository: ctx.service,
                tag: ctx.dockerImageTag,
                pullPolicy: 'IfNotPresent'
            ],

            replicaCount: 2,

            gitBranch: ctx.currentBranchName,
            jenkinsBuildNumber: ctx.jenkinsBuildNumber,

            nodeSelector: [
                runtime: 'java'
            ],

            readinessProbe: [
                httpGet: [
                    path: '/actuator/health/readiness',
                    port: 8080
                ],
                initialDelaySeconds: 3,
                periodSeconds: 3
            ],

            livenessProbe: [
                httpGet: [
                    path: '/actuator/health/liveness',
                    port: 8080
                ],
                initialDelaySeconds: 3,
                periodSeconds: 3
            ],
            resources: [
                requests: [
                    memory: '1Gi',
                    cpu: 1
                ],
                limits: [
                    memory: '2Gi',
                    cpu: 2
                ]
            ]
        ],

        service: [
            externalPort: 80,
            internalPort: 8080,
            metricsPort: 8081,
        ],

        ingress: [
            enabled: false
        ]
    ]
}