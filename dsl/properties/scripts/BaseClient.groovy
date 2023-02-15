@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7.1' )

import groovy.json.JsonBuilder
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method


import static groovyx.net.http.ContentType.JSON


public class BaseClient {

    def logLevel = 2

    Object doHttpRequest(Method method, String requestUrl,
                         String requestUri, def requestHeaders,
                         Boolean failOnErrorCode = true,
                         Object requestBody = null,
                         def queryArgs = null) {

        logger(1, "requestUrl: $requestUrl")
        logger(1, "URI: $requestUri")
        logger(1, "QUery: $queryArgs")
        if (requestBody) logger(1, "Payload: $requestBody")

        def http = new HTTPBuilder(requestUrl)
        http.ignoreSSLIssues()

        http.request(method, JSON) {
            uri.path = requestUri
            headers = requestHeaders
            body = requestBody
            uri.query = queryArgs

            response.success = { resp, json ->
                logger(1, "request was successful $resp.statusLine.statusCode $json")
                [statusLine: resp.statusLine,
                 data      : json]
            }

            response.failure = { resp, reader ->
                if ( failOnErrorCode ) {
                    handleError("Request failed with $resp.statusLine")
                }
                [statusLine: resp.statusLine]
            }
        }
    }

    def logger (int level, def message) {
        if ( level >= this.logLevel ) {
            println message
        }
    }
}
