package com.mikhalchuk.tests

import com.mikhalchuk.testSupport.PipelineSpockTestBase

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class npmInstallTestSpec extends PipelineSpockTestBase {

    def setup() {
    }

    def "test"() {
        given:
        def npmInstall = loadScript('vars/npmInstall.groovy')

        and:
        npmInstall.getBinding().setVariable('JOB_NAME', "npm-project/master")

        when:
        npmInstall()

        then:
        testNonRegression()
        assertJobStatusSuccess()
    }

    def "test with specified node_modules cache path"() {
        given:
        def npmInstall = loadScript('vars/npmInstall.groovy')

        and:
        npmInstall.getBinding().setVariable('JOB_NAME', "npm-project/master")

        when:
        npmInstall {
            nodeModulesCachePath = '/super/path'
        }

        then:
        testNonRegression("specific_node_modules_cache_path")
        assertJobStatusSuccess()
    }

    def mockClock(pipeline) {
        addEnvVar('IS_CLOCK_MOCKED', 'true')
        pipeline.getBinding().setVariable('MOCKED_CLOCK',
                Clock.fixed(Instant.parse('2020-08-30T00:59:45.00Z'), ZoneId.of("UTC")))
    }
}
