pipeline {
    agent any

    environment {
        DEPLOY_DIR = '/home/wonryeol5336-server/docker/mumuk'
    }

    stages {
        stage('Build') {
            steps {
                sh 'chmod +x ./gradlew'
                sh './gradlew clean bootJar -x test'
            }
        }
    }
}
