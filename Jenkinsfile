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
    // stage('Run Tests'){
    //   steps{
    //     script{
    //       sh 'docker-compose up test'
    //     }
    //   }
    // }
    // stage('Capture Test results'){
    //   steps{
    //     xunit (
    //             thresholds: [ skipped(failureThreshold: '0'), failed(failureThreshold: '0') ],
    //             tools: [ JUnit(pattern: 'target/surefire-reports/*Test.xml') ]
    //     )
    //   }
    // }
    stage('Run Jmeter Performance Tests'){
      steps{
        script{
          sh 'docker-compose -f docker-compose-production.yml up -d rs'
          sh 'sleep 45'
          //sh 'rm -rf Jmeter/LatestData ; mkdir -p Jmeter/LatestData ; /var/lib/jenkins/apache-jmeter-5.4.1/bin/jmeter.sh -n -t Jmeter/LatestData.jmx -l Jmeter/LatestData/latestData.jtl -e -o Jmeter/LatestData/'
          //sh 'rm -rf Jmeter/TemporalCount ; mkdir -p Jmeter/TemporalCount ; /var/lib/jenkins/apache-jmeter-5.4.1/bin/jmeter.sh -n -t Jmeter/TemporalCount.jmx -l Jmeter/TemporalCount/temporalCount.jtl -e -o Jmeter/TemporalCount/'
          //sh 'rm -rf Jmeter/TemporalSearch ; mkdir -p Jmeter/TemporalSearch ; /var/lib/jenkins/apache-jmeter-5.4.1/bin/jmeter.sh -n -t Jmeter/TemporalSearch.jmx -l Jmeter/TemporalSearch/temporalSearch.jtl -e -o Jmeter/TemporalSearch/'
          sh 'rm -rf Jmeter/report ; mkdir -p Jmeter/report ; /var/lib/jenkins/apache-jmeter-5.4.1/bin/jmeter.sh -n -t Jmeter/Resourceserver.jmx -l Jmeter/report/JmeterTest.jtl -e -o Jmeter/report/'
          sh 'docker-compose down'
        }
      }
    }
    stage('Capture Jmeter report'){
      steps{
        //perfReport filterRegex: '', sourceDataFiles: 'Jmeter/LatestData/*.jtl;Jmeter/TemporalCount/*.jtl;Jmeter/TemporalSearch/*.jtl'
        perfReport filterRegex: '', sourceDataFiles: 'Jmeter/report/*.jtl'
      }
    }

    // stage('Push Image') {
    //   steps{
    //     script {
    //       docker.withRegistry( registryUri, registryCredential ) {
    //         devImage.push()
    //         deplImage.push()
    //         testImage.push()
    //       }
    //     }
    //   }
    // }
    // stage('Remove Unused docker image') {
    //  steps{
    //    sh "docker rmi dockerhub.iudx.io/jenkins/catalogue-dev"
    //    sh "docker rmi dockerhub.iudx.io/jenkins/catalogue-depl"
    //  }
    //}
  }
}
