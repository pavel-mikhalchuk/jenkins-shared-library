package com.mikhalchuk.tests

import com.mikhalchuk.Yaml
import spock.lang.Specification

import static org.assertj.core.api.Assertions.assertThat

class YamlTest extends Specification {

    static def YAML_1 = '''resources:
  resources:
    requests: 
      memory: "1500Mi"
      cpu: "100m"
    limits:
      memory: "5Gi"
      cpu: "2"
javaOpts: "-Xms500m -Xmx4g"'''

    static def MAP_1 = [
        resources: [
            resources: [
                requests: [
                    memory: '1500Mi',
                    cpu   : '100m'
                ],
                limits  : [
                    memory: '5Gi',
                    cpu   : '2'
                ]
            ]
        ],
        javaOpts : '-Xms500m -Xmx4g'
    ]

    static def YAML_2 = '''resources:
  resources:
    requests:
      memory: "4Gi"
      cpu: "100m"
    limits:
      memory: "5Gi"
      cpu: "2"
javaOpts: "-Xms4g -Xmx4g -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.rmi.port=9010 -Djava.rmi.server.hostname=127.0.0.1 -server -XX:MaxMetaspaceSize=1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:ParallelGCThreads=20 -XX:ConcGCThreads=5 -XX:InitiatingHeapOccupancyPercent=70 -Djdk.nio.maxCachedBufferSize=262144 -XX:HeapDumpPath=/aservice-images/oom-dumps/"'''

    static def MAP_2 = [
        resources: [
            resources: [
                requests: [
                    memory: '4Gi',
                    cpu   : '100m'
                ],
                limits  : [
                    memory: '5Gi',
                    cpu   : '2'
                ]
            ]
        ],
        javaOpts : '-Xms4g -Xmx4g -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.rmi.port=9010 -Djava.rmi.server.hostname=127.0.0.1 -server -XX:MaxMetaspaceSize=1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:ParallelGCThreads=20 -XX:ConcGCThreads=5 -XX:InitiatingHeapOccupancyPercent=70 -Djdk.nio.maxCachedBufferSize=262144 -XX:HeapDumpPath=/aservice-images/oom-dumps/'
    ]

    def "test parse"() {
        given:
        def yaml = P_YAML
        def expected = P_MAP

        when:
        def map = Yaml.parse(yaml)

        then:
        assertThat(map).isEqualTo(expected)

        where:
        P_YAML | P_MAP
        YAML_1 | MAP_1
        YAML_2 | MAP_2
    }

    def "test write strings"() {
        given:
        def map = [
                resources: [
                        resources: [
                                requests: [
                                        memory: '1500Mi',
                                        cpu   : '100m'
                                ],
                                limits  : [
                                        memory: '5Gi',
                                        cpu   : '2'
                                ]
                        ]
                ],
                javaOpts : null
        ]

        when:
        def yaml = Yaml.write(map)

        then:
        assertThat(yaml).isEqualTo(
                '''resources:
  resources:
    requests:
      memory: "1500Mi"
      cpu: "100m"
    limits:
      memory: "5Gi"
      cpu: "2"
javaOpts: null'''
        )
    }

    def "test write integers"() {
        given:
        def map = [
                livenessProbe: [
                        httpGet: [
                                path: '/actuator/health/liveness',
                                port: 8081
                        ]
                ]
        ]

        when:
        def yaml = Yaml.write(map)

        then:
        assertThat(yaml).isEqualTo(
                '''livenessProbe:
  httpGet:
    path: "/actuator/health/liveness"
    port: 8081'''
        )
    }
}