pipeline {
    agent any
    tools {
        maven "MAVEN"
        jdk "JDK"
    }
    stages {
        stage('Build') {
            steps { 
                sh 'mvn clean install'
                }
            }
       stage('Test') {
            steps { 
                echo "Running Test Cases"
                }
            }
      stage('Deploy') {
            steps { 
                sh 'docker build -t dmap_ext_dev:latest .'
                }
            }
        }
     }
    post {
       always {
          junit(
        allowEmptyResults: true,
        testResults: '*/test-reports/.xml'
      )
      }
   } 
