package com.mikhalchuk

class DeploymentPipelineHelper {

    def pipeline

    public DeploymentPipelineHelper(pipeline) {
        this.pipeline = pipeline
    }

    def defineMoreContextBasedOnUserInput(ctx) {
        defineJavaMsDeploymentContext(
                ctx.env,
                pipeline.params.NAMESPACE,
                "${ctx.service}:${pipeline.params.IMAGE_TAG}",
                ctx
        )
    }

    def defineJavaMsPipelineDeploymentContext(env, namespace, dockerImageTag, ctx) {
        defineJavaMsDeploymentContext(env, namespace, dockerImageTag, ctx)

        ctx.helmChartFolder = "kubernetes-${ctx.timestamp}/helm-chart/${ctx.service}"
    }

    def defineJavaMsDeploymentContext(env, namespace, dockerImageTag, ctx) {
        validateInputParameters(dockerImageTag)

        ctx.env = env
        ctx.namespace = namespace
        ctx.dockerImage = dockerImageTag
        ctx.jenkinsBuildNumber = "${pipeline.JOB_NAME}-${pipeline.BUILD_NUMBER}"
        ctx.currentBranchName = "${pipeline.BRANCH_NAME}"

        ctx.timestamp = pipeline.sh(script: 'echo $(date +"%d-%m-%Y_%H-%M-%S")', returnStdout: true).trim()

        ctx.infraFolder = "infra-${ctx.timestamp}"
        ctx.helmChartFolder = "kubernetes/helm-chart/${ctx.service}"
        ctx.helmRelease = "${ctx.service}-${ctx.namespace}"

        //// env-specific (dev VS prod)
        ctx.kubeStateFolder = "${ctx.infraFolder}/kube-${ctx.env}/cluster-state/alutech-services/${ctx.namespace}/${ctx.service}/raw-manifests"
        ////  ////  //// //// //// ////////  ////  //// //// //// ////
    }

    def validateInputParameters(dockerImageTag) {
        if (dockerImageTag == '') {
            error "You didn\'t enter a IMAGE_TAG parameter!"
        }
    }

    def notifySlack(ctx) {
        pipeline.script {
            pipeline.wrap([$class: 'BuildUser']) {
                pipeline.slackSend channel: "${ctx.slackChannel}", color: "good", message: "*${pipeline.BUILD_USER}* rolls branch *${ctx.currentBranchName}* on *${ctx.service} ${ctx.namespace}*.\n${ctx.dockerImage}\nKeep calm!"
            }
        }
    }

    def checkoutInfraRepo(ctx) {
        pipeline.sh "mkdir ${ctx.infraFolder}"

        pipeline.dir("${ctx.infraFolder}") {
            pipeline.git credentialsId: 'jenkins', url: 'http://bb.alutech-mc.com:8080/scm/as/infra.git'
        }
    }

    def getHelmChart(ctx) {
        pipeline.sh "mkdir -p ${ctx.helmChartFolder}"
        pipeline.sh "cp -r ${ctx.infraFolder}/helm-charts/java/microservice/* ${ctx.helmChartFolder}"
    }

    def copyConfigToHelmChart(ctx) {
        pipeline.sh "cp src/main/resources/application.${ctx.namespace}.properties ${ctx.helmChartFolder}/application.properties"
    }

    def smartCopyConfigToHelmChart(ctx) {
        pipeline.sh "find . -name application.${ctx.namespace}.properties -type f -exec cp {} ${ctx.helmChartFolder}/application.properties \";\""
        pipeline.sh "find . -name application.${ctx.namespace}.yaml -type f -exec cp {} ${ctx.helmChartFolder}/application.yaml \";\""
    }

    def copyCustomKubeConfigs(ctx) {
        pipeline.sh "cp -r kubernetes/ ${ctx.helmChartFolder}/templates || true"
    }

    def writeHelmValuesYaml(ctx) {
        def jenkinsFileHelmValues = ClosureUtils.maybeResolve(ctx, ctx.helmValues)
        def userInputHelmValues = Yaml.parse(pipeline.params.RESOURCES)

        writeRawHelmValuesYaml(merge(defaultValues(ctx), merge(jenkinsFileHelmValues, userInputHelmValues)), ctx)
    }

    def writeRawHelmValuesYaml(helmValuesMap, ctx) {
        pipeline.writeFile file: "${ctx.helmChartFolder}/values.yaml", text: helmValues(helmValuesMap, ctx)
    }

    def helmValues(helmValues, ctx) {
        Yaml.write(resolveClosureValues(ctx, helmValues)).trim()
    }

