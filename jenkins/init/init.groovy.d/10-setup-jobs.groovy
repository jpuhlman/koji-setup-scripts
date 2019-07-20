import hudson.plugins.git.*
import hudson.slaves.EnvironmentVariablesNodeProperty
import jenkins.model.Jenkins
import hudson.model.*
import hudson.security.*
import hudson.triggers.*
def strategy = new GlobalMatrixAuthorizationStrategy()

def repo = "git://gitcentos.mvista.com/centos/upstream/utils/centos-updates.git"
def branch = "c7-mv"
def cronTrigger = "H/20 * * * *"

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

envVars.put("DIST_TAG", "dist-centos-updates-mv")

instance.save()
Jenkins.instance.doQuietDown()
instance.save()
String fileContents = new File('/var/jenkins_home/app.list').getText('UTF-8')
def jobAdded = false
def jobTriggered = false
for (String item: fileContents.split()) {
   if ( !Jenkins.instance.getItemByFullName(item) ) {
           def scm = new GitSCM(repo)
           scm.branches = [new BranchSpec("*/" + branch)];
           def flowDefinition = new org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition(scm, "Jenkinsfile")

	   def parent = Jenkins.instance
	   def job = new org.jenkinsci.plugins.workflow.job.WorkflowJob(parent, item)
	   job.definition = flowDefinition
	   ParameterDefinition[] newParameters = [
              new StringParameterDefinition("PACKAGE", item, ""),
  	      new StringParameterDefinition("PACKAGE_BRANCH", branch, ""),
           ];
           job.removeProperty(ParametersDefinitionProperty.class)
           job.addProperty(new ParametersDefinitionProperty(newParameters))
	   parent.reload()
           jobAdded = true
   }
}
Jenkins.instance.doCancelQuietDown()
instance.save()
jobs = Jenkins.instance.getAllItems(Job.class)
for (j in jobs) {
       if ( ! j.getLastBuild() ) {
          j.scheduleBuild();
          jobTriggered = true
       }
}
if ( jobTriggered ) {
   sleep(10000)
   def q = Jenkins.instance.queue
   while (q.items) {
      println("waiting for first buids to complete")
      sleep 30000
   }
   println("All jobs complete")
   println("Adding timers to jobs")
   for (j in jobs) {
       if ( ! j.getTriggers() ) {
          j.addTrigger(new TimerTrigger(cronTrigger))
       }
   }
}
