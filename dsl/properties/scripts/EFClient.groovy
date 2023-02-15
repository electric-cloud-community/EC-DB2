@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7.1' )

import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

import static groovyx.net.http.ContentType.URLENC
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import static groovyx.net.http.Method.PUT
import static groovyx.net.http.Method.DELETE


public class EFClient extends BaseClient {

    def getServerUrl() {
        def commanderServer = System.getenv('COMMANDER_SERVER')
        def commanderPort = System.getenv("COMMANDER_HTTPS_PORT")
        def secure = Integer.getInteger("COMMANDER_SECURE", 1).intValue()
        def protocol = secure ? "https" : "http"

        return "$protocol://$commanderServer:$commanderPort"
    }

    // Shared uri prefix for all API calls
    private String uriPrefix = "/rest/v1.0/"

    public static def splitCommaSeparatedList( String list ) {
        if ( !list ) {
            return null
        }
        return list.split(/,\s/)
    }

    Object doHttpGet(String requestUri, boolean failOnErrorCode = true, def query = null) {
        def sessionId = System.getenv('COMMANDER_SESSIONID')
        doHttpRequest(GET, getServerUrl(), uriPrefix + requestUri, ['Cookie': "sessionId=$sessionId"],
                failOnErrorCode, /*requestBody*/ null, query)
    }

    Object doHttpPost(String requestUri, Object requestBody, boolean failOnErrorCode = true, def query = null) {
        def sessionId = System.getenv('COMMANDER_SESSIONID')
        doHttpRequest(POST, getServerUrl(), uriPrefix + requestUri, ['Cookie': "sessionId=$sessionId"], failOnErrorCode, requestBody, query)
    }

    Object doHttpPut(String requestUri, Object requestBody, boolean failOnErrorCode = true, def query = null) {
        def sessionId = System.getenv('COMMANDER_SESSIONID')
        doHttpRequest(PUT, getServerUrl(), uriPrefix + requestUri, ['Cookie': "sessionId=$sessionId"], failOnErrorCode, requestBody, query)
    }

    Object doHttpDelete(String requestUri, boolean failOnErrorCode = true, def query = null) {
        def sessionId = System.getenv('COMMANDER_SESSIONID')
        doHttpRequest(DELETE, getServerUrl(), uriPrefix + requestUri, ['Cookie': "sessionId=$sessionId"], failOnErrorCode, null, query)
    }

    // TODO: remove jobStepID
    @Deprecated
    def setProperty( String jobStepId, String propertyName, String value) {
        def query = [
            propertyName: propertyName,
            value: value,
            jobStepId: jobStepId
        ]
        doHttpPost("properties", /* request body */ null, /* fail on error*/ true, query)
    }

    def setProperty(String propertyName, String  value, Boolean withJobStepId = true, Boolean failOnError = true) {
        def query = [:]
        if (withJobStepId) {
            def jobStepId = System.getenv('COMMANDER_JOBSTEPID')
            if (jobStepId) {
                query.jobStepId = jobStepId
            }
        }
        def body = groovy.json.JsonOutput.toJson([value: value])
        doHttpPut("properties/${propertyName}", body, failOnError, query)
    }

    def createProperty(String propertyName, String value, Boolean withJobStepId = true, Boolean failOnError = true) {
        def query = [
            propertyName: propertyName
        ]
        if (withJobStepId) {
            def jobStepId = System.getenv('COMMANDER_JOBSTEPID')
            if (jobStepId) {
                query.jobStepId = jobStepId
            }
        }
        def body = groovy.json.JsonOutput.toJson([value: value])
        doHttpPost('properties', body, failOnError, query)
    }

    def getProperty(String propertyName, Boolean withJobStepId = true, Boolean failOnError = true) {
        def query = [:]
        if (withJobStepId) {
            def jobStepId = System.getenv('COMMANDER_JOBSTEPID')
            if (jobStepId) {
                query.jobStepId = jobStepId
            }
        }
        def answer = doHttpGet("properties/${propertyName}", failOnError, query)
        answer.data?.property?.value
    }

    def getParameter(String parameterName) {
        def answer = getProperty(parameterName, true, true)
        answer
    }

    def deleteProperty(String propertyName, Boolean withJobStepId = true, Boolean failOnError = true) {
        def query = [:]
        if (withJobStepId) {
            def jobStepId = System.getenv('COMMANDER_JOBSTEPID')
            if (jobStepId) {
                query.jobStepId = jobStepId
            }
        }
        doHttpDelete("properties/${propertyName}", failOnError, query)
    }

    def getConfigValues(def configPropertySheet, def config, def pluginProjectName) {

        // Get configs property sheet
        def result = doHttpGet("projects/$pluginProjectName/$configPropertySheet", false)

        def configPropSheetId = result.data?.property?.propertySheetId
        if (!configPropSheetId) {
            throw new RuntimeException("No plugin configurations exist!")
        }

        result = doHttpGet("propertySheets/$configPropSheetId", false)
        // Get the property sheet id of the config from the result
        def configProp = result.data.propertySheet.property.find{
            it.propertyName == config
        }

        if (!configProp) {
            throw new RuntimeException("Configuration $config does not exist!")
        }

        result = doHttpGet("propertySheets/$configProp.propertySheetId")

        def values = result.data.propertySheet.property.collectEntries{
            [(it.propertyName): it.value]
        }

        logger(1, "Config values: " + values)

        def cred = getCredentials(config)
        values << [credential: [userName: cred.userName, password: cred.password]]

        logger(1, "After Config values: " + values ) // TODO DANGER!! CREDENTIALS!!!

        if ( values.debugLevel ) {
            values.debugLevel = values.debugLevel as int
        }
        else {
            values.debugLevel = 1
        }

        values
    }


    def getCredentials(def credentialName) {
        def jobStepId = System.getenv('COMMANDER_JOBSTEPID')
        def result = doHttpGet("jobsSteps/$jobStepId/credentials/$credentialName")
        result.data.credential
    }

    def evalDsl(String dsl, Map query = [:], Boolean withJobStepId = false) {
        def body = JsonOutput.toJson([dsl: dsl])
        query.request = 'evalDsl'
        if (withJobStepId) {
            def jobStepId = System.getenv('COMMANDER_JOBSTEPID')
            query.jobStepId = jobStepId
        }
        def result = doHttpPost('server/dsl', body, true, query)
        result.data.value
    }

    def handleError (String msg) {
        println "ERROR: $msg"
        throw new RuntimeException(msg)
    }

}
