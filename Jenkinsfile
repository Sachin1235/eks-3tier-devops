pipeline {
    agent any
    environment {
        AWS_DEFAULT_REGION = 'ap-south-1'
        ECR_REGISTRY = '757077150713.dkr.ecr.ap-south-1.amazonaws.com'
        FRONTEND_IMAGE = 'eks-3tier-frontend'
        BACKEND_IMAGE = 'eks-3tier-backend'
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
        stage('Build Docker Images') {
            steps {
                withCredentials([aws(credentialsId: 'aws-credentials')]) {
                    sh '''
                        aws ecr get-login-password --region ap-south-1 | \
                        docker login --username AWS --password-stdin ${ECR_REGISTRY}
                        docker build -t ${ECR_REGISTRY}/${FRONTEND_IMAGE}:${IMAGE_TAG} ./frontend
                        docker build -t ${ECR_REGISTRY}/${BACKEND_IMAGE}:${IMAGE_TAG} ./backend
                    '''
                }
            }
        }
        stage('Push to ECR') {
            steps {
                withCredentials([aws(credentialsId: 'aws-credentials')]) {
                    sh '''
                        docker push ${ECR_REGISTRY}/${FRONTEND_IMAGE}:${IMAGE_TAG}
                        docker push ${ECR_REGISTRY}/${BACKEND_IMAGE}:${IMAGE_TAG}
                    '''
                }
            }
        }
        stage('Deploy to EKS') {
            steps {
                withCredentials([aws(credentialsId: 'aws-credentials')]) {
                    sh '''
                        aws eks update-kubeconfig --region ap-south-1 --name eks-3tier-cluster
                        kubectl apply -f k8s/dev/backend-deployment.yaml
                        kubectl apply -f k8s/dev/frontend-deployment.yaml
                        kubectl rollout status deployment/backend -n dev
                        kubectl rollout status deployment/frontend -n dev
                    '''
                }
            }
        }
    }
    post {
        success {
            echo "App deployed successfully! Build: ${IMAGE_TAG}"
        }
        failure {
            echo "Deployment failed!"
        }
    }
}
