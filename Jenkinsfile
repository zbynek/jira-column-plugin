pipeline {
    agent any

    options {
        timestamps()
        ansiColor('xterm')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        MAVEN_OPTS = '-Xmx2g'
    }

    stages {

        stage('Build') {
            steps {
                sh "mvn -B package"
            }
        }
    }
}