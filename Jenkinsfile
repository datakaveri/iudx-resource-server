properties([pipelineTriggers([githubPush()])])
pipeline {

  environment {
    devRegistry = 'ghcr.io/karun-singh/iudx-resource-server:dev'
    deplRegistry = 'ghcr.io/karun-singh/iudx-resource-server:depl'
    testRegistry = 'ghcr.io/karun-singh/iudx-resource-server:devtest'
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
          thresholds: [ skipped(failureThreshold: '5'), failed(failureThreshold: '20') ],
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
          sh 'scp src/test/resources/IUDX-Resource-Server-Release-v2.1.postman_collection.json jenkins@20.193.225.59:/var/lib/jenkins/iudx/rs/Newman/'
          sh 'docker-compose -f docker-compose-production.yml up -d rs'
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
            set +x
          authtoken=$(curl --silent --location --request POST \'https://authdev.iudx.io/auth/v1/token\' --cert /var/lib/jenkins/iudx/rs/cert.pem --key /var/lib/jenkins/iudx/rs/privkey.pem --header \'Content-Type: application/json\' --data-raw \'{
    "request": [
       "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information"
    ]
}\' | grep -o \'authdev[^"]*\' | awk \'{print $1}\')
/var/lib/jenkins/apache-jmeter-5.4.1/bin/jmeter.sh -n -t /var/lib/jenkins/iudx/rs/Jmeter/ResourceServer.jmx -l /var/lib/jenkins/iudx/rs/Jmeter/report/JmeterTest.jtl -e -o /var/lib/jenkins/iudx/rs/Jmeter/report/ -Jtoken=$authtoken
set -x
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

    stage('Run Newman collection'){
      steps{
        node('master') {
          script{
            //catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
              sh 'rm -rf /var/lib/jenkins/iudx/rs/Newman/report/report.html'
              sh 'newman run /var/lib/jenkins/iudx/rs/Newman/IUDX-Resource-Server-Release-v2.1.postman_collection.json -e /var/lib/jenkins/iudx/rs/Newman/postman-env.json --insecure -r htmlextra --reporter-htmlextra-export /var/lib/jenkins/iudx/rs/Newman/report/report.html'
            //}
          }
        }
      }
      post{
        always{
          node('master') {
            publishHTML([allowMissing: false, alwaysLinkToLastBuild: true, keepAll: false, reportDir: '/var/lib/jenkins/iudx/rs/Newman/report/', reportFiles: 'report.html', reportName: 'HTML Report', reportTitles: ''])
          }
        }
      }
    }

    stage('Clean up'){
      steps{
        sh 'docker-compose down --remove-orphans'
      }
    }

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
