pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk 'JDK-17'
    }

    environment {
        SONAR_HOST_URL = 'http://sonarqube:9000'
        SONAR_LOGIN = credentials('sonarqube-token')
        DOCKER_IMAGE = 'vg-ms-users'
        DOCKER_TAG = "${BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                echo '=== Clonando repositorio ==='
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo '=== Compilando proyecto con Maven ==='
                sh 'mvn clean compile -DskipTests'
            }
        }

        stage('Unit Tests') {
            steps {
                echo '=== Ejecutando pruebas unitarias ==='
                echo '📊 Ejecutando todas las pruebas del proyecto...'
                sh 'mvn test'

                echo ''
                echo '🎯 Ejecutando pruebas específicas de UserServiceImplTest con logs detallados...'
                sh 'mvn test -Dtest=UserServiceImplTest -X | grep -E "(TEST [0-9]:|Usuario|Ejecutando:|Mock Data|Respuesta|COMPLETADO|Tests run:)" || true'
            }
            post {
                always {
                    echo '📋 Generando reportes de pruebas...'
                    junit '**/target/surefire-reports/*.xml'

                    echo '📊 Generando reporte de cobertura con JaCoCo...'
                    jacoco(
                        execPattern: '**/target/jacoco.exec',
                        classPattern: '**/target/classes',
                        sourcePattern: '**/src/main/java',
                        exclusionPattern: '**/config/**,**/dto/**,**/exception/**,**/enums/**'
                    )

                    // Publicar resultados de tests en el console output
                    script {
                        def testResults = junit(testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true)
                        echo "✅ Tests ejecutados: ${testResults.totalCount}"
                        echo "✅ Tests exitosos: ${testResults.passCount}"
                        echo "❌ Tests fallidos: ${testResults.failCount}"
                        echo "⚠️  Tests omitidos: ${testResults.skipCount}"
                    }
                }
                success {
                    echo '✅ Todas las pruebas unitarias pasaron correctamente!'
                }
                failure {
                    echo '❌ Algunas pruebas unitarias fallaron. Revisar logs.'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo '=== Analizando código con SonarQube ==='
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        mvn sonar:sonar \
                          -Dsonar.projectKey=vg-ms-users \
                          -Dsonar.projectName="VG MS Users" \
                          -Dsonar.host.url=${SONAR_HOST_URL} \
                          -Dsonar.login=${SONAR_LOGIN}
                    '''
                }
            }
        }

        stage('Quality Gate') {
            steps {
                echo '=== Verificando Quality Gate de SonarQube ==='
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Package') {
            steps {
                echo '=== Empaquetando aplicación ==='
                sh 'mvn package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                echo '=== Construyendo imagen Docker ==='
                script {
                    docker.build("${DOCKER_IMAGE}:${DOCKER_TAG}")
                    docker.build("${DOCKER_IMAGE}:latest")
                }
            }
        }

        stage('Deploy to Dev') {
            steps {
                echo '=== Desplegando en ambiente de desarrollo ==='
                sh '''
                    docker stop vg-ms-users-dev || true
                    docker rm vg-ms-users-dev || true
                    docker run -d \
                      --name vg-ms-users-dev \
                      --network devops-network \
                      -p 8085:8085 \
                      -e SPRING_PROFILES_ACTIVE=dev \
                      -e SPRING_DATA_MONGODB_URI=mongodb://admin:admin123@mongodb:27017/users_db?authSource=admin \
                      ${DOCKER_IMAGE}:${DOCKER_TAG}
                '''
            }
        }
    }

    post {
        always {
            echo '=== Limpiando workspace ==='
            cleanWs()
        }
        success {
            echo '✅ Pipeline ejecutado exitosamente!'
            emailext(
                subject: "✅ BUILD SUCCESS: ${env.JOB_NAME} - Build #${env.BUILD_NUMBER}",
                body: """
                    <h2>Build Exitoso</h2>
                    <p><strong>Proyecto:</strong> ${env.JOB_NAME}</p>
                    <p><strong>Build:</strong> #${env.BUILD_NUMBER}</p>
                    <p><strong>Estado:</strong> SUCCESS</p>
                    <p><a href="${env.BUILD_URL}">Ver detalles del build</a></p>
                """,
                to: 'team@example.com',
                mimeType: 'text/html'
            )
        }
        failure {
            echo '❌ Pipeline falló!'
            emailext(
                subject: "❌ BUILD FAILED: ${env.JOB_NAME} - Build #${env.BUILD_NUMBER}",
                body: """
                    <h2>Build Fallido</h2>
                    <p><strong>Proyecto:</strong> ${env.JOB_NAME}</p>
                    <p><strong>Build:</strong> #${env.BUILD_NUMBER}</p>
                    <p><strong>Estado:</strong> FAILURE</p>
                    <p><a href="${env.BUILD_URL}">Ver detalles del build</a></p>
                """,
                to: 'team@example.com',
                mimeType: 'text/html'
            )
        }
    }
}
