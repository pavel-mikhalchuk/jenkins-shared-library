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
//        pipeline.getBinding().setVariable('BUILD_USER', 'mikhalchuk')

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

    void mockCurrentBuildDetails(Script pipeline) {
        pipeline.currentBuild.rawBuild = []
        pipeline.currentBuild.rawBuild.metaClass.getPreviousBuildInProgress = { return null }
    }
}