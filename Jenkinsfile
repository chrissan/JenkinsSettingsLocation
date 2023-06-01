node {
    
    sh 'mkdir -p devops'
	dir("devops")

    {
        script{
            git branch: 'main', credentialsId: 'ssh01', url: 'git@github.com:BanCoppelUnity/springboot-test.git'
        }
    withEnv(["COMMIT=${scmVars.GIT_COMMIT}","BRANCH=${scmVars.GIT_BRANCH}"]) {    
        load 'devops/project_A/full-pipeline.Jenkinsfile'  
    }
    }
}
