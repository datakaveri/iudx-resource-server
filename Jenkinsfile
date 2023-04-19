pipeline {

  environment {
    devRegistry = 'ghcr.io/datakaveri/rs-dev'
    deplRegistry = 'ghcr.io/datakaveri/rs-depl'
    testRegistry = 'ghcr.io/datakaveri/rs-test:latest'
    registryUri = 'https://ghcr.io'
    registryCredential = 'datakaveri-ghcr'
    GIT_HASH = GIT_COMMIT.take(7)
  }

  agent { 
    node {
      label 'slave1' 
    }
  }

  stages {
    
    stage('Build images') {
      steps{
        script {
          echo 'Pulled - ' + env.GIT_BRANCH
          devImage = docker.build( devRegistry, "-f ./docker/dev.dockerfile .")
          deplImage = docker.build( deplRegistry, "-f ./docker/depl.dockerfile .")
          testImage = docker.build( testRegistry, "-f ./docker/test.dockerfile .")
        }
      }
    }

    stage('Unit Tests and Code Coverage Test'){
      steps{
        script{
          sh 'docker compose -f docker-compose.test.yml up test'
        }
        xunit (
          thresholds: [ skipped(failureThreshold: '1'), failed(failureThreshold: '0') ],
          tools: [ JUnit(pattern: 'target/surefire-reports/*.xml') ]
        )
        jacoco classPattern: 'target/classes', execPattern: 'target/jacoco.exec', sourcePattern: 'src/main/java', exclusionPattern:'iudx/resource/server/apiserver/ApiServerVerticle.class,**/*VertxEBProxy.class,**/Constants.class,**/*VertxProxyHandler.class,**/*Verticle.class,iudx/resource/server/database/archives/DatabaseService.class,iudx/resource/server/database/async/AsyncService.class,iudx/resource/server/database/latest/LatestDataService.class,iudx/resource/server/deploy/*.class,iudx/resource/server/database/postgres/PostgresService.class,iudx/resource/server/apiserver/ManagementRestApi.class,iudx/resource/server/apiserver/AdminRestApi.class,iudx/resource/server/apiserver/AsyncRestApi.class,iudx/resource/server/callback/CallbackService.class,**/JwtDataConverter.class,**/EncryptionService.class'
      }
      post{
      always {
                recordIssues enabledForFailure: true, tool: checkStyle(pattern: 'target/checkstyle-result.xml')
                recordIssues enabledForFailure: true, tool: pmdParser(pattern: 'target/pmd.xml')
              }
        failure{
          script{
            sh 'docker compose -f docker-compose.test.yml down --remove-orphans'
          }
          error "Test failure. Stopping pipeline execution!"
        }
        cleanup{
          script{
            sh 'sudo rm -rf target/'
          }
        }        
      }
    }

    stage('Start Resource-Server for Performance and Integration Testing'){
      steps{
        script{
          sh 'scp Jmeter/ResourceServer.jmx jenkins@jenkins-master:/var/lib/jenkins/iudx/rs/Jmeter/'
          sh 'scp src/test/resources/IUDX-Resource-Server-Consumer-APIs-V4.5.0.postman_collection.json jenkins@jenkins-master:/var/lib/jenkins/iudx/rs/Newman/'
          sh 'docker compose -f docker-compose.test.yml up -d perfTest'
          sh 'sleep 120'
        }
      }
      post{
        failure{
          script{
            sh 'docker compose -f docker-compose.test.yml down --remove-orphans'
          }
        }
      }
    }
    
    stage('Jmeter Performance Test'){
      steps{
        script{
          env.puneToken = sh(returnStdout: true, script: 'python3 Jenkins/resources/get-token.py --pune').trim()
          env.suratToken = sh(returnStdout: true, script: 'python3 Jenkins/resources/get-token.py --surat').trim()
        }
        node('built-in') {
          script{
            sh 'rm -rf /var/lib/jenkins/iudx/rs/Jmeter/report ; mkdir -p /var/lib/jenkins/iudx/rs/Jmeter/report'
            sh "set +x;/var/lib/jenkins/apache-jmeter/bin/jmeter.sh -n -t /var/lib/jenkins/iudx/rs/Jmeter/ResourceServer.jmx -l /var/lib/jenkins/iudx/rs/Jmeter/report/JmeterTest.jtl -e -o /var/lib/jenkins/iudx/rs/Jmeter/report/ -Jhost=jenkins-slave1 -JpuneToken=$env.puneToken -JsuratToken=$env.suratToken"
          }
          perfReport filterRegex: '', showTrendGraphs: true, sourceDataFiles: '/var/lib/jenkins/iudx/rs/Jmeter/report/*.jtl'     
        }
      }
      post{
        failure{
          script{
            sh 'docker compose -f docker-compose.test.yml  down --remove-orphans'
          }
        }
      }
    }

    stage('Integration Tests and OWASP ZAP pen test'){
      steps{
        node('built-in') {
          script{
            startZap ([host: 'localhost', port: 8090, zapHome: '/var/lib/jenkins/tools/com.cloudbees.jenkins.plugins.customtools.CustomTool/OWASP_ZAP/ZAP_2.11.0'])
            sh 'curl http://127.0.0.1:8090/JSON/pscan/action/disableScanners/?ids=10096'
            sh 'HTTP_PROXY=\'127.0.0.1:8090\' newman run /var/lib/jenkins/iudx/rs/Newman/IUDX-Resource-Server-Consumer-APIs-V4.5.0.postman_collection.json -e /home/ubuntu/configs/rs-postman-env.json -n 2 --insecure -r htmlextra --reporter-htmlextra-export /var/lib/jenkins/iudx/rs/Newman/report/report.html --reporter-htmlextra-skipSensitiveData'
            runZapAttack()
          }
        }
      }
      post{
        always{
          node('built-in') {
            script{
              publishHTML([allowMissing: false, alwaysLinkToLastBuild: true, keepAll: true, reportDir: '/var/lib/jenkins/iudx/rs/Newman/report/', reportFiles: 'report.html', reportTitles: '', reportName: 'Integration Test Report'])
              archiveZap failHighAlerts: 1, failMediumAlerts: 1, failLowAlerts: 1
            }
          }
        }
        failure{
          error "Test failure. Stopping pipeline execution!"
        }
        cleanup{
          script{
            sh 'docker compose -f docker-compose.test.yml down --remove-orphans'
          } 
        }
      }
    }

    stage('Continuous Deployment') {
      when {
        allOf {
          anyOf {
            changeset "docker/**"
            changeset "docs/**"
            changeset "pom.xml"
            changeset "src/main/**"
            triggeredBy cause: 'UserIdCause'
          }
          expression {
            return env.GIT_BRANCH == 'origin/master';
          }
        }
      }
      stages {
        stage('Push Images') {
          steps {
            script {
              docker.withRegistry( registryUri, registryCredential ) {
                devImage.push("5.0.0-alpha-${env.GIT_HASH}")
                deplImage.push("5.0.0-alpha-${env.GIT_HASH}")
              }
            }
          }
        }
        stage('Docker Swarm deployment') {
          steps {
            script {
              sh "ssh azureuser@docker-swarm 'docker service update rs_rs --image ghcr.io/datakaveri/rs-depl:5.0.0-alpha-${env.GIT_HASH}'"
              sh 'sleep 10'
            }
          }
          post{
            failure{
              error "Failed to deploy image in Docker Swarm"
            }
          }          
        }
        stage('Integration test on swarm deployment') {
          steps {
            node('built-in') {
              script{
                sh 'newman run /var/lib/jenkins/iudx/rs/Newman/IUDX-Resource-Server-Consumer-APIs-V4.5.0.postman_collection.json -e /home/ubuntu/configs/cd/rs-postman-env.json --insecure -r htmlextra --reporter-htmlextra-export /var/lib/jenkins/iudx/rs/Newman/report/cd-report.html --reporter-htmlextra-skipSensitiveData'
              }
            }
          }
          post{
            always{
              node('built-in') {
                script{
                  publishHTML([allowMissing: false, alwaysLinkToLastBuild: true, keepAll: true, reportDir: '/var/lib/jenkins/iudx/rs/Newman/report/', reportFiles: 'cd-report.html', reportTitles: '', reportName: 'Docker-Swarm Integration Test Report'])
                }
              }
            }
            failure{
              error "Test failure. Stopping pipeline execution!"
            }
          }
        }
      }
    }
  }
}
