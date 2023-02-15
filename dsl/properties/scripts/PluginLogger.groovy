import groovy.json.JsonOutput

class PluginLogger {
    def level = 1
    static int INFO = 1
    static int DEBUG = 2
    static int TRACE = 3
    static int ALWAYS = 0

    def info(Object... objects) {
        logger(INFO, objects)
    }

    def debug(Object... objects) {
        logger(DEBUG, '[DEBUG] ', objects)
    }

    def trace(Object... objects) {
        logger(TRACE, '[TRACE] ', objects)
    }

    def error(Object... objects) {
        logger(ALWAYS, "[ERROR] ", objects)
    }

    def warning(Object... objects) {
        logger(ALWAYS, '[WARNING] ', objects)
    }

    def printStackTrace( Throwable e ) {
        if (DEBUG <= level) {
            e.printStackTrace()
        }
    }

    def logger(def currentLevel, Object ... objects) {
        if ( currentLevel <= level || currentLevel == ALWAYS ) {
            objects.each { o ->
                if (o instanceof String || o instanceof GString) {
                    print o
                }
                else {
                    print JsonOutput.prettyPrint(JsonOutput.toJson(o))
                }
            }
            println ''
        }
    }
}
