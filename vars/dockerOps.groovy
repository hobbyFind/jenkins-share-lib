import cheers.utils.Logger


def login(Map config) {
    def registryUrl = config.registry ?: 'registry.example.com'
    def credentialsId = config.credentialsId
    
    Logger.info("正在登录镜像仓库: ${registryUrl}")
    withCredentials([usernamePassword(credentialsId: credentialsId, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
        sh "echo ${DOCKER_PASS} | docker login ${registryUrl} -u ${DOCKER_USER} --password-stdin"
    }
}

def buildAndPush(Map config) {
    def image = "${config.registry}/${config.project}/${config.appName}:${config.tag}"
    def dockerfile = config.dockerfilePath ?: 'Dockerfile'
    def MAX_RETRIES = 3

    Logger.info("开始构建镜像: ${image}")

    def buildStatus = sh(script: "docker build -t ${image} -f ${dockerfile} . --no-cache", returnStatus: true)
    if (buildStatus != 0) {
        Logger.error("镜像${image}构建失败")
    }
    for (int i = 1; i <= MAX_RETRIES; i++) {
        Logger.info("正在推送镜像${image} (第 ${i} 次)")
        def pushStatus = sh(script: "docker push ${image}", returnStatus: true)

        if (pushStatus == 0) {
            Logger.info("镜像${image}推送成功")
            break
        }
        
        if (i == MAX_RETRIES) {
            sh "docker compose -f ${composeFile} down --rmi local ${services}"
            Logger.ERROR("镜像推送在尝试 ${MAX_RETRIES} 次后仍然失败，删除镜像并直接退出 !!!!!!!!!!")
        }
        
        Logger.warn("镜像推送失败，5秒后重试...")
        sleep(5)
    }
    return image
}


def dockerBuildPushWithCompose(Map config) {
    def MAX_RETRIES = 3
    def composeFile = config.composeFile ?: 'Dockerfile'
    def services = config.services ?: ''    //要构建的服务，按空格分隔，传参示例：  "mysql nginx redis"
    // 构建镜像
    Logger.info("正在构建镜像: ${services}")
    def buildStatus = sh(script: "sudo docker compose -f ${composeFile} build --no-cache ${services}", returnStatus: true)
    if (buildStatus != 0) {
        Logger.error("服务 ${services} 镜像构建失败")
    }
    
    // 推送镜像
    for (int i = 1; i <= MAX_RETRIES; i++) {
        Logger.info("正在推送镜像 (第 ${i} 次)")
        def pushStatus = sh(script: "sudo docker compose -f ${composeFile} push ${services}", returnStatus: true)
        
        if (pushStatus == 0) {
            Logger.info("服务 ${services} 镜像推送成功！")
            break
        }
        
        if (i == MAX_RETRIES) {
            sh "sudo docker compose -f ${composeFile} down --rmi local ${services}"
            Logger.error("镜像推送在尝试 ${MAX_RETRIES} 次后仍然失败，删除镜像并直接退出")
        }
        
        Logger.warn("镜像推送失败，5秒后重试...")
        sleep(5)
    }
    
    sh "sudo docker compose -f ${composeFile} down --rmi local ${services}"
}