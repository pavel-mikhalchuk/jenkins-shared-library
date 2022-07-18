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
            stage('build') {
                agent { label 'java-build-16' }
                steps {
                    container('maven') {
                        script {
                            builder.mavenPackage()
                        }
                    }
                    container('docker') {
                        script {
                            builder.dockerBuild(ctx)
                            builder.dockerLoginNexus(ctx)
                            builder.dockerPush(ctx)
                        }
                    }
                }
            }
            stage('deploy-to-dev-dev') {
                agent { label 'helm-deploy' }
                steps {
                    script {
                        if (BRANCH_NAME != 'master') {
                            try {
                                timeout(time: 10, unit: 'SECONDS') {
                                    input message: "Deploy to 'dev-dev'?"
                                }
                            } catch(err) {
                                error('Aborting the build.')
                            }
                        }
                    }
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
            stage('deploy-to-prod') {
                input {
                    message "Deploy to 'prod'?"
                }
                agent { label 'helm-deploy' }
                steps {
                    deploy('prod', 'prod', deployer, ctx)
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

def deploy(env, namespace, deployer, ctx) {
    script {
        deployer.defineJavaMsPipelineDeploymentContext(env, namespace, ctx.dockerImageTag, ctx)
        deployer.checkoutInfraRepo(ctx)

        deployer.getHelmChart(ctx)
        deployer.smartCopyConfigToHelmChart(ctx)
        deployer.copyCustomKubeConfigs(ctx)
        deployer.writeRawHelmValuesYaml(helmValuesForEnvironment(env, ctx), ctx)
    }
    container('helm') {
        script {
            deployer.generateK8SManifests(ctx)
        }
    }
    script {
        deployer.pushK8SManifests(ctx)
        deployer.notifyArgoCD()
    }
}

static def helmValuesForEnvironment(env, ctx) {
    switch (env) {
        case "dev":
            return devEnvHelmValues(ctx)
        case "prod":
            return prodEnvHelmValues(ctx)
        default:
            return []
    }
}

static def devEnvHelmValues(ctx) {
    def values = [
        name: ctx.service,

        deployment: [
            image: [
                registry: 'nexus-dockerhub.alutech.local',
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
                    port: 8081
                ],
                initialDelaySeconds: 30,
                periodSeconds: 3
            ],

            livenessProbe: [
                httpGet: [
                    path: '/actuator/health/liveness',
                    port: 8081
                ],
                initialDelaySeconds: 30,
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
            enabled: true,
            host: { ingUtils-> "${ingUtils.svc_ns_inin()}" },
            annotations: [
                "kubernetes.io/ingress.class": "nginx-dev"
            ]
        ]
    ]

    return addStorageHelmValues(ctx, values)
}

static def prodEnvHelmValues(ctx) {
    def values = [
        name: ctx.service,

        deployment: [
            image: [
                registry: 'nexus-dockerhub.alutech.local',
                repository: ctx.service,
                tag: ctx.dockerImageTag,
                pullPolicy: 'IfNotPresent'
            ],

            replicaCount: 2,

            gitBranch: ctx.currentBranchName,
            jenkinsBuildNumber: ctx.jenkinsBuildNumber,

            readinessProbe: [
                httpGet: [
                    path: '/actuator/health/readiness',
                    port: 8081
                ],
                initialDelaySeconds: 30,
                periodSeconds: 3
            ],

            livenessProbe: [
                httpGet: [
                    path: '/actuator/health/liveness',
                    port: 8081
                ],
                initialDelaySeconds: 30,
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

    return addStorageHelmValues(ctx, values)
}

static def addStorageHelmValues(ctx, values) {
    if (ctx.storage) {
        values.nfs = ctx.storage
    }

    return values
}