import org.grails.plugin.resource.mapper.MapperPhase
import org.codehaus.groovy.grails.commons.GrailsApplication
import java.util.concurrent.ConcurrentHashMap

/**
 * Less resource mapping. Compile .less files into .css files.
 *
 * @author Karol Balejko
 */
class LessResourceMapper {

    static phase = MapperPhase.GENERATION
    static defaultIncludes = [ '**/*.less' ]

    GrailsApplication grailsApplication
    def lessCompilerService

    def paths = [].asSynchronized()

    def map(resource, config){
        File lessFile = resource.processedFile
        File cssFile = new File(lessFile.absolutePath + '.css')

        if(paths.isEmpty()) {
            def defaultPath = grailsApplication.config.grails.resources.less.default.importPath
            if(defaultPath) {
                addDefaultPath(defaultPath)
            }
        }

        def importPath = grailsApplication.parentContext.getResource(resource.originalUrl)?.file?.parentFile?.absolutePath
        if (importPath) {
            def order = resource.tagAttributes.order ?: 10
            if(! paths.find{it.path == importPath}) {
                log.debug "Adding import path [${importPath}][order: ${order}] for resource [${resource}]"
                paths << [path:importPath, order:order]
                paths.sort {it.order}
            }
        }
        
        try {
            log.debug "Compiling LESS file [${lessFile}] into [${cssFile}]"
            lessCompilerService.compile (lessFile, cssFile, paths.collect {it.path})
            resource.processedFile = cssFile
            resource.contentType = 'text/css'
            resource.tagAttributes.rel = 'stylesheet'
            resource.updateActualUrlFromProcessedFile()
        } catch (Exception e) {
            log.error("Error compiling less file: ${lessFile}", e)
        }
    }

    void addDefaultPath(defaultPath) {
        def applicationContext = grailsApplication.parentContext

        if(!(defaultPath instanceof List)) {
            defaultPath = [defaultPath]
        }
        int order = 1
        for(path in defaultPath) {
            def importPath = applicationContext.getResource(path)?.file?.absolutePath
            if (importPath) {
                log.debug "Adding default import path [${importPath}][order: ${order}]"
                if(! paths.find{it.path == importPath}) {
                    paths << [path: importPath, order: order++]
                }
            }
        }
        paths.sort {it.order}
    }
}
