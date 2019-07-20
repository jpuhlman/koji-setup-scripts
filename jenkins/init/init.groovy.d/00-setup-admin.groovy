import jenkins.*
import hudson.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.jenkins.plugins.sshcredentials.impl.*
import hudson.plugins.sshslaves.*;
import hudson.model.*
import jenkins.model.*
import hudson.security.*

global_domain = Domain.global()
credentials_store =
  Jenkins.instance.getExtensionList(
    'com.cloudbees.plugins.credentials.SystemCredentialsProvider'
  )[0].getStore()

credentials = new BasicSSHUserPrivateKey(CredentialsScope.GLOBAL,null,"root",new BasicSSHUserPrivateKey.UsersPrivateKeySource(),"","")

credentials_store.addCredentials(global_domain, credentials)

def hudsonRealm = new HudsonPrivateSecurityRealm(false)
def adminUsername = System.getenv('JENKINS_ADMIN_USERNAME') ?: 'admin'
def adminPassword = System.getenv('JENKINS_ADMIN_PASSWORD') ?: 'password'
hudsonRealm.createAccount(adminUsername, adminPassword)
//hudsonRealm.createAccount("charles", "charles")

def instance = Jenkins.getInstance()
instance.setSecurityRealm(hudsonRealm)
instance.save()


def strategy = new GlobalMatrixAuthorizationStrategy()

//  Setting Anonymous Permissions
strategy.add(hudson.model.Hudson.READ,'anonymous')
//strategy.add(hudson.model.Item.BUILD,'anonymous')
//strategy.add(hudson.model.Item.CANCEL,'anonymous')
strategy.add(hudson.model.Item.DISCOVER,'anonymous')
strategy.add(hudson.model.Item.READ,'anonymous')

// Setting Admin Permissions
strategy.add(Jenkins.ADMINISTER, "admin")

instance.setAuthorizationStrategy(strategy)
instance.save()

//fix "Agent to master security subsystem is currently off."
import jenkins.security.s2m.*
instance.injector.getInstance(AdminWhitelistRule.class).setMasterKillSwitch(false);
instance.save()

//fix complaint amount missing URL setting.
jlc = JenkinsLocationConfiguration.get()
jlc.setUrl("http://bn1slave-109.mvista.com:8080/")
jlc.save()

//fix You have not configured the CSRF issuer. This could be a security issue.
import hudson.security.csrf.DefaultCrumbIssuer 
instance.setCrumbIssuer(new DefaultCrumbIssuer(true));
