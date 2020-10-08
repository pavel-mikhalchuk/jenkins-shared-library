package com.mikhalchuk.tests

import com.mikhalchuk.testSupport.PipelineSpockTestBase

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

import static com.mikhalchuk.tests.MockUtils.mockContainer
import static com.mikhalchuk.tests.MockUtils.mockGitRevParse

class javaMicroserviceBuildPipelineTestSpec extends PipelineSpockTestBase {

    def setup() {
        mockContainer(this)
        mockGitRevParse(this)
    }

    def "test java ms build pipeline 1.0"() {
        given:
        def pipeline = loadScript('vars/javaMicroserviceBuildPipeline.groovy')

        and:
        pipeline.getBinding().setVariable('BRANCH_NAME', "master")
        mockClock(pipeline)

        when:
        pipeline.call({
            service = P_SERVICE
            noUnitTests = P_NO_UNIT_TESTS
        })

        then:
        testNonRegression("${P_SERVICE}_no_unit_tests_${P_NO_UNIT_TESTS}")
        assertJobStatusSuccess()

        where:
        P_SERVICE | P_NO_UNIT_TESTS
        "pricing" | false
        "pricing" | true
    }

    def "test java ms build pipeline 1.1"() {
        given:
        def pipeline = loadScript('vars/javaMicroserviceBuildPipeline.groovy')

        and:
        pipeline.getBinding().setVariable('BRANCH_NAME', "master")
        mockClock(pipeline)

        when:
        pipeline.call({
            noUnitTests = P_NO_UNIT_TESTS
            containerImages = [
                [ source: './image-1', name: "${P_SERVICE}_1" ],
                [ source: './image-2', name: "${P_SERVICE}_2" ],
            ]
        })

        then:
        testNonRegression("${P_SERVICE}_no_unit_tests_${P_NO_UNIT_TESTS}")
        assertJobStatusSuccess()

        where:
        P_SERVICE | P_NO_UNIT_TESTS
        "multi-container-service" | false
        "multi-container-service" | true
    }

    def mockClock(pipeline) {
        addEnvVar('IS_CLOCK_MOCKED', 'true')
        pipeline.getBinding().setVariable('MOCKED_CLOCK',
                Clock.fixed(Instant.parse('2020-08-30T00:59:45.00Z'), ZoneId.of("UTC")))
    }
}