    def resolveClosureValues(ctx, helmValues) {
        ObjUtils.walk(helmValues, { res, key, value ->
            if (value instanceof Closure) {
                def v = resolveClosureValue(ctx, value)

                // Fix for mistakenly wrap ingress host with quotes on the client side.
                // Host can be a list so wrapping it would break Kube ingress object
                if (key == 'host' || key == 'hostin' || key == 'hostGates') {
                    if (v instanceof String || v instanceof GString) {
                        // Check if it is an array
                        if (v[0] == '[' && v[v.length() - 1] == ']') {
                            res[key] = v.toString()
                                    .substring(1, v.length() - 1)
                                    .split(",")
                                    .collect {it.trim()}
                        } else {
                            res[key] = v
                        }
                    } else {
                        res[key] = v
                    }
                } else {
                    res[key] = v
                }
            } else {
                res[key] = value
            }
        })
    }

    def resolveClosureValue(ctx, value) {
        try {
            resolveIngressHost(value, ctx)
        } catch (Exception e) {
            ClosureUtils.invoke(ctx, value, pipeline)
        }
    }

    def static defaultValues(ctx) {
        [
            replicaCount: 1,
            gitBranch: ctx.currentBranchName,
            image: [
                repository: 'nexus-dockerhub.alutech.local',
                tag: ctx.dockerImage,
                pullPolicy: 'IfNotPresent'
            ],
            service: [
                externalPort: 80,
                internalPort: 8080
            ],
            jenkinsBuildNumber: ctx.jenkinsBuildNumber,
            environment: ctx.env == 'prod' ? "${ctx.env}" : ""
        ]
    }

    def merge(Map lhs, Map rhs) {
        return rhs.inject(lhs.clone()) { map, entry ->
            if (map[entry.key] instanceof Map && entry.value instanceof Map) {
                map[entry.key] = merge(map[entry.key], entry.value)
            } else if (map[entry.key] instanceof Collection && entry.value instanceof Collection) {
                map[entry.key] += entry.value
            } else {
                map[entry.key] = entry.value
            }
            return map
        }
    }

    def preDeploy(ctx) {
        ClosureUtils.invoke(ctx, ctx.preDeploy, pipeline)
    }

    def generateK8SManifests(ctx) {
        pipeline.sh "rm -rf ${ctx.kubeStateFolder}"
        pipeline.sh "mkdir -p ${ctx.kubeStateFolder}"
        pipeline.sh "helm template --namespace ${ctx.namespace} --name ${ctx.helmRelease} ${ctx.helmChartFolder} > '${ctx.kubeStateFolder}/kube-state.yaml'"
    }

    def pushK8SManifests(ctx) {
        pipeline.dir("${ctx.infraFolder}") {
            pipeline.withCredentials([pipeline.usernamePassword(credentialsId: 'jenkins', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                pipeline.sh 'git config user.email "jenkins@infra.tower"'
                pipeline.sh 'git config user.name "Jenkins"'

                pipeline.sh 'git checkout master'
                pipeline.sh 'git pull http://${GIT_USERNAME}:${GIT_PASSWORD}@bb.alutech-mc.com:8080/scm/as/infra.git HEAD:master'

                pipeline.sh 'git add *'
                pipeline.sh 'git commit -m "[jenkins]: ${JOB_NAME} - ${BUILD_NUMBER}"'
                pipeline.sh 'git push http://${GIT_USERNAME}:${GIT_PASSWORD}@bb.alutech-mc.com:8080/scm/as/infra.git HEAD:master'
            }
        }
    }

    def notifyArgoCD() {
        pipeline.sh 'curl -k -X POST https://git-events-publisher.in.in.alutech24.com/push'
    }

    def static resolveIngressHost(host, ctx) {
        if (host instanceof Closure) {
            def ingUtils = [
                svc_ns_inin: {
                    return IngressUtils.resolveNsInIn(ctx, ctx.service)
                },
                str_ns_inin: { str ->
                    return IngressUtils.resolveNsInIn(ctx, str)
                },
                svc_prod: {
                    return IngressUtils.resolve(ctx, ctx.service)
                },
                str_prod: { str ->
                    return IngressUtils.resolve(ctx, str)
                }
            ]
            return host.call(ingUtils)
        } else {
            return host
        }
    }

    def initDockerImageChoiceParameter(ctx) {
        pipeline.properties([
                pipeline.parameters([
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
                                        script: [classpath: [], sandbox: true, script: """
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
                            log.info("Start fetching tags from 'https://nexus-dockerhub.alutech.local/v2/${ctx.service}/tags/list'")

                            Process process = ["curl", "-k", "https://nexus-dockerhub.alutech.local/v2/${ctx.service}/tags/list"].execute()
                            
                            def out = new StringBuffer()
                            def err = new StringBuffer()
                            
                            process.consumeProcessOutput( out, err )
                            process.waitFor()
                            
                            if (out.size() > 0) {
                              log.info("Response from docker hub: " + out.toString())
                              
                              return parse(out.toString())
                            } else if (err.size() > 0) {
                              log.info(err.toString())

                              return ["there was an error during pulling data from dockerhub"]
                            }
                        } catch (Exception e) {
                            e.printStackTrace()
                        }
                    """]
                                ]
                        ]
                ])
        ])
    }
}
