import hudson.model.User
import jenkins.model.Jenkins
import jenkins.security.ApiTokenProperty
import java.time.LocalDate

Jenkins.instance.getExtensionList('hudson.model.listeners.ItemListener').each { }

def userId = System.getenv('JENKINS_ADMIN_ID') ?: 'admin'
def tokenValue = System.getenv('JENKINS_API_TOKEN')
def user = User.getById(userId, false)

if (user != null && tokenValue) {
    def property = user.getProperty(ApiTokenProperty.class)
    if (property != null && !property.tokenStore.tokenList.any { it.name == 'local-platform' }) {
        property.tokenStore.addFixedNewToken('local-platform', tokenValue, LocalDate.now().plusYears(10))
        user.save()
        println('Created the local Jenkins API token for the platform.')
    }
}
