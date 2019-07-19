import jenkins.*
import hudson.*
import hudson.plugins.sshslaves.*;
import hudson.model.*
import jenkins.model.*
import hudson.security.*
import java.util.logging.Logger

def logger = Logger.getLogger("")
def installed = false
def initialized = false
def pluginParameter="ant authorize-project build-timeout credentials-binding email-ext cloudbees-folder github-branch-source matrix-auth antisamy-markup-formatter github-organization-folder gradle workflow-aggregator pipeline pipeline-github-lib ssh-slaves timestamper ws-cleanup"
//def pluginParameter="matrix-auth credentials-binding"
def plugins = pluginParameter.split()
logger.info("" + plugins)
def instance = Jenkins.getInstance()
def pm = instance.getPluginManager()
def uc = instance.getUpdateCenter()
plugins.each {
  logger.info("Checking " + it)
  if (!pm.getPlugin(it)) {
    logger.info("Looking UpdateCenter for " + it)
    if (!initialized) {
      uc.updateAllSites()
      initialized = true
    }
    def plugin = uc.getPlugin(it)
    if (plugin) {
      logger.info("Installing " + it)
    	def installFuture = plugin.deploy()
      while(!installFuture.isDone()) {
        logger.info("Waiting for plugin install: " + it)
        sleep(3000)
      }
      installed = true
    }
  }
}
if (installed) {
  logger.info("Plugins installed, initializing a restart!")
  instance.save()
  instance.restart()
} else {
  instance.setNumExecutors(20)
  instance.save()
}

