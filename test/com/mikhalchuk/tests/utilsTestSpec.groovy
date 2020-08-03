package com.mikhalchuk.tests

import com.mikhalchuk.testSupport.PipelineSpockTestBase

import static org.assertj.core.api.Assertions.assertThat

class utilsTestSpec extends PipelineSpockTestBase {

    def "test timestamped"() {
        given:
        addParam('STRING', P_STRING)

        and:
        def shellMock = Mock(Closure)
        helper.registerAllowedMethod("sh", [Map.class], shellMock)

        when:
        def utils = loadScript('vars/utils.groovy')

        and:
        utils.timestamped(P_STRING)

        then:
        1 * shellMock.call(_) >> { List args ->

            def cmd = args[0][0]['script'].toString()
            assertThat(cmd).isEqualTo('echo ' + P_STRING + '_$(date +"%d-%m-%Y_%H-%M-%S")')

            return ""
        }

        where:
        P_STRING  | _
        "infra"   | _
        "jenkins" | _
    }
}