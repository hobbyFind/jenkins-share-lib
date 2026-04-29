def call(Map config = [:]) {
    def projectName = config.projectName ?: env.JOB_NAME
    def buildUser = config.buildUser ?: env.BUILD_USER ?: '未知用户'
    def imageTag = config.imageTag ?: env.IMAGETAG ?: '无'
	def namespace = config.namespace ?: env.NAMESPACE ?: '无'
	def serviceName = config.serviceName ?: 'env.SERVICE_NAME' ?: '无'
    def webhookUrl = config.webhookUrl ?: env.WEBHOOK // 优先使用传入的 webhook，其次使用环境变量
    def buildStatus = config.buildStatus ?: currentBuild.currentResult
    // 处理时间格式
    def endTime = new Date().format("yyyy-MM-dd HH:mm:ss")
    def startTime = env.BUILD_TIMESTAMP ? env.BUILD_TIMESTAMP.split(" ")[0..1].join(" ") : "未知时间"
	// 定义颜色
	def statusColor = (buildStatus == 'SUCCESS') ? '#33FF00' : '#FF0000'
    def statusText = (buildStatus == 'SUCCESS') ? '成功' : '失败'
    // 准备钉钉 Markdown 消息体
    def message = """
    {
        "msgtype": "markdown",
        "markdown": {
            "title": "jenkins通知",
            "text": "### <font face='楷体' color='#3399FF'> ${projectName}</font>
> -------------------------------
> - 状态: <font color='{statusColor}'>${statusText}</font>
> - 执行人: ${buildUser}
> - 开始时间: ${startTime}
> - 命名空间: ${namespace}
> - 服务名称: ${serviceName}
> - 镜像标签: ${imageTag}
> - 结束时间: ${endTime}
> -------------------------------
> 服务版本更新成功，请关注服务运行状态"
        },
        "at": {
            "atMobiles": [],
            "atUserIds": [],
            "isAtAll": false
        }
    }"""

    // 打印消息内容，方便在 Jenkins 控制台排错
    println("准备发送钉钉通知：\n${message}")

    // 4. 发送 HTTP POST 请求给钉钉机器人
    try {
        def url = new URL(webhookUrl)
        def connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        connection.doOutput = true

        def writer = new OutputStreamWriter(connection.outputStream, "UTF-8")
        writer.write(message)
        writer.flush()
        writer.close()

        println("钉钉响应码: ${connection.responseCode}")
        println("钉钉响应信息: ${connection.responseMessage}")
    } catch (Exception e) {
        println("发送钉钉通知失败: ${e.message}")
    }
}