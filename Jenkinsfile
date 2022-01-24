pipeline {

  environment {
    devRegistry = 'ghcr.io/karun-singh/iudx-resource-server:dev'
    deplRegistry = 'ghcr.io/karun-singh/iudx-resource-server:depl'
    testRegistry = 'ghcr.io/karun-singh/iudx-resource-server:test'
    registryUri = 'https://ghcr.io'
    registryCredential = 'karun-ghcr'
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
          devImage = docker.build( devRegistry, "-f ./docker/dev.dockerfile .")
          deplImage = docker.build( deplRegistry, "-f ./docker/depl.dockerfile .")
          testImage = docker.build( testRegistry, "-f ./docker/test.dockerfile .")
        }
      }
    }

    stage('Unit Tests'){
      steps{
        script{
          sh 'docker-compose up test'
        }
      }
    }

    stage('Capture Unit Test results'){
      steps{
        xunit (
          thresholds: [ skipped(failureThreshold: '10'), failed(failureThreshold: '20') ],
          tools: [ JUnit(pattern: 'target/surefire-reports/*Test.xml') ]
        )
      }
      post{
        failure{
          error "Test failure. Stopping pipeline execution!"
        }
      }
    }

    stage('Capture Code Coverage'){
      steps{
        jacoco classPattern: 'target/classes', execPattern: 'target/jacoco.exec', sourcePattern: 'src/main/java'
      }
    }

    stage('Start RS server for performance testing'){
      steps{
        script{
          sh 'scp Jmeter/ResourceServer.jmx jenkins@jenkins-master:/var/lib/jenkins/iudx/rs/Jmeter/'
          sh 'scp src/test/resources/IUDX-Resource-Server-Consumer-APIs-V3.5.postman_collection_new.json jenkins@jenkins-master:/var/lib/jenkins/iudx/rs/Newman/'
          sh 'docker-compose -f docker-compose.yml up -d perfTest'
          sh 'sleep 45'
        }
      }
    }
    
    stage('Jmeter Performance Test'){
      steps{
        node('master') {
          script{
            // echo 'token - '+ env.authtoken
            sh 'rm -rf /var/lib/jenkins/iudx/rs/Jmeter/report ; mkdir -p /var/lib/jenkins/iudx/rs/Jmeter/report'
            sh "set +x;/var/lib/jenkins/apache-jmeter-5.4.1/bin/jmeter.sh -n -t /var/lib/jenkins/iudx/rs/Jmeter/ResourceServer.jmx -l /var/lib/jenkins/iudx/rs/Jmeter/report/JmeterTest.jtl -e -o /var/lib/jenkins/iudx/rs/Jmeter/report/ -Jhost=jenkins-slave1 -JpuneToken=$env.puneToken -JsuratToken=$env.suratToken; set -x"
          }
        }
      }
    }

    stage('Capture Jmeter report'){
      steps{
        node('master') {
          perfReport filterRegex: '', sourceDataFiles: '/var/lib/jenkins/iudx/rs/Jmeter/report/*.jtl'
          // perfReport errorFailedThreshold: 0, errorUnstableThreshold: 0, filterRegex: '', showTrendGraphs: true, sourceDataFiles: '/var/lib/jenkins/iudx/rs/Jmeter/report/*.jtl'
        }
      }
      post{
        failure{
          error "Test failure. Stopping pipeline execution!"
        }
      }
    }

    // stage('Run Newman collection and ZAP test'){
    //   steps{
    //     node('master') {
    //       script{
    //         startZap ([host: 'localhost', port: 8090, zapHome: '/var/lib/jenkins/tools/com.cloudbees.jenkins.plugins.customtools.CustomTool/OWASP_ZAP/ZAP_2.11.0', additionalConfigurations: ["pscans.org.zaproxy.zap.extension.enabled=false"]])
    //         catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
    //           // sh 'rm -rf /var/lib/jenkins/iudx/rs/Newman/report/report.html'
    //           sh 'curl http://127.0.0.1:8090/JSON/pscan/action/disableScanners/?ids=10096'
    //           sh 'HTTP_PROXY=\'127.0.0.1:8090\' newman run /var/lib/jenkins/iudx/rs/Newman/IUDX-Resource-Server-Consumer-APIs-V3.5.postman_collection_new.json -e /home/ubuntu/configs/rs-postman-env.json --insecure -r htmlextra --reporter-htmlextra-export /var/lib/jenkins/iudx/rs/Newman/report/report.html'
    //         }
    //         runZapAttack()
    //       }
    //     }
    //   }
    //   post{
    //     always{
    //       node('master') {
    //         script{
    //           archiveZap failAllAlerts: 20
    //           publishHTML([allowMissing: false, alwaysLinkToLastBuild: true, keepAll: true, reportDir: '/var/lib/jenkins/iudx/rs/Newman/report/', reportFiles: 'report.html', reportName: 'HTML Report', reportTitles: '', reportName: 'Integration Test Report'])
    //         }
    //       }
    //       script{
    //         sh 'docker-compose logs perfTest > rs.log'
    //         sh 'scp rs.log jenkins@jenkins-master:/var/lib/jenkins/userContent/'
    //         echo 'container logs (rs.log) can be found at jenkins-url/userContent'
    //         sh 'docker-compose down --remove-orphans'
    //       } 
    //     }
    //   }
    // }

    stage('Push Images') {
      steps{
        script {
          docker.withRegistry( registryUri, registryCredential ) {
            devImage.push("3.0-${env.GIT_HASH}")
            deplImage.push("3.0-${env.GIT_HASH}")
          }
        }
      }
    }
  }
}
