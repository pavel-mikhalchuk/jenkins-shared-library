package com.mikhalchuk.tests

import com.mikhalchuk.HelmChartDeployPipelineContracts
import spock.lang.Specification

import static org.assertj.core.api.Assertions.assertThat

class HelmChartDeployPipelineContractsTest extends Specification {

    def "test v 1.0 contract - conforms 1"() {
        given:
        def contract = [
            service: 'super-service',
            env: 'dev',
            namespaces: ['dev-dev', 'tst-test'],
            helmValues: [
                host: { ingUtils -> "${ingUtils.svc_ns_inin()}" },
                resources: [
                    requests: [
                        memory: '1500Mi',
                        cpu: '100m'
                    ],
                    limits: [
                        memory: '5Gi',
                        cpu: '2'
                    ]
                ],
            ]
        ]

        when:
        def conformsToContract = HelmChartDeployPipelineContracts.is_v_1_0(contract)

        then:
        assertThat(conformsToContract).isEqualTo(true)
    }

    def "test v 1.0 contract - conforms 2"() {
        given:
        def contract = [
            service: 'pricing',
            env: 'dev',
            namespaces: ['dev-dev', 'tst-test']
        ]

        when:
        def conformsToContract = HelmChartDeployPipelineContracts.is_v_1_0(contract)

        then:
        assertThat(conformsToContract).isEqualTo(true)
    }

    def "test v 1.0 contract - does not conform"() {
        given:
        def contract = [
            service: 'pricing',
            namespaces: ['dev-dev', 'tst-test']
        ]
        when:
        def conformsToContract = HelmChartDeployPipelineContracts.is_v_1_0(contract)

        then:
        assertThat(conformsToContract).isEqualTo(false)
    }
}