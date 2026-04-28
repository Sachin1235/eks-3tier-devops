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

        stage('SonarQube Analysis') {
            steps {
                dir('backend') {
                    sh 'mvn sonar:sonar -Dsonar.host.url=http://localhost:9000'
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                sh 'docker build -t ${ECR_REGISTRY}/${FRONTEND_IMAGE}:${IMAGE_TAG} ./frontend'
                sh 'docker build -t ${ECR_REGISTRY}/${BACKEND_IMAGE}:${IMAGE_TAG} ./backend'
            }
        }

        stage('Trivy Security Scan') {
            steps {
                sh 'trivy image --exit-code 1 --severity HIGH,CRITICAL ${ECR_REGISTRY}/${FRONTEND_IMAGE}:${IMAGE_TAG}'
                sh 'trivy image --exit-code 1 --severity HIGH,CRITICAL ${ECR_REGISTRY}/${BACKEND_IMAGE}:${IMAGE_TAG}'
            }
        }

        stage('Push to ECR') {
            steps {
                sh '''
                    aws ecr get-login-password --region ap-south-1 | \
                    docker login --username AWS --password-stdin ${ECR_REGISTRY}
                    
                    docker push ${ECR_REGISTRY}/${FRONTEND_IMAGE}:${IMAGE_TAG}
                    docker push ${ECR_REGISTRY}/${BACKEND_IMAGE}:${IMAGE_TAG}
                '''
            }
        }

        stage('Deploy to EKS') {
            steps {
                sh '''
                    aws eks update-kubeconfig --region ap-south-1 --name eks-3tier-cluster
                    
                    kubectl set image deployment/frontend \
                        frontend=${ECR_REGISTRY}/${FRONTEND_IMAGE}:${IMAGE_TAG} \
                        -n dev
                    
                    kubectl set image deployment/backend \
                        backend=${ECR_REGISTRY}/${BACKEND_IMAGE}:${IMAGE_TAG} \
                        -n dev
                    
                    kubectl rollout status deployment/frontend -n dev
                    kubectl rollout status deployment/backend -n dev
                '''
            }
        }
    }

    post {
        success {
            echo "Deployed successfully! Image tag: ${IMAGE_TAG}"
        }
        failure {
            echo "Deployment failed!"
        }
    }
}
