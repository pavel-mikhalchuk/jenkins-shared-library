import com.mikhalchuk.*

def call(body) {
    def ctx = ObjUtils.closureToMap(body)

    if (!ctx.nodeModulesCachePath) {
        ctx.nodeModulesCachePath = "/root/node_modules/${JOB_NAME}"
    }

    sh "rm -rf ./node_modules && (cp -r ${ctx.nodeModulesCachePath}/node_modules ./node_modules || true)"

    sh "npm install --loglevel=verbose"

    sh "rm -rf ${ctx.nodeModulesCachePath} && mkdir -p ${ctx.nodeModulesCachePath} && cp -r ./node_modules ${ctx.nodeModulesCachePath}"
}