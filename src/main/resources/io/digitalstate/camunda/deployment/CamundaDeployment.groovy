package io.digitalstate.camunda.deployment

@Grab(group='org.jsoup', module='jsoup', version='1.11.3')

import groovy.io.FileType
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.jsoup.Connection
import org.jsoup.Jsoup
import static org.jsoup.Connection.Method.POST

//Script to execute
Map<String, Object> config = (Map<String, Object>) configs
String apiUrl = "${config.host}${config.apiPath}"
String deploymentFiles = config.deploymentFilesDir
// If additionalConfigs are not used then a empty HashMap is generated.
Map<String, String> additionalConfigs = (Map<String, String>) config.additionalConfigs ?: new HashMap<String, String>()

deployToUrl(apiUrl, deploymentFiles, additionalConfigs)


// Helper Methods
static Connection.Response deploy(String apiUrl,
                                  String deploymentFileDir,
                                  Map<String, String> deploymentConfigs)
{

    Connection deploymentBuild = Jsoup.connect("${apiUrl}/deployment/create")
            .method(POST)
            .headers([
                'accept': 'application/json'
                ])
            .timeout(30000)
            .ignoreContentType(true)
            .ignoreHttpErrors(true)

    // Get each file in the deployment folder
    File dir = new File(deploymentFileDir)
    dir.eachFileRecurse (FileType.FILES) { file ->
        deploymentBuild.data(file.getName(), file.getName(), file.newInputStream())
    }

    deploymentBuild.data('deployment-name', deploymentConfigs.get('deploymentName', 'maven-deployment'))
    deploymentBuild.data('enable-duplicate-filtering', deploymentConfigs.get('duplicateFiltering', 'false'))
    deploymentBuild.data('deploy-changed-only', deploymentConfigs.get('deployChangedOnly', 'false'))
    deploymentBuild.data('deployment-source', deploymentConfigs.get('deploymentSource', 'maven'))

    if (deploymentConfigs.tenantId){
        deploymentBuild.data('tenant-id', (String)deploymentConfigs.tenantId)
    }

    // execute the POST and return the response
    Connection.Response deploymentResponse = deploymentBuild.execute()
    return deploymentResponse
}

static String deployToUrl(String apiUrl,
                          String deploymentFileDir,
                          Map<String, String> additionalConfigs)
{
    Connection.Response deploymentResponse = deploy(apiUrl, deploymentFileDir, additionalConfigs)

    if (deploymentResponse.statusCode() == 200){
        try {
            InputStream body = deploymentResponse.bodyStream()
            def json = new JsonSlurper().parse(body)
            String prettyJson = new JsonBuilder(json).toPrettyString()

            return "Deployment Successful: \n${prettyJson}"

        } catch (all){
            throw new Exception("Could not parse the response from Camunda: \n${all.getLocalizedMessage()}")
        }
    } else {
        throw new Exception("Deployment Failed. \nStatus Code: ${deploymentResponse.statusCode()}. \nResponse from Camunda: ${deploymentResponse.body()}")
    }
}