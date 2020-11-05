package com.mikhalchuk.tests

import com.mikhalchuk.Ingresses
import com.mikhalchuk.JavaMicroserviceDeployPipelineContracts
import spock.lang.Specification

import static org.assertj.core.api.Assertions.assertThat

class JavaMicroserviceDeployPipelineContractsTest extends Specification {

    def "test v 1.0 contract - conforms 1"() {
        given:
        def contract = [
            service: 'pricing',
            namespaces: ['dev-dev', 'tst-test'],
            podResources: PodResources.PRICING_DEV
        ]

        when:
        def conformsToContract = JavaMicroserviceDeployPipelineContracts.is_v_1_0(contract)

        then:
        assertThat(conformsToContract).isEqualTo(true)
    }

    def "test v 1.0 contract - does not conform"() {
        given:
        def contract = [
            service: 'pricing',
            ingress: [enabled: false]
        ]

        when:
        def conformsToContract = JavaMicroserviceDeployPipelineContracts.is_v_1_0(contract)

        then:
        assertThat(conformsToContract).isEqualTo(false)
    }

    def "test v 1.0 contract upgrade 1 dev env"() {
        given:
        def contract = [
            service: 'pricing',
            namespaces: ['dev-dev', 'tst-test'],
            podResources: '''resources:
  resources:
    requests:
      memory: "1500Mi"
      cpu: "100m"
    limits:
      memory: "5Gi"
      cpu: "2"
javaOpts: -Xms500m -Xmx4g -Djava.rmi.server.hostname=127.0.0.1'''
        ]

        when:
        def upgraded = JavaMicroserviceDeployPipelineContracts.upgrade_v_1_0(contract)

        then:
        assertThat(upgraded).isEqualTo(
                JavaMicroserviceDeployPipelineContracts.latestContract(
                    service: 'pricing',
                    env: 'dev',
                    namespaces: ['dev-dev', 'tst-test'],
                    helmValues: [
                        host: Ingresses.SVC_NS_IN_IN.host,
                        resources: [
                            resources: [
                                requests: [
                                    memory: '1500Mi',
                                    cpu: '100m'
                                ],
                                limits: [
                                    memory: '5Gi',
                                    cpu: '2'
                                ]
                            ]
                        ],
                        javaOpts: '-Xms500m -Xmx4g -Djava.rmi.server.hostname=127.0.0.1'
                    ]
                )
        )
    }

    def "test v 1.0 contract upgrade 1 dev prod"() {
        given:
        def contract = [
            service: 'pricing',
            namespaces: ['prod'],
            podResources: '''resources:
  resources:
    requests:
      memory: "1500Mi"
      cpu: "100m"
    limits:
      memory: "5Gi"
      cpu: "2"
javaOpts: -Xms500m -Xmx4g -Djava.rmi.server.hostname=127.0.0.1'''
        ]

        when:
        def upgraded = JavaMicroserviceDeployPipelineContracts.upgrade_v_1_0(contract)

        then:
        assertThat(upgraded).isEqualTo(
                JavaMicroserviceDeployPipelineContracts.latestContract(
                    service: 'pricing',
                    env: 'prod',
                    namespaces: ['prod'],
                    helmValues: [
                        resources: [
                            resources: [
                                requests: [
                                    memory: '1500Mi',
                                    cpu: '100m'
                                ],
                                limits: [
                                    memory: '5Gi',
                                    cpu: '2'
                                ]
                            ]
                        ],
                        javaOpts: '-Xms500m -Xmx4g -Djava.rmi.server.hostname=127.0.0.1'
                    ]
                )
        )
    }

    def "test v 1.1 contract - conforms 1"() {
        given:
        def contract = [
            service: 'pricing',
            env: 'prod',
            namespaces: ['dev-dev', 'tst-test'],
            ingress: Ingresses.SVC_PROD,
            podResources: PodResources.PRICING_DEV
        ]

        when:
        def conformsToContract = JavaMicroserviceDeployPipelineContracts.is_v_1_1(contract)

        then:
        assertThat(conformsToContract).isEqualTo(true)
    }

    def "test v 1.1 contract - does not conform"() {
        given:
        def contract = [
            service: 'pricing',
            namespaces: ['dev-dev', 'tst-test'],
            ingress: Ingresses.SVC_PROD,
            podResources: PodResources.PRICING_DEV
        ]

        when:
        def conformsToContract = JavaMicroserviceDeployPipelineContracts.is_v_1_1(contract)

        then:
        assertThat(conformsToContract).isEqualTo(false)
    }

