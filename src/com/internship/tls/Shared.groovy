package com.internship.tls

class Shared implements Serializable {
    def pipelineContext

    Shared(pipelineContext) {
        this.pipelineContext = pipelineContext
    }

    def getTagsScript(){
        return '''
            import groovy.json.JsonSlurperClassic

            def creds = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
                        com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials.class,
                        jenkins.model.Jenkins.instance)
            def jenkinscred = creds.findResult { it.username == 'jenkins' ? it : null }

            def projectID = (Application == "Backend") ? 9 : 5
            def list = ["latest"]
            def connection = new URL("https://gitlab.demo.think-it.work/api/v4/projects/${projectID}/repository/tags?private_token=${jenkinscred.password}").openConnection() as HttpURLConnection
            connection.setRequestProperty('Accept', 'application/json')
            def json = connection.inputStream.text

            data = new JsonSlurperClassic().parseText(json)
            data.each{ val-> list.add(val.name)}
            
            return list
        '''
    }

    def apiURLScript(){
        return '''
        if (Application == "Frontend" && Environment == "Development") {
            return ["tls-dev.demo.think-it.work/fake", "tls-dev.demo.think-it.work/api"]
        } else if (Application == "Frontend" && Environment == "Production") {
            return ["tls-dev.demo.think-it.work/fake", "tls-prod.demo.think-it.work/api"]
        } else {
            return ["Skip this step"]
        }
        '''
    }
    
    def getLastTag(appType, appVersion){
    
            if (appVersion == "latest") {
                def creds = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
                                        com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials.class,
                                        jenkins.model.Jenkins.instance)
                def jenkinscred = creds.findResult { it.username == 'jenkins' ? it : null }

                def projectID = (appType == "Backend") ? 9 : 5

                def sout = new StringBuffer(), serr = new StringBuffer()
                def proc =["bash", "-c", "curl https://gitlab.demo.think-it.work/api/v4/projects/${projectID}/repository/tags?private_token=${jenkinscred.password} | grep -Po '\"name\":[\".\\d]*' | head -1 | awk -F ':' '{ print \$2 }'"].execute()
                proc.consumeProcessOutput(sout, serr)
                proc.waitForOrKill(10000)

                return sout.replaceAll('"', '').trim()
            }
        
    }

}