package com.mikhalchuk.tests

class PodResources {

    static def PRICING_DEV =
            '''resources:
  resources:
    requests:
      memory: "1500Mi"
      cpu: "100m"
    limits:
      memory: "5Gi"
      cpu: "2"
javaOpts: "-Xms500m -Xmx4g -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.port=9011 -Dcom.sun.management.jmxremote.rmi.port=9011 -Djava.rmi.server.hostname=127.0.0.1"'''

    static def PRICING_PROD =
            '''resources:
  resources:
    requests:
      memory: "1500Mi"
      cpu: "100m"
    limits:
      memory: "5Gi"
      cpu: "2"
javaOpts: "-Xms500m -Xmx4g -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.port=9011 -Dcom.sun.management.jmxremote.rmi.port=9011 -Djava.rmi.server.hostname=127.0.0.1"'''

    static def CALCULATION_DEV =
            '''resources:
  resources:
    requests:
      memory: "500Mi"
      cpu: "100m"
    limits:
      memory: "2Gi"
      cpu: "2"
javaOpts: "-Xmx2g -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.port=9012 -Dcom.sun.management.jmxremote.rmi.port=9012 -Djava.rmi.server.hostname=127.0.0.1"'''

    static def CALCULATION_PROD =
            '''resources:
  resources:
    requests:
      memory: "1Gi"
      cpu: "1"
    limits:
      memory: "1Gi"
      cpu: "1"
javaOpts: "-Xms1024m -Xmx1024m -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.port=9012 -Dcom.sun.management.jmxremote.rmi.port=9012 -Djava.rmi.server.hostname=127.0.0.1"'''

    static def ASERVICE_DEV =
            '''resources:
  resources:
    requests:
      memory: "4Gi"
      cpu: "100m"
    limits:
      memory: "5Gi"
      cpu: "2"
javaOpts: "-Xms4g -Xmx4g -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.rmi.port=9010 -Djava.rmi.server.hostname=127.0.0.1 -server -XX:MaxMetaspaceSize=1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:ParallelGCThreads=20 -XX:ConcGCThreads=5 -XX:InitiatingHeapOccupancyPercent=70 -Djdk.nio.maxCachedBufferSize=262144 -XX:HeapDumpPath=/aservice-images/oom-dumps/"'''

    static def ASERVICE_DEV_JAVA_OPTS_NULL =
            '''resources:
  resources:
    requests:
      memory: "4Gi"
      cpu: "100m"
    limits:
      memory: "5Gi"
      cpu: "2"
javaOpts: null'''

}
