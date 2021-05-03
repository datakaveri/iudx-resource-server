properties([pipelineTriggers([githubPush()])])
pipeline {

  environment {
    devRegistry = 'dockerhub.iudx.io/jenkins/resource-dev'
    deplRegistry = 'dockerhub.iudx.io/jenkins/resource-depl'
    testRegistry = 'dockerhub.iudx.io/jenkins/resource-test'
    registryUri = 'https://dockerhub.iudx.io'
    registryCredential = 'docker-jenkins'
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
          thresholds: [ skipped(failureThreshold: '5'), failed(failureThreshold: '4') ],
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
          sh 'scp Jmeter/ResourceServer.jmx root@128.199.18.230:/var/lib/jenkins/iudx/rs/Jmeter/'
          sh 'scp src/test/resources/IUDX-Resource-Server-Release-v2.1.postman_collection.json root@128.199.18.230:/var/lib/jenkins/iudx/rs/Newman/'
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
            catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
              sh 'rm -rf /var/lib/jenkins/iudx/rs/Newman/report/report.html'
              sh 'newman run /var/lib/jenkins/iudx/rs/Newman/IUDX-Resource-Server-Release-v2.1.postman_collection.json -e /var/lib/jenkins/iudx/rs/Newman/postman-env.json --insecure -r htmlextra --reporter-htmlextra-export /var/lib/jenkins/iudx/rs/Newman/report/report.html'
            }
          }
        }
      }
      post{
        always{
          node('master') {
            sh 'scp -i /var/lib/jenkins/iudx/.ssh/id_rsa /var/lib/jenkins/iudx/rs/Newman/report/report.html root@139.59.72.254:/var/lib/jenkins/iudx/rs/Newman/report/'
          }
        }
      }
    }

    stage('Publish newman report'){
      steps{
        publishHTML([allowMissing: false, alwaysLinkToLastBuild: true, keepAll: false, reportDir: '/var/lib/jenkins/iudx/rs/Newman/report/', reportFiles: 'report.html', reportName: 'HTML Report', reportTitles: ''])
      }
      post{
        always{
          sh 'hostname'
          //sh 'docker-compose down --remove-orphans'
        }
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
