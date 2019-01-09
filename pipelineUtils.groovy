def withRetry(iterations, sleepTime, Closure closure) {
    for (i = 0; i < iterations; i++) {
        try {
            println "Try number ${i}"
            closure()
            return
        } catch (Exception e) {
            sleep sleepTime
            println "Retrying..."
        }
    }

    error("Exceeded number of retries")

}

def restGetJson(url, credentialId) {
    res = httpRequest url: url, contentType: "APPLICATION_JSON", authentication: credentialId
    res.getStatus()
    res.getContent()
}

return this