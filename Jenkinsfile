properties([pipelineTriggers([githubPush()])])
pipeline {
  environment {
    devRegistry = 'dockerhub.iudx.io/jenkins/resource-dev'
    deplRegistry = 'dockerhub.iudx.io/jenkins/resource-depl'
    testRegistry = 'dockerhub.iudx.io/jenkins/resource-test'
    registryUri = 'https://dockerhub.iudx.io'
    registryCredential = 'docker-jenkins'
  }
  agent any
  stages {
    stage('Building images') {
      steps{
        script {
          devImage = docker.build( devRegistry, "-f ./docker/dev.dockerfile .")
          deplImage = docker.build( deplRegistry, "-f ./docker/depl.dockerfile .")
          testImage = docker.build( testRegistry, "-f ./docker/test.dockerfile .")
        }
      }
    }
    stage('Run Tests'){
      steps{
        script{
          sh 'docker-compose up test'
        }
      }
    }
    stage('Capture Test results'){
      steps{
        xunit (
                thresholds: [ skipped(failureThreshold: '3'), failed(failureThreshold: '4') ],
                tools: [ JUnit(pattern: 'target/surefire-reports/*Test.xml') ]
        )
      }
      post{
        failure{
          error "Test failure. Stopping pipeline execution!"
        }
      }
    }
    stage('Run Jmeter Performance Tests'){
      steps{
        script{
          sh 'rm -rf Jmeter/report ; mkdir -p Jmeter/report'
          sh 'docker-compose -f docker-compose-production.yml up -d rs'
          sh 'sleep 45'
          authtoken=$(. /var/lib/jenkins/iudx/rs/generateToken.sh)
          /var/lib/jenkins/apache-jmeter-5.4.1/bin/jmeter.sh -n -t Jmeter/ResourceServer.jmx -l Jmeter/report/JmeterTest.jtl -e -o Jmeter/report/ -Jtoken=$authtoken
          //sh 'authtoken=$(sh /var/lib/jenkins/iudx/rs/generateToken.sh); rm -rf Jmeter/report ; mkdir -p Jmeter/report ; /var/lib/jenkins/apache-jmeter-5.4.1/bin/jmeter.sh -n -t Jmeter/ResourceServer.jmx -l Jmeter/report/JmeterTest.jtl -e -o Jmeter/report/ -Jtoken=$authtoken'
          sh 'docker-compose down'
        }
      }
    }
    stage('Capture Jmeter report'){
      steps{
        perfReport filterRegex: '', sourceDataFiles: 'Jmeter/report/*.jtl'
      }
    }
    stage('Push Image') {
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
