package com.mikhalchuk.tests

import com.mikhalchuk.testSupport.PipelineSpockTestBase

import static com.mikhalchuk.tests.MockUtils.*

class javaMicroservicePipelineTestSpec extends PipelineSpockTestBase {

    def setup() {
        mockContainer(this)
        mockGit(this)
        mockInput(this)
        mockMessage(this)
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

    void mockCurrentBuildDetails(Script pipeline) {
        pipeline.currentBuild.rawBuild = []
        pipeline.currentBuild.rawBuild.metaClass.getPreviousBuildInProgress = { return null }
    }
}