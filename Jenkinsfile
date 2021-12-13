properties([pipelineTriggers([githubPush()])])
pipeline {

  environment {
    devRegistry = 'ghcr.io/karun-singh/iudx-resource-server:dev'
    deplRegistry = 'ghcr.io/karun-singh/iudx-resource-server:depl'
    testRegistry = 'ghcr.io/karun-singh/iudx-resource-server:test'
    registryUri = 'https://ghcr.io'
    registryCredential = 'karun-ghcr'
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

    stage('Start RS server for performance testing'){
      steps{
        script{
          sh 'scp Jmeter/ResourceServer.jmx jenkins@20.193.225.59:/var/lib/jenkins/iudx/rs/Jmeter/'
          sh 'scp src/test/resources/IUDX-Resource-Server-Consumer-APIs-V3.5.postman_collection.json jenkins@20.193.225.59:/var/lib/jenkins/iudx/rs/Newman/'
          sh 'docker-compose -f docker-compose.yml up -d perfTest'
          sh 'sleep 45'
        }
      }
    }
    
    stage('Jmeter Performance Test'){
      steps{
        node('master') {
          script{
            sh 'rm -rf /var/lib/jenkins/iudx/rs/Jmeter/report ; mkdir -p /var/lib/jenkins/iudx/rs/Jmeter/report'
            sh '''
          echo $authtoken; 
          /var/lib/jenkins/apache-jmeter-5.4.1/bin/jmeter.sh -n -t /var/lib/jenkins/iudx/rs/Jmeter/ResourceServer.jmx -l /var/lib/jenkins/iudx/rs/Jmeter/report/JmeterTest.jtl -e -o /var/lib/jenkins/iudx/rs/Jmeter/report/ -Jtoken=$authtoken
        '''
          }
        }
      }
    }

    stage('Capture Jmeter report'){
      steps{
        node('master') {
          perfReport filterRegex: '', sourceDataFiles: '/var/lib/jenkins/iudx/rs/Jmeter/report/*.jtl'
        }
      }
    }

    stage('Run Newman collection and ZAP test'){
      steps{
        node('master') {
          script{
            startZap ([host: 'localhost', port: 8090, zapHome: '/var/lib/jenkins/tools/com.cloudbees.jenkins.plugins.customtools.CustomTool/OWASP_ZAP/ZAP_2.11.0'])
            catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
              // sh 'rm -rf /var/lib/jenkins/iudx/rs/Newman/report/report.html'
              sh 'HTTP_PROXY=\'127.0.0.1:8090\' newman run /var/lib/jenkins/iudx/rs/Newman/IUDX-Resource-Server-Consumer-APIs-V3.5.postman_collection.json -e /home/ubuntu/configs/rs-postman-env.json --insecure -r htmlextra --reporter-htmlextra-export /var/lib/jenkins/iudx/rs/Newman/report/report.html'
            }
          }
        }
      }
      post{
        always{
          node('master') {
            script{
//               archiveZap failAllAlerts: 15
              publishHTML([allowMissing: false, alwaysLinkToLastBuild: true, keepAll: false, reportDir: '/var/lib/jenkins/iudx/rs/Newman/report/', reportFiles: 'report.html', reportName: 'HTML Report', reportTitles: ''])
            }
          }
        }
      }
    }

    // stage('Clean up'){
    //   steps{
    //     sh 'docker-compose down --remove-orphans'
    //   }
    // }

    stage('Push Images') {
      steps{
        script {
          docker.withRegistry( registryUri, registryCredential ) {
            devImage.push()
            deplImage.push()
          }
        }
      }
    }
  }
}
