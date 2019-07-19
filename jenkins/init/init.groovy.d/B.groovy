import hudson.plugins.git.*
import hudson.slaves.EnvironmentVariablesNodeProperty
import jenkins.model.Jenkins
import hudson.model.*
import hudson.security.*
def strategy = new GlobalMatrixAuthorizationStrategy()
instance = Jenkins.getInstance()
globalNodeProperties = instance.getGlobalNodeProperties()
envVarsNodePropertyList = globalNodeProperties.getAll(EnvironmentVariablesNodeProperty.class)

newEnvVarsNodeProperty = null
envVars = null

if ( envVarsNodePropertyList == null || envVarsNodePropertyList.size() == 0 ) {
  newEnvVarsNodeProperty = new EnvironmentVariablesNodeProperty();
  globalNodeProperties.add(newEnvVarsNodeProperty)
  envVars = newEnvVarsNodeProperty.getEnvVars()
} else {
  envVars = envVarsNodePropertyList.get(0).getEnvVars()
}

envVars.put("DIST_TAG", "dist-centos-updates")

instance.save()
def pipelineScriptHead = '''
pipeline {
   agent any
   stages {
        stage('Initialize') {
            steps {
                //enable remote triggers
                script {
                    properties([pipelineTriggers([pollSCM('H/5 * * * *')])])
                }
                //define scm connection for polling
'''
def pipelineScriptTail = """
            }
                 }
         stage ('Build') {
               steps {
                 sh '''
                    if [ "\$BUILD_NUMBER" -eq "1" ] ; then 
                          export KOJI_BUILD="echo"
                    else 
                          export KOJI_BUILD="koji build"
                    fi
                    \$KOJI_BUILD \$DIST_TAG \$(git config remote.origin.url)#\$(git rev-parse HEAD)
                '''
               }
         }
    }
}
"""


String fileContents = new File('/var/jenkins_home/apps').getText('UTF-8')
def branch = "c7"
def urlBase = "git://gitcentos.mvista.com/centos/upstream/packages"
for (String item: fileContents.split()) {
   if ( !Jenkins.instance.getItemByFullName(item) ) {
	   def pipelineScript = pipelineScriptHead +
	     "                git branch: '" + branch +"',  url: '" + urlBase + "/" + item + "'" +
	     pipelineScriptTail

	   def flowDefinition = new org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition(pipelineScript, true)

	   def parent = Jenkins.instance
	   def job = new org.jenkinsci.plugins.workflow.job.WorkflowJob(parent, item)
	   job.definition = flowDefinition
	   parent.reload()
	   println("git://gitcentos.mvista.com/centos/upstream/packages/" + item)
   }
}
jobs = Jenkins.instance.getAllItems(Job.class)
for (j in jobs) {
  if ( ! j.getLastBuild() ) {
     j.scheduleBuild();
  }
}
