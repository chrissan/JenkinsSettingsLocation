def call(){
    pipeline {
    agent any
    tools{
        maven 'MavenTool'
    }
    stages {
        
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
        stage('Download Dockerfile'){
            steps{
                sh 'pwd'
                git branch: 'main', credentialsId: 'chris', url: 'git@github.com:BanCoppelUnity/pipeline-config.git'
                cd "$WORSPACE"
                sh 'cp dockerfiles/archetypes/springboot/Dockerfile ../'
                sh 'cd ../'
                sh 'rm -rf pipeline-config'
                sh 'ls -la'
            }
        }
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
    }
}
