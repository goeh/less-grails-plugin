package com.groovydev

import grails.test.spock.IntegrationSpec
import spock.lang.Unroll

class LessCompilerServiceSpec extends IntegrationSpec {

    def lessCompilerService

    @Unroll
    def "compile #srcLessFile into CSS"() {
        when:
        def tmpTarget = File.createTempFile("LessCompilerServiceSpec", ".css")
        tmpTarget.deleteOnExit()
        def srcFile = new File("test/integration", srcLessFile)
        def modelFile = new File("test/integration", modelCssFile)
        def importPath = srcFile?.parentFile?.absolutePath
        lessCompilerService.compile(srcFile, tmpTarget, [importPath])

        then:
        tmpTarget.text.equals(modelFile.text)

        where:
        srcLessFile                           | modelCssFile
        "css-only.less"                       | "css-only.css"
        "bootstrap-2.2.2/less/bootstrap.less" | "bootstrap-2.2.2/css/bootstrap.css"
        "bootstrap-2.3.0/less/bootstrap.less" | "bootstrap-2.3.0/css/bootstrap.css"
    }
}
