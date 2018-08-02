package io.digitalstate.camunda.deployment

@Grab(group='org.jsoup', module='jsoup', version='1.11.3')

import groovy.io.FileType
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.jsoup.Connection
import org.jsoup.Jsoup
import static org.jsoup.Connection.Method.POST

//Script to execute
Map<String, Object> config = (Map<String, Object>)configs
String apiUrl = "${config.host}${config.apiPath}"
String deploymentFiles = config.deploymentFilesDir

deployToUrl(apiUrl, deploymentFiles)


// Helper Methods
static Connection.Response deploy(String apiUrl, String deploymentFileDir){

    Connection deploymentBuild = Jsoup.connect("${apiUrl}/deployment/create")
            .method(POST)
            .headers([
            'accept': 'application/json',
//                'content-type': 'multipart/form-data'
    ])
            .timeout(30000)
            .ignoreContentType(true)
            .ignoreHttpErrors(true)

    // Get each file in the deployment folder
    File dir = new File(deploymentFileDir)
    dir.eachFileRecurse (FileType.FILES) { file ->
        deploymentBuild.data(file.getName(), file.getName(), file.newInputStream())
    }
    deploymentBuild.data('deployment-name', 'myDeployment')
    deploymentBuild.data('enable-duplicate-filtering', 'false')
    deploymentBuild.data('deploy-changed-only', 'false')
    // execute the POST and return the response
    Connection.Response deploymentResponse = deploymentBuild.execute()
    return deploymentResponse
}

static String deployToUrl(String apiUrl, String deploymentFileDir){
    Connection.Response deploymentResponse = deploy(apiUrl, deploymentFileDir)
    if (deploymentResponse.statusCode() == 200){
        try {
            InputStream body = deploymentResponse.bodyStream()
            def json = new JsonSlurper().parse(body)
            String prettyJson = new JsonBuilder(json).toPrettyString()

            return "Deployment Successful: \n${prettyJson}"

        } catch (all){
            throw new Exception("Could not parse the response from Camunda: \n${all}")
        }
    } else {
        throw new Exception("Deployment Failed. \nStatus Code: ${deploymentResponse.statusCode()}. \nResponse from Camunda: ${deploymentResponse.body()}")
    }
}