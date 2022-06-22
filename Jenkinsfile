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
          sh 'docker-compose -f docker-compose.test.yml up test'
        }
        xunit (
          thresholds: [ skipped(failureThreshold: '8'), failed(failureThreshold: '0') ],
          tools: [ JUnit(pattern: 'target/surefire-reports/*.xml') ]
        )
        jacoco classPattern: 'target/classes', execPattern: 'target/jacoco.exec', sourcePattern: 'src/main/java', exclusionPattern:'iudx/resource/server/apiserver/ApiServerVerticle.class,**/*VertxEBProxy.class,**/Constants.class,**/*VertxProxyHandler.class,**/*Verticle.class,iudx/resource/server/database/archives/DatabaseService.class,iudx/resource/server/database/latest/LatestDataService.class,iudx/resource/server/deploy/*.class'
      }
      post{
        failure{
          script{
            sh 'docker-compose -f docker-compose.test.yml down --remove-orphans'
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
          sh 'scp src/test/resources/IUDX-Resource-Server-Consumer-APIs-V3.5.postman_collection_new.json jenkins@jenkins-master:/var/lib/jenkins/iudx/rs/Newman/'
          sh 'docker-compose -f docker-compose.test.yml up -d perfTest'
          sh 'sleep 45'
        }
      }
    }
    
    stage('Jmeter Performance Test'){
      steps{
        node('master') {
          script{
            sh 'rm -rf /var/lib/jenkins/iudx/rs/Jmeter/report ; mkdir -p /var/lib/jenkins/iudx/rs/Jmeter/report'
            sh "set +x;/var/lib/jenkins/apache-jmeter-5.4.1/bin/jmeter.sh -n -t /var/lib/jenkins/iudx/rs/Jmeter/ResourceServer.jmx -l /var/lib/jenkins/iudx/rs/Jmeter/report/JmeterTest.jtl -e -o /var/lib/jenkins/iudx/rs/Jmeter/report/ -Jhost=jenkins-slave1 -JpuneToken=$env.puneToken -JsuratToken=$env.suratToken"
          }
          perfReport filterRegex: '', showTrendGraphs: true, sourceDataFiles: '/var/lib/jenkins/iudx/rs/Jmeter/report/*.jtl'     
        }
      }
      post{
        failure{
          script{
            sh 'docker-compose down --remove-orphans'
          }
        }
      }
    }

    stage('Integration Tests and OWASP ZAP pen test'){
      steps{
        node('master') {
          script{
            startZap ([host: 'localhost', port: 8090, zapHome: '/var/lib/jenkins/tools/com.cloudbees.jenkins.plugins.customtools.CustomTool/OWASP_ZAP/ZAP_2.11.0'])
            sh 'curl http://127.0.0.1:8090/JSON/pscan/action/disableScanners/?ids=10096'
            sh 'HTTP_PROXY=\'127.0.0.1:8090\' newman run /var/lib/jenkins/iudx/rs/Newman/IUDX-Resource-Server-Consumer-APIs-V3.5.postman_collection_new.json -e /home/ubuntu/configs/3.5.0/rs-postman-env.json --insecure -r htmlextra --reporter-htmlextra-export /var/lib/jenkins/iudx/rs/Newman/report/report.html --reporter-htmlextra-skipSensitiveData'
            runZapAttack()
          }
        }
      }
      post{
        always{
          node('master') {
            script{
              archiveZap failHighAlerts: 1, failMediumAlerts: 1, failLowAlerts: 1
              publishHTML([allowMissing: false, alwaysLinkToLastBuild: true, keepAll: true, reportDir: '/var/lib/jenkins/iudx/rs/Newman/report/', reportFiles: 'report.html', reportTitles: '', reportName: 'Integration Test Report'])
            }
          }
        }
        failure{
          error "Test failure. Stopping pipeline execution!"
        }
        cleanup{
          script{
            sh 'docker-compose -f docker-compose.test.yml down --remove-orphans'
          } 
        }
      }
    }

    stage('Push Images') {
      when{
        expression {
          return env.GIT_BRANCH == 'origin/3.5.0';
        }
      }
      steps{
        script {
          docker.withRegistry( registryUri, registryCredential ) {
            devImage.push("4.0-alpha-${env.GIT_HASH}")
            deplImage.push("4.0-alpha-${env.GIT_HASH}")
          }
        }
      }
    }
  }
}
