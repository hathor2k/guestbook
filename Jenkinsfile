import java.text.SimpleDateFormat

def TODAY = (new SimpleDateFormat("yyyyMMddHHmmss")).format(new Date())

pipeline {
    agent { label 'master' }
    environment {
        strDockerTag = "${TODAY}_${BUILD_ID}"
        strDockerImage ="hathor2k/cicd_guestbook:${strDockerTag}"
    }

    stages {
        stage('Checkout') {
            agent { label 'agent1' }
            steps {
                git branch: 'master', url:'https://github.com/hathor2k/guestbook.git'
            }
        }
        stage('Build') {
            agent { label 'agent1' }
            steps {
                sh './mvnw clean package'
            }
        }
        stage('Unit Test') {
            agent { label 'agent1' }
            steps {
                sh './mvnw test'
            }
            
            post {
                always {
                    junit '**/target/surefire-reports/TEST-*.xml'
                }
            }
        }

        stage('SonarQube Analysis') {
            agent { label 'agent1' }
            steps{
                echo 'SonarQube Analysis'
                withSonarQubeEnv('sonarqube-server'){
                    sh '''
                        ./mvnw sonar:sonar \
                        -Dsonar.projectKey=guestbook \
                        -Dsonar.host.url=http://10.10.128.41:9000 \
                        -Dsonar.login=235a1c900259941e4f026556ecc2e06877909afc
                    '''
                }
            }
        }
        stage('SonarQube Quality Gate'){
            agent { label 'agent1' }
            steps{
                echo 'SonarQube Quality Gate'
                timeout(time: 1, unit: 'MINUTES') {
                    script{
                        def qg = waitForQualityGate()
                        if(qg.status != 'OK') {
                            echo "NOT OK Status: ${qg.status}"
                            error "Pipeline aborted due to quality gate failure: ${qg.status}"
                        } else{
                            echo "OK Status: ${qg.status}"
                        }
                    }
                }
            }
        }
        stage('Docker Image Build') {
            agent { label 'agent2' }
            steps {
                git branch: 'master', url:'https://github.com/hathor2k/guestbook.git'
                sh './mvnw clean package'
                script {
                    oDockImage = docker.build(strDockerImage)
                    oDockImage = docker.build(strDockerImage, "--build-arg VERSION=${strDockerTag} -f Dockerfile .")
                }
            }
        }
        stage('Docker Image Push') {
            agent { label 'agent2' }
            steps {
                script {
                    docker.withRegistry('', 'DockerHub_hathor2k') {
                        oDockImage.push()
                    }
                }
            }
        }
        stage('Staging Deploy') {
            agent { label 'master' }
            steps {
                sshagent(credentials: ['Staging-PrivateKey']) {
                    sh "ssh -o StrictHostKeyChecking=no ubuntu@10.10.128.41 docker container rm -f guestbookapp"
                    sh "ssh -o StrictHostKeyChecking=no ubuntu@10.10.128.41 docker container run \
                                        -d \
                                        -p 38080:80 \
                                        --name=guestbookapp \
                                        -e MYSQL_IP=10.10.128.41 \
                                        -e MYSQL_PORT=3306 \
                                        -e MYSQL_DATABASE=guestbook \
                                        -e MYSQL_USER=root \
                                        -e MYSQL_PASSWORD=education \
                                        ${strDockerImage} "
                }
            }
        }
        stage ('JMeter LoadTest') {
            agent { label 'agent1' }
            steps { 
                sh '~/lab/sw/jmeter/bin/jmeter.sh -j jmeter.save.saveservice.output_format=xml -n -t src/main/jmx/guestbook_loadtest.jmx -l loadtest_result.jtl' 
                perfReport filterRegex: '', showTrendGraphs: true, sourceDataFiles: 'loadtest_result.jtl' 
            } 
        }
    }
    post { 
        always { 
            emailext (attachLog: true, body: '본문', compressLog: true
                    , recipientProviders: [buildUser()], subject: '제목', to: 'hathor2kj@gmail.com')

        }
        success { 
            slackSend(tokenCredentialId: 'slack-token'
                , channel: '#devops-test'
                , color: 'good'
                , message: "${JOB_NAME} (${BUILD_NUMBER}) 빌드가 성공적으로 끝났습니다. Details: (<${BUILD_URL} | here >)")
        }
        failure { 
            slackSend(tokenCredentialId: 'slack-token'
                , channel: '#devops-test'
                , color: 'danger'
                , message: "${JOB_NAME} (${BUILD_NUMBER}) 빌드가 실패하였습니다. Details: (<${BUILD_URL} | here >)")
    }
  }
}

