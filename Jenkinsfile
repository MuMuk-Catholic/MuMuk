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

        stage('Docker Build') {
            steps {
                sh 'docker build -t mumuk-backend:latest .'
            }
        }

        stage('Sync Config') {
            steps {
                sh """
                    cp docker-compose.yml ${DEPLOY_DIR}/
                    cp -r prometheus/ ${DEPLOY_DIR}/
                    cp -r promtail/ ${DEPLOY_DIR}/
                    cp -r grafana/ ${DEPLOY_DIR}/
                """
            }
        }
    }
}
