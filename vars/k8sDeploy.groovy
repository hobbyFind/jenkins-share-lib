import cheers.utils.Logger

def call(Map config) {
    def ns = config.namespace ?: 'default'
    def deployName = config.deploymentName
    def image = config.image
    
    loggerUtils.info("准备部署到 K8s: Namespace=${ns}, Deployment=${deployName}")
    
    // 使用 withKubeConfig 插件配置上下文，或者直接使用 kubectl set image
    withKubeConfig([credentialsId: config.kubeConfigId, serverUrl: config.serverUrl]) {
        try {
            sh "kubectl set image deployment/${deployName} ${deployName}=${image} -n ${ns}"
            
            def timeoutMin = config.timeout ?: 5
            sh "kubectl rollout status deployment/${deployName} -n ${ns} --timeout=${timeoutMin}m"
            
            loggerUtils.success("应用 ${deployName} 部署成功！")
        } catch (Exception e) {
            loggerUtils.error("K8s 部署失败，正在回滚...")
            // 部署失败自动回滚
            sh "kubectl rollout undo deployment/${deployName} -n ${ns}"
            throw e
        }
    }
}