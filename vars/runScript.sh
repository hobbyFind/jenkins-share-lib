def call(Map config) {
    def scriptName = config.name
    def args = config.args ?: ''
    
    def scriptContent = libraryResource "scripts/${scriptName}"

    writeFile file: scriptName, text: scriptContent
    
    sh "chmod +x ${scriptName}"
    sh "./${scriptName} ${args}"
}