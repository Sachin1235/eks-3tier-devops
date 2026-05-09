pipeline {

    agent any

    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'qa', 'preprod', 'prod'],
            description: 'Select deployment environment'
        )
    }

    environment {

        AWS_DEFAULT_REGION = 'ap-south-1'

        ECR_REGISTRY = '757077150713.dkr.ecr.ap-south-1.amazonaws.com'

        FRONTEND_IMAGE = 'eks-3tier-frontend'
        BACKEND_IMAGE  = 'eks-3tier-backend'

        IMAGE_TAG = "${BUILD_NUMBER}"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Backend') {
            steps {
                dir('backend') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Docker Login') {
            steps {
                withCredentials([aws(credentialsId: 'aws-credentials')]) {

                    sh '''
                        aws ecr get-login-password --region ap-south-1 | \
                        docker login --username AWS --password-stdin ${ECR_REGISTRY}
                    '''
                }
            }
        }

        stage('Build Docker Images') {
            steps {

                sh '''
                    docker build -t ${ECR_REGISTRY}/${FRONTEND_IMAGE}:${IMAGE_TAG} ./frontend

                    docker build -t ${ECR_REGISTRY}/${BACKEND_IMAGE}:${IMAGE_TAG} ./backend
                '''
            }
        }

        stage('Push Docker Images') {
            steps {

                sh '''
                    docker push ${ECR_REGISTRY}/${FRONTEND_IMAGE}:${IMAGE_TAG}

                    docker push ${ECR_REGISTRY}/${BACKEND_IMAGE}:${IMAGE_TAG}
                '''
            }
        }

        stage('Update kubeconfig') {
            steps {

                withCredentials([aws(credentialsId: 'aws-credentials')]) {

                    sh '''
                        aws eks update-kubeconfig \
                        --region ap-south-1 \
                        --name eks-3tier-cluster
                    '''
                }
            }
        }

        stage('Deploy using Helm') {
            steps {

                sh '''
                    helm upgrade --install employee-app-${ENVIRONMENT} ./helm/employee-app \
                    -f ./helm/employee-app/values-${ENVIRONMENT}.yaml \
                    --set backend.image.tag=${IMAGE_TAG} \
                    --set frontend.image.tag=${IMAGE_TAG} \
                    -n ${ENVIRONMENT}
                '''
            }
        }

        stage('Verify Deployment') {
            steps {

                sh '''
                    kubectl get pods -n ${ENVIRONMENT}

                    kubectl get svc -n ${ENVIRONMENT}
                '''
            }
        }
    }

    post {

        success {

            echo "Application deployed successfully to ${ENVIRONMENT} with image tag ${IMAGE_TAG}"
        }

        failure {

            echo "Deployment failed!"
        }
    }
}