    def "test v 1.1 contract upgrade 1 dev env"() {
        given:
        def contract = [
            service: 'pricing',
            env: 'dev',
            namespaces: ['dev-dev', 'tst-test'],
            ingress: Ingresses.SVC_NS_IN_IN,
            podResources: '''resources:
  resources:
    requests:
      memory: "1500Mi"
      cpu: "100m"
    limits:
      memory: "5Gi"
      cpu: "2"
javaOpts: -Xms500m -Xmx4g -Djava.rmi.server.hostname=127.0.0.1'''
        ]

        when:
        def upgraded = JavaMicroserviceDeployPipelineContracts.upgrade_v_1_1(contract)

        then:
        assertThat(upgraded).isEqualTo(
                JavaMicroserviceDeployPipelineContracts.latestContract(
                    service: 'pricing',
                    env: 'dev',
                    namespaces: ['dev-dev', 'tst-test'],
                    helmValues: [
                        host: Ingresses.SVC_NS_IN_IN.host,
                        resources: [
                            resources: [
                                requests: [
                                    memory: '1500Mi',
                                    cpu: '100m'
                                ],
                                limits: [
                                    memory: '5Gi',
                                    cpu: '2'
                                ]
                            ]
                        ],
                        javaOpts: '-Xms500m -Xmx4g -Djava.rmi.server.hostname=127.0.0.1'
                    ]
                )
        )
    }

    def "test v 1.1 contract upgrade 1 dev prod"() {
        given:
        def contract = [
            service: 'pricing',
            env: 'prod',
            namespaces: ['prod'],
            ingress: Ingresses.SVC_PROD,
            podResources: '''resources:
  resources:
    requests:
      memory: "1500Mi"
      cpu: "100m"
    limits:
      memory: "5Gi"
      cpu: "2"
javaOpts: -Xms500m -Xmx4g -Djava.rmi.server.hostname=127.0.0.1'''
        ]

        when:
        def upgraded = JavaMicroserviceDeployPipelineContracts.upgrade_v_1_1(contract)

        then:
        assertThat(upgraded).isEqualTo(
                JavaMicroserviceDeployPipelineContracts.latestContract(
                    service: 'pricing',
                    env: 'prod',
                    namespaces: ['prod'],
                    helmValues: [
                        host: Ingresses.SVC_PROD.host,
                        resources: [
                            resources: [
                                requests: [
                                    memory: '1500Mi',
                                    cpu: '100m'
                                ],
                                limits: [
                                    memory: '5Gi',
                                    cpu: '2'
                                ]
                            ]
                        ],
                        javaOpts: '-Xms500m -Xmx4g -Djava.rmi.server.hostname=127.0.0.1'
                    ]
                )
        )
    }

    def "test v 1.2 contract - conforms 1"() {
        given:
        def contract = [
            service: 'pricing',
            env: 'dev',
            namespaces: ['dev-dev', 'tst-test'],
            helmValues: [
                host: { ingUtils-> "${ingUtils.svc_ns_inin()}" },
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
        def conformsToContract = JavaMicroserviceDeployPipelineContracts.is_v_1_2(contract)

        then:
        assertThat(conformsToContract).isEqualTo(true)
    }

    def "test v 1.2 contract - conforms 2"() {
        given:
        def contract = [
            service: 'pricing',
            env: 'dev',
            namespaces: ['dev-dev', 'tst-test']
        ]

        when:
        def conformsToContract = JavaMicroserviceDeployPipelineContracts.is_v_1_2(contract)

        then:
        assertThat(conformsToContract).isEqualTo(true)
    }

    def "test v 1.2 contract - conforms 3"() {
        given:
        def contract = [
            service: 'aservice',
            env: 'dev',
            namespaces: ['dev-dev', 'tst-test'],
            preDeploy: {
                sh 'pre-deploy-script.sh'
            },
            helmValues: [
                host: { ingUtils-> "${ingUtils.svc_ns_inin()}" },
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
        def conformsToContract = JavaMicroserviceDeployPipelineContracts.is_v_1_2(contract)

        then:
        assertThat(conformsToContract).isEqualTo(true)
    }

    def "test v 1.2 contract - does not conform"() {
        given:
        def contract = [
            service: 'pricing',
            namespaces: ['dev-dev', 'tst-test']
        ]
        when:
        def conformsToContract = JavaMicroserviceDeployPipelineContracts.is_v_1_2(contract)

        then:
        assertThat(conformsToContract).isEqualTo(false)
    }
}