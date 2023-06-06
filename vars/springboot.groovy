def call(){
    pipeline {
    agent any
    tools{
        maven 'MavenTool'
    }
    stages {
        stage("Get Configuration Files") {
            steps {
                sh 'echo ENVIRONMENT: $ENVIRONMENT'
                sh 'ls -la'
                dir ('temp') {
                    checkout changelog: false, poll: false, scm: scmGit(branches: [[name: '*/main']], extensions: [], userRemoteConfigs: [[credentialsId: "${GIT_PIPE_CONFIG_CREDENTIAL_NAME}", url: "${GIT_PIPE_CONFIG_URL}"]])
                }
                sh 'mv -f temp/$DOCKERFILE_SPRINGBOOT_PATH ./Dockerfile'
                sh 'cat ./Dockerfile'
                sh 'mv -f temp/scripts/branching-strategy-validation.sh ./branching-strategy-validation.sh'
                sh 'cat ./branching-strategy-validation.sh'
                sh 'mv -f temp/scripts/report-sonarqube.sh ./report-sonarqube.sh'
                sh 'cat ./report-sonarqube.sh'
                sh 'mv -f temp/dependencies/sonar-cnes-report-4.1.1.jar ./sonar-cnes-report-4.1.1.jar'
                sh 'ls -la'
                sh 'rm temp'
            }
        }

        stage("Raplace Tokens") {
            steps {
                sh 'sed -i s/#{EXPOSE_PORT}#/$DOCKERFILE_EXPOSE_PORT/g Dockerfile'
                sh 'cat Dockerfile'
            }
        }
        
        stage('SonarQube Analysis') {
            steps {
                
                sh 'ls -la'
                withSonarQubeEnv('SonarServer') {
                    sh 'mvn clean verify sonar:sonar'
                }
            }
        }
        
    //    stage("Checkmarx") {
    //         steps {
    //             step([$class: 'CxScanBuilder', comment: '', configAsCode: true, credentialsId: '', customFields: '', excludeFolders: '', exclusionsSetting: 'global', failBuildOnNewResults: false, failBuildOnNewSeverity: 'HIGH', filterPattern: '''!**/_cvs/**/*, !**/.svn/**/*, !**/.hg/**/*, !**/.git/**/*, !**/.bzr/**/*,
    //                 !**/.gitgnore/**/*, !**/.gradle/**/*, !**/.checkstyle/**/*, !**/.classpath/**/*, !**/bin/**/*,
    //                 !**/obj/**/*, !**/backup/**/*, !**/.idea/**/*, !**/*.DS_Store, !**/*.ipr, !**/*.iws,
    //                 !**/*.bak, !**/*.tmp, !**/*.aac, !**/*.aif, !**/*.iff, !**/*.m3u, !**/*.mid, !**/*.mp3,
    //                 !**/*.mpa, !**/*.ra, !**/*.wav, !**/*.wma, !**/*.3g2, !**/*.3gp, !**/*.asf, !**/*.asx,
    //                 !**/*.avi, !**/*.flv, !**/*.mov, !**/*.mp4, !**/*.mpg, !**/*.rm, !**/*.swf, !**/*.vob,
    //                 !**/*.wmv, !**/*.bmp, !**/*.gif, !**/*.jpg, !**/*.png, !**/*.psd, !**/*.tif, !**/*.swf,
    //                 !**/*.jar, !**/*.zip, !**/*.rar, !**/*.exe, !**/*.dll, !**/*.pdb, !**/*.7z, !**/*.gz,
    //                 !**/*.tar.gz, !**/*.tar, !**/*.gz, !**/*.ahtm, !**/*.ahtml, !**/*.fhtml, !**/*.hdm,
    //                 !**/*.hdml, !**/*.hsql, !**/*.ht, !**/*.hta, !**/*.htc, !**/*.htd, !**/*.war, !**/*.ear,
    //                 !**/*.htmls, !**/*.ihtml, !**/*.mht, !**/*.mhtm, !**/*.mhtml, !**/*.ssi, !**/*.stm,
    //                 !**/*.bin,!**/*.lock,!**/*.svg,!**/*.obj,
    //                 !**/*.stml, !**/*.ttml, !**/*.txn, !**/*.xhtm, !**/*.xhtml, !**/*.class, !**/*.iml, !Checkmarx/Reports/*.*,
    //                 !OSADependencies.json, !**/node_modules/**/*, !**/.cxsca-results.json, !**/.cxsca-sast-results.json, !.checkmarx/cx.config''', fullScanCycle: 10, groupId: '88', password: '{AQAAABAAAAAQbBNhu0/vp69ntf2YCBmiLUQIRA2dNr4q13KlSvWUnoM=}', preset: '0', projectName: 'TestUnityCM', sastEnabled: true, serverUrl: 'https://coppel.checkmarx.net', sourceEncoding: '1', username: '', vulnerabilityThresholdResult: 'FAILURE', waitForResultsEnabled: true])
    //         }
    //     }
        stage('Dockerize') {
            environment {
                pom = readMavenPom file: 'pom.xml'
                tag = sh(returnStdout: true, script: "echo $pom | awk -F':' '{print \$2 \":\" $env.BUILD_NUMBER}'")
            }
            steps {

                sh 'docker build -t $tag .'
            }
        }

        stage('Push Docker image') {
            environment {
                password = credentials('harborCredentials')
                pom = readMavenPom file: 'pom.xml'
                tag = sh(returnStdout: true, script: "echo $pom | awk -F':' '{print \$2 \":\" $env.BUILD_NUMBER}'")
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
        post {
        // Clean after build
        always {
            cleanWs(cleanWhenNotBuilt: false,
                    deleteDirs: true,
                    disableDeferredWipeout: true,
                    notFailBuild: true,
                    patterns: [[pattern: '.gitignore', type: 'INCLUDE'],
                               [pattern: '.propsfile', type: 'EXCLUDE']])
            }
        }
    }
}
