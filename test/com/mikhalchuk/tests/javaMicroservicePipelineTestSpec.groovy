package com.mikhalchuk.tests

import com.mikhalchuk.testSupport.PipelineSpockTestBase

import static com.mikhalchuk.tests.MockUtils.*

class javaMicroservicePipelineTestSpec extends PipelineSpockTestBase {

    def setup() {
        mockContainer(this)
        mockGit(this)
        mockInput(this)
        mockMessage(this)
        mockDateShellCommand(this)
    }

    def "test java ms pipeline 1.0"() {
        given:
        def pipeline = loadScript('vars/javaMicroservicePipeline.groovy')

        and:
        mockCurrentBuildDetails(pipeline)
        mockClock(pipeline, this)
        pipeline.getBinding().setVariable('JOB_NAME', "java-micro-service")
        pipeline.getBinding().setVariable('BUILD_NUMBER', '666')
        pipeline.getBinding().setVariable('BRANCH_NAME', "master")

        when:
        pipeline {
            service = 'java-micro-service'
            containerImages = [
                    [source: '.', name: 'java-micro-service']
            ]
        }

        then:
        testNonRegression()
        assertJobStatusSuccess()
    }

    def "test java ms pipeline 1.0 with storage enabled"() {
        given:
        def pipeline = loadScript('vars/javaMicroservicePipeline.groovy')

        and:
        mockCurrentBuildDetails(pipeline)
        mockClock(pipeline, this)
        pipeline.getBinding().setVariable('JOB_NAME', "java-micro-service")
        pipeline.getBinding().setVariable('BUILD_NUMBER', '666')
        pipeline.getBinding().setVariable('BRANCH_NAME', "master")

        when:
        pipeline {
            service = 'java-micro-service'
            containerImages = [
                    [source: '.', name: 'java-micro-service']
            ]
            storage = [
                    enabled: true,
                    size: '1Gi'
            ]
        }

        then:
        testNonRegression("nfs_enabled")
        assertJobStatusSuccess()
    }

    def "test java ms pipeline 2.0"() {
        given:
        def pipeline = loadScript('vars/javaMicroservicePipeline.groovy')

        and:
        mockCurrentBuildDetails(pipeline)
        mockClock(pipeline, this)
        pipeline.getBinding().setVariable('JOB_NAME', "java-micro-service")
        pipeline.getBinding().setVariable('BUILD_NUMBER', '666')
        pipeline.getBinding().setVariable('BRANCH_NAME', "master")

        when:
        pipeline {
            service = 'java-micro-service'
            containerImages = [
                    [source: '.', name: 'java-micro-service']
            ]
            helmValues = [
                    'dev-dev' : [
                            replicaCount : 2,
                            resources: [
                                    requests: [
                                            memory: '1Gi',
                                            cpu   : 1
                                    ],
                                    limits  : [
                                            memory: '2Gi',
                                            cpu   : 2
                                    ]
                            ],
                            javaOpts : '-Xmx1g'
                    ],
                    'tst-test': [
                            replicaCount : 2,
                            resources: [
                                    requests: [
                                            memory: '1Gi',
                                            cpu   : 1
                                    ],
                                    limits  : [
                                            memory: '2Gi',
                                            cpu   : 2
                                    ]
                            ],
                            javaOpts : '-Xmx1g'
                    ],
                    'prod': [
                            replicaCount : 2,
                            resources: [
                                    requests: [
                                            memory: '1Gi',
                                            cpu   : 1
                                    ],
                                    limits  : [
                                            memory: '2Gi',
                                            cpu   : 2
                                    ]
                            ],
                            javaOpts : '-Xmx1g'
                    ]
            ]
        }

        then:
        testNonRegression()
        assertJobStatusSuccess()
    }

    void mockCurrentBuildDetails(Script pipeline) {
        pipeline.currentBuild.rawBuild = []
        pipeline.currentBuild.rawBuild.metaClass.getPreviousBuildInProgress = { return null }
    }
}