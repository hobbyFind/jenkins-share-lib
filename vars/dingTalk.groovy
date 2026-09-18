def call(Map config = [:]) {
    // 1. 基础参数解析
    def projectName   = config.projectName ?: env.JOB_NAME ?: 'Unknown'
    def buildUser     = config.buildUser ?: env.BUILD_USER ?: '系统触发'
    def WEBHOOK_URL = config.WEBHOOK_URL ?: '123456'
    def buildStatus   = config.buildStatus ?: currentBuild.currentResult
    def duration      = config.duration ?: currentBuild.durationString?.replace(' and counting', '') ?: '未知'
    
    // 服务类型，默认为 backend
    def projectType   = config.projectType ?: 'backend' 
    // 自定义扩展字段 (Map类型)
    def extraInfo     = config.extraInfo ?: [:] 
	def footerTip = config.footerTip ?: ""
	// @人 配置项解析
    def atMobiles     = config.atMobiles ?: []      // 手机号列表，如 ['13800138000']
    def isAtAll       = config.isAtAll ?: false     // 是否 @所有人
	
    // 状态映射（状态 + 颜色）
    def statusMap = [
        'SUCCESS':  ['text': '成功', 'color': '#33FF00'],
        'FAILURE':  ['text': '失败', 'color': '#FF0000'],
        'UNSTABLE': ['text': '不稳定', 'color': '#FFA500'],
        'ABORTED':  ['text': '已取消', 'color': '#999999']
    ]
    def statusInfo = statusMap[buildStatus] ?: ['text': buildStatus, 'color': '#3399FF']
	
	
	// 时间处理
    def now = new Date()
    def endTime = now.format("yyyy-MM-dd HH:mm:ss")
    def startTime = env.BUILD_TIMESTAMP ?: endTime

	// 动态构建消息字段 (核心优化点)
    LinkedHashMap<String, String> fields = new LinkedHashMap<>()
    fields.put("状态", "<font color=\"${statusInfo.color}\">${statusInfo.text}</font>")
    fields.put("执行人", buildUser)
    fields.put("构建耗时", duration)
	
    switch (projectType.toLowerCase()) {
        case 'kubernetes':
            fields.put("命名空间", config.namespace ?: env.NAMESPACE ?: '-')
            fields.put("服务名称", config.serviceName ?: env.SERVICE_NAME ?: '-')
            fields.put("镜像标签", config.imageTag ?: env.IMAGETAG ?: '-')
            break
        case 'web':
            fields.put("部署环境", config.envName ?: env.DEPLOY_ENV ?: '-')
            fields.put("访问域名", config.domain ?: '-')
            fields.put("Git分支", env.GIT_BRANCH ?: env.BRANCH_NAME ?: '-')
            break
        default:
            break
    }
	
	if (extraInfo instanceof Map) {
        extraInfo.each { k, v -> fields.put(k.toString(), v.toString()) }
    }
	
	def markdownBody = fields.collect { k, v -> "> **${k}**: ${v}\n" }.join("\n")
	
	
	def atText = ""
    if (isAtAll) {
        atText = "@所有人"
    } else if (atMobiles instanceof List && !atMobiles.isEmpty()) {
        atText = atMobiles.collect { "@${it}" }.join(" ")
    }
	
	def markdownText = """### <font face='楷体' color='#3399FF'>${projectName}</font>\n
-------------------------------\n
${markdownBody}
-------------------------------\n
###### ${footerTip}${atText}"""

	def payload = JsonOutput.toJson([
        msgtype: 'markdown',
        markdown: [
            title: "Jenkins通知",
            text : markdownText
        ],
        at: [
			atMobiles: atMobiles ?: [],
			isAtAll: isAtAll
		]
    ])
    
    // 打印消息内容，方便在 Jenkins 控制台排错
    println("[Dingtalk] 准备发送通知:\n${markdownText}")

    // 发送 HTTP POST 请求给钉钉机器人
    try {
		def url = new URL(WEBHOOK_URL)
		httpRequest(
			url: WEBHOOK_URL,
			httpMode: 'POST',
			contentType: 'APPLICATION_JSON_UTF8',
			requestBody: payload,
			validResponseCodes: '200',
			quiet: false
		)	
		println("[Dingtalk] 通知发送成功")
    } catch (Exception e) {
        println("[Dingtalk]发送钉钉通知失败: ${e.message}")
    }
}
