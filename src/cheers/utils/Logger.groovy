package cheers.utils

class Logger implements Serializable {
    static def info(def msg) {
        println("[INFO] ${msg}")
    }

    static def error(def msg) {
        println("[ERROR] ${msg}")
        throw new AbortException(msg)
    }

    static def warn(def msg) {
        println("[WARN] ${msg}")
    }

    static def debug(def msg) {
        println("[DEBUG] ${msg}")
    }
}