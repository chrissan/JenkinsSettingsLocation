def call(){
    pipeline{
    agent any
    environment {
        pom = readMavenPom file: 'pom.xml'
        tag = sh(returnStdout: true, script: "echo $pom | awk -F':' '{print \$2 \":\" $env.BUILD_NUMBER}'")
    }
    stages{
        stage('Download Dockerfile'){
            steps{
                git branch: 'main', credentialsId: 'chris', url: 'git@github.com:BanCoppelUnity/pipeline-config.git'
                sh 'cp pipeline-config/dockerfiles/archetypes/springboot/Dockerfile .'
            }
        }
        stage('Dockerize') {
            steps {
                sh 'printenv'
                sh 'docker build -t $tag .'
            }
        }

        stage('Push Docker image') {
            environment {
                password = credentials('harborCredentials')
            }
            steps{
                sh 'docker login -u admin -p $password $harborURL'
                sh 'docker tag $tag tempservices.eastus.cloudapp.azure.com/archetype/$tag'
                sh 'docker push tempservices.eastus.cloudapp.azure.com/archetype/$tag'
                sh 'docker rmi $tag'
                sh 'docker rmi tempservices.eastus.cloudapp.azure.com/archetype/$tag'
            }
        }
        }
    }
}

