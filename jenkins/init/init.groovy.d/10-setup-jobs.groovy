import hudson.plugins.git.*
import hudson.slaves.EnvironmentVariablesNodeProperty
import jenkins.model.Jenkins
import hudson.model.*
import hudson.security.*
def strategy = new GlobalMatrixAuthorizationStrategy()

def repo = "git://gitcentos.mvista.com/centos/upstream/utils/centos-updates.git"
def branch = "c7-mv"
def cronTrigger = "H/5 * * * *"

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

String fileContents = new File('/var/jenkins_home/apps').getText('UTF-8')

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
           job.addTrigger(new TimerTrigger(cronTrigger))
	   parent.reload()
   }
}
