package com.mikhalchuk.tests

import com.mikhalchuk.Yaml
import spock.lang.Specification

import static org.assertj.core.api.Assertions.assertThat

class YamlTest extends Specification {

    def "test parse"() {
        given:
        def yaml = '''resources:
  resources:
    requests: 
      memory: "1500Mi"
      cpu: "100m"
    limits:
      memory: "5Gi"
      cpu: "2"
javaOpts: "-Xms500m -Xmx4g"'''

        when:
        def map = Yaml.parse(yaml)

        then:
        def expected = [
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
        assertThat(map).isEqualTo(
            expected
        )
    }

    def "test write"() {
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
}