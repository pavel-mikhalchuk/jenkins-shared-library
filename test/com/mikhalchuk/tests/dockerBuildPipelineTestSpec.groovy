package com.mikhalchuk.tests

import com.mikhalchuk.testSupport.PipelineSpockTestBase

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

import static com.mikhalchuk.tests.MockUtils.mockContainer
import static com.mikhalchuk.tests.MockUtils.mockGitRevParse

class dockerBuildPipelineTestSpec extends PipelineSpockTestBase {

    def setup() {
        mockContainer(this)
        mockGitRevParse(this)
    }

    def "test docker build pipeline 1.0"() {
        given:
        def pipeline = loadScript('vars/dockerBuildPipeline.groovy')

        and:
        pipeline.getBinding().setVariable('BRANCH_NAME', "super/master")
        mockClock(pipeline)

        when:
        pipeline.call({
            containerImages = [
                [ source: '.', name: "service" ]
            ]
        })

        then:
        testNonRegression("service")
        assertJobStatusSuccess()
    }

    def mockClock(pipeline) {
        addEnvVar('IS_CLOCK_MOCKED', 'true')
        pipeline.getBinding().setVariable('MOCKED_CLOCK',
                Clock.fixed(Instant.parse('2020-08-30T00:59:45.00Z'), ZoneId.of("UTC")))
    }
}
