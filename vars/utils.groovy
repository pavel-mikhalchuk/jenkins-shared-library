String timestamped(string) {
    sh(script: "echo ${string}_\$(date +\"%d-%m-%Y_%H-%M-%S\")", returnStdout: true).trim()
}