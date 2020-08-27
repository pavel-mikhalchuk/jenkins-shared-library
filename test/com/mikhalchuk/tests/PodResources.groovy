package com.mikhalchuk.tests

class PodResources {

    static def PRICING_DEV_DEV =
            '''resources:
  resources:
    requests:
      memory: "1500Mi"
      cpu: "100m"
    limits:
      memory: "5Gi"
      cpu: "2"
javaOpts: "-Xms500m -Xmx4g -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.port=9011 -Dcom.sun.management.jmxremote.rmi.port=9011 -Djava.rmi.server.hostname=127.0.0.1"'''

}
