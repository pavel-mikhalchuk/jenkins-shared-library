package com.mikhalchuk.tests

import com.mikhalchuk.testSupport.PipelineSpockTestBase

import static com.mikhalchuk.tests.MockUtils.mockContainer
import static com.mikhalchuk.tests.MockUtils.mockGitRevParse

class javaMicroserviceBuildPipelineTestSpec extends PipelineSpockTestBase {

    def setup() {
        mockContainer(this)
        mockGitRevParse(this)
    }

    def "test java ms build pipeline"() {
        given:
        def pipeline = loadScript('vars/javaMicroserviceBuildPipeline.groovy')

        and:
        pipeline.getBinding().setVariable('BRANCH_NAME', "master")

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
}
