#!/usr/bin/groovy

@Library('test-shared-library@1.1') _


pipeline {
    agent {
        dockerfile {
            label 'docker'
            filename "Dockerfile"
            reuseNode true
        }
    }

    environment {
        JAVA_HOME='/usr/local/openjdk-11'
    }

    options {
        ansiColor('xterm')
        timestamps()
        timeout(time: 60, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Build and Test') {
            steps {
                script {
                    try {
                        sh """
                            mvn clean package
                        """
                    } finally {
                        archiveArtifacts artifacts: 'target/javassist-*.jar, **/*out*', allowEmptyArchive: true
                    }
                }
            }
        }
    }
    post {
        always {
            cleanWs()
        }
    }
}