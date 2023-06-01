node {
    
    sh 'mkdir -p devops'
	dir("devops")

    {
        script{
            git branch: 'main', credentialsId: 'ssh01', url: 'git@github.com:BanCoppelUnity/pipeline-config.git'
            sh 'ls'
        }
    withEnv(["COMMIT=${env.GIT_COMMIT}","BRANCH=${env.GIT_BRANCH}"]) {    
        load '/jenkinsfiles/archetypes/springboot/load.Jenkinsfile'  
    }
    }
}
