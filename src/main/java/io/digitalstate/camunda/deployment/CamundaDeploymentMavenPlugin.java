package io.digitalstate.camunda.deployment;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Goal which deploys Camunda deployment files through Camunda Rest API
 * @author Stephen Russett Github: @StephenOTT
 **/
@Mojo( name = "deploy-with-rest-api")
public class CamundaDeploymentMavenPlugin extends AbstractMojo
{
    @Parameter(defaultValue = "http://localhost:8080" )
    private String host;

    @Parameter(defaultValue = "/engine-rest" )
    private String apiPath;

    @Parameter(defaultValue = "${project.build.directory}/camunda-deployment-files-from-source" )
    private String deploymentFilesDir;

    @Parameter
    private String scriptPath;

    @Parameter
    private Map additionalConfigs;

    public void execute() throws MojoExecutionException
    {
        Map<String, Object> configs = new HashMap<String, Object>();
        configs.put("host", host);
        configs.put("apiPath", apiPath);
        configs.put("deploymentFilesDir", deploymentFilesDir);
        configs.put("scriptPath", scriptPath);
        configs.put("additionalConfigs", additionalConfigs);

        getLog().info( "Starting deployment to Camunda..." );
        URI file;
        // If not custom script path was provided, then use the build in default:
        if (scriptPath==null){
            try {
                file = getClass().getResource("CamundaDeployment.groovy").toURI();

            }catch (Exception e){
                throw new MojoExecutionException("Something big went wrong...Cannot find CamundaDeployment.groovy: " + e.getLocalizedMessage());
            }
            // If a Custom script path was provided then use it:
        } else {
            try {
                file =  new File(scriptPath).toURI();

            } catch (Exception e){
                throw new MojoExecutionException("Error with provided script path: " + scriptPath + "\n" + e.getLocalizedMessage());
            }
        }
        // Eval the groovy script
        Binding binding = new Binding();
        binding.setProperty("configs", configs);
        GroovyShell shell = new GroovyShell(binding);

        try {
            getLog().info("Deployment Destination: " + configs.get("host") + configs.get("apiPath"));
            getLog().info("Deployment Files: " + configs.get("deploymentFilesDir"));

            Object value = shell.evaluate(file);
            getLog().info(value.toString());

        } catch (Exception e){
            throw new MojoExecutionException("An error occurred during deployment to Camunda.\n" + e.getLocalizedMessage());
        }
    }
}