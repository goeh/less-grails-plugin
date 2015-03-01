import org.grails.plugin.resource.mapper.MapperPhase

/**
 * Less resource mapping. Compile .less files into .css files.
 *
 * @author Karol Balejko
 */
class LessResourceMapper {

    static phase = MapperPhase.GENERATION
    static defaultIncludes = [ '**/*.less' ]

    def grailsApplication
    def lessCompilerService

    def paths = [].asSynchronized()

    def map(resource, config){
        File lessFile = resource.processedFile
        File cssFile = new File(lessFile.absolutePath + '.css')

        if(paths.isEmpty()) {
            addDefaultPath(grailsApplication.config.grails.resources.less.default.importPath ?: ['less'])
        }
        def importPath = grailsApplication.mainContext.getResource(resource.originalUrl)?.file?.parentFile?.absolutePath
        if (importPath) {
            if(! paths.find{it.path == importPath}) {
                def order = resource.tagAttributes.order ?: 10
                log.debug "Adding import path [${importPath}][order: ${order}] for resource [${resource}]"
                paths << [path:importPath, order:order]
                paths.sort {it.order}
            }
        }

        try {
            log.debug "Compiling LESS file [${lessFile}] into [${cssFile}]"
            lessCompilerService.compile (lessFile, cssFile, paths*.path)
            resource.processedFile = cssFile
            resource.contentType = 'text/css'
            resource.tagAttributes.rel = 'stylesheet'
            resource.updateActualUrlFromProcessedFile()
        } catch (Exception e) {
            log.error("Error compiling less file: ${lessFile}", e)
        }
    }

    private void addDefaultPath(defaultPath) {
        def applicationContext = grailsApplication.mainContext

        if(!(defaultPath instanceof Collection)) {
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
            } else {
                log.error "Default import path not found: $path"
            }
        }
        paths.sort {it.order}
    }
}