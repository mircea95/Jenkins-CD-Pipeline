/* groovylint-disable LineLength */
// ${JOB_NAME}
import com.internship.tls.Shared

def call() {
    def shared = new Shared(this)
    def nameSpace = (params.Environment == "Development") ? "tls-dev" : "tls-prod"
    def appName = (params.Application == "Frontend") ? "tls-fe" : "tls-be"
    def appVersion = (params.Version == "latest") ? shared.getLastTag(params.Application, params.Version) : params.Version
    
    pipeline {
        agent {
            node {
                label 'training-licenses-sharing-cloud'
            }
        }

        stages {
            stage('Setup parameters') {
                steps {
                    script {
                        properties([
                                parameters([
                                        [$class      : 'ChoiceParameter',
                                         choiceType  : 'PT_SINGLE_SELECT',
                                         description : 'Select the Environemnt',
                                         filterLength: 1,
                                         filterable  : false,
                                         name        : 'Environment',
                                         script      : [
                                                 $class        : 'GroovyScript',
                                                 fallbackScript: [
                                                         classpath: [],
                                                         sandbox  : true,
                                                         script   :
                                                                 "return['Could not get The environemnts']"
                                                 ],
                                                 script        : [
                                                         classpath: [],
                                                         sandbox  : true,
                                                         script   : "return['Development', 'Production']"
                                                 ]
                                         ]
                                        ],

                                        [$class      : 'ChoiceParameter',
                                         choiceType  : 'PT_SINGLE_SELECT',
                                         description : 'Select the application you want to implement',
                                         filterLength: 1,
                                         filterable  : false,
                                         name        : 'Application',
                                         script      : [
                                                 $class        : 'GroovyScript',
                                                 fallbackScript: [
                                                         classpath: [],
                                                         sandbox  : true,
                                                         script   :
                                                                 "return['Could not get app']"
                                                 ],
                                                 script        : [
                                                         classpath: [],
                                                         sandbox  : true,
                                                         script   :
                                                                 "return['Frontend', 'Backend']"
                                                 ]
                                         ]
                                        ],

                                        [$class              : 'CascadeChoiceParameter',
                                         choiceType          : 'PT_SINGLE_SELECT',
                                         description         : 'Select the version',
                                         name                : 'Version',
                                         referencedParameters: 'Application',
                                         filterable          : true,
                                         filterLength        : 1,
                                         script              :
                                                 [$class        : 'GroovyScript',
                                                  fallbackScript: [
                                                          classpath: [],
                                                          sandbox  : true,
                                                          script   : "return['Could not get tags from gitlab']"
                                                  ],
                                                  script        : [
                                                          classpath: [],
                                                          sandbox  : false,
                                                          script   : shared.getTagsScript()
                                                  ]
                                                 ]
                                        ],
                                        [$class              : 'CascadeChoiceParameter',
                                         choiceType          : 'PT_SINGLE_SELECT',
                                         description         : 'Select the API URL',
                                         name                : 'API_URL',
                                         referencedParameters: 'Application, Environment',
                                         filterable          : false,
                                         filterLength        : 1,
                                         script              :
                                                 [$class        : 'GroovyScript',
                                                  fallbackScript: [
                                                          classpath: [],
                                                          sandbox  : true,
                                                          script   : "return['Could not get url']"
                                                  ],
                                                  script        : [
                                                          classpath: [],
                                                          sandbox  : false,
                                                          script   : shared.apiURLScript()
                                                  ]
                                                 ]
                                        ],
                                ])
                        ])
                    }
                }
            }
            stage('Deploy Frontend App') {
                when {
                    expression { params.Application == 'Frontend' }
                }
                steps {
                    script{
                        sh "sed -i 's#<API_URL>#${params.API_URL}#g' ./resources/deploy/values.yaml"
                        sh "sed -i 's#latest#${appVersion}#g' ./resources/deploy/Chart.yaml"
                        sh "helm upgrade --install ${appName} ./resources/deploy --set metadata.app=fe --set configmap.enabled=true --set image.tag=${appVersion} --set metadata.namespace=${nameSpace} -n ${nameSpace}"
                    }
                }
            }

            stage('Deploy Backend App') {
                when {
                    expression { params.Application == 'Backend' }
                }
                steps {
                    script{
                        sh "sed -i 's/latest/${appVersion}/g' ./resources/deploy/Chart.yaml"
                        sh "helm upgrade --install ${appName} ./resources/deploy --set metadata.app=be --set configmap.enabled=false --set image.tag=${appVersion} --set metadata.namespace=${nameSpace} -n ${nameSpace}"
                    }
                }
            }

            stage('INFO') {
                steps {
                    script{
                        sh "helm list -n ${nameSpace}"
                    }
                }
            }

        }
    }
}