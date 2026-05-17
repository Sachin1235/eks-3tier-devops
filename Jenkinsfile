pipeline {

    agent any

    parameters {

        choice(
            name: 'ACTION',
            choices: ['build', 'deploy'],
            description: 'Select action'
        )

        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'qa', 'preprod', 'prod'],
            description: 'Select deployment environment'
        )

        string(
            name: 'DEPLOY_TAG',
            defaultValue: '',
            description: 'Enter image tag for deploy (example: 8)'
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

            when {
                expression {
                    params.ACTION == 'build'
                }
            }

            steps {
                dir('backend') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Docker Login') {

            when {
                expression {
                    params.ACTION == 'build'
                }
            }

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

            when {
                expression {
                    params.ACTION == 'build'
                }
            }

            steps {

                sh '''
                docker build -t ${ECR_REGISTRY}/${FRONTEND_IMAGE}:${IMAGE_TAG} ./frontend

                docker build -t ${ECR_REGISTRY}/${BACKEND_IMAGE}:${IMAGE_TAG} ./backend
                '''
            }
        }

        stage('Push Docker Images') {

            when {
                expression {
                    params.ACTION == 'build'
                }
            }

            steps {

                withCredentials([aws(credentialsId: 'aws-credentials')]) {

                    sh '''
                    docker push ${ECR_REGISTRY}/${FRONTEND_IMAGE}:${IMAGE_TAG}

                    docker push ${ECR_REGISTRY}/${BACKEND_IMAGE}:${IMAGE_TAG}
                    '''
                }
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

                withCredentials([aws(credentialsId: 'aws-credentials')]) {

                    script {

                        def TAG = params.ACTION == 'build' ? IMAGE_TAG : params.DEPLOY_TAG

                        sh """
                        helm upgrade --install employee-app-${params.ENVIRONMENT} ./helm/employee-app \
                        -f ./helm/employee-app/values-${params.ENVIRONMENT}.yaml \
                        --set backend.image.tag=${TAG} \
                        --set frontend.image.tag=${TAG} \
                        -n ${params.ENVIRONMENT}
                        """
                    }
                }
            }
        }

        stage('Verify Deployment') {

            steps {

                withCredentials([aws(credentialsId: 'aws-credentials')]) {

                    sh '''
                    kubectl get pods -n ${ENVIRONMENT}

                    kubectl get svc -n ${ENVIRONMENT}
                    '''
                }
            }
        }
    }

    post {

        success {
            echo "Deployment completed successfully!"
        }

        failure {
            echo "Deployment failed!"
        }
    }
}
