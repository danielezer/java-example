timestamps {

    node('generic') {

        def server
        def rtFullUrl
        def rtIpAddress
        def buildNumber
        def mavenBuildName
        def dockerBuildName
        def mavenPromotionRepo = 'stable-maven-repo'
        def distributionUrl = "http://35.195.75.184"
        def releaseBundleName = 'java-project-bundle'

        stage("checkout") {
            checkout scm
            buildNumber = env.BUILD_NUMBER
            def jobName = env.JOB_NAME
            mavenBuildName = "maven-${jobName}"
            dockerBuildName = "docker-${jobName}"
        }

        stage("Build+Deploy") {
            server = Artifactory.server "local-artifactory"
            rtFullUrl = server.url
            rtIpAddress = rtFullUrl - ~/^http?.:\/\// - ~/\/artifactory$/

            def rtMaven = Artifactory.newMavenBuild()
            rtMaven.deployer server: server, releaseRepo: 'libs-snapshot-local', snapshotRepo: 'libs-snapshot-local'
            rtMaven.tool = 'maven-3.5.3'
            String mvnGoals = "-B clean install -DartifactVersion=${buildNumber} -s settings.xml"
            def buildInfo = Artifactory.newBuildInfo()
            buildInfo.name = mavenBuildName
            buildInfo.env.collect()
            rtMaven.run pom: 'pom.xml', goals: mvnGoals, buildInfo: buildInfo
            server.publishBuildInfo buildInfo
            def scanConfig = [
                    'buildName'  : buildInfo.name,
                    'buildNumber': buildInfo.number,
                    'failBuild'  : true
            ]
            server.xrayScan scanConfig

            def promotionConfig = [
                    'buildName'  : buildInfo.name,
                    'buildNumber': buildInfo.number,
                    'targetRepo' : mavenPromotionRepo,
                    'comment'    : 'This is a stable java-project version',
                    'status'     : 'Released',
                    'sourceRepo' : 'libs-snapshot-local',
                    'copy'       : true,
                    'failFast'   : true
            ]

            server.promote promotionConfig

        }

        stage("Build docker image") {
            def dockerBuildInfo = Artifactory.newBuildInfo()
            dockerBuildInfo.name = dockerBuildName
            def downloadSpec = """{
             "files": [
              {
                  "pattern": "libs-snapshot-local/com/mkyong/hashing/java-project/${buildNumber}-SNAPSHOT/java-project-*.jar",
                  "target": "target/downloads/",
                  "flat": "true"
                }
             ]
            }"""

            server.download spec: downloadSpec, buildInfo: dockerBuildInfo
            def rtDocker = Artifactory.docker server: server
            def dockerImageTag = "${rtIpAddress}/docker-java:${buildNumber}"
            docker.build(dockerImageTag)
            dockerBuildInfo.env.collect()
            rtDocker.push(dockerImageTag, 'docker-repo', dockerBuildInfo)
            server.publishBuildInfo dockerBuildInfo
            def dockerScanConfig = [
                    'buildName'  : dockerBuildInfo.name,
                    'buildNumber': dockerBuildInfo.number,
                    'failBuild'  : true
            ]
            server.xrayScan dockerScanConfig

            def dockerPromotionConfig = [
                    'buildName'  : dockerBuildInfo.name,
                    'buildNumber': dockerBuildInfo.number,
                    'targetRepo' : 'stable-docker-repo',
                    'comment'    : 'This is a stable java-project docker image',
                    'status'     : 'Released',
                    'sourceRepo' : 'docker-repo',
                    'copy'       : true,
                    'failFast'   : true
            ]

            Artifactory.addInteractivePromotion server: server, promotionConfig: dockerPromotionConfig, displayName: "Promote docker image to stable repo"

        }

        stage("Create release bundle") {

            withCredentials([usernameColonPassword(credentialsId: 'artifactory-login', variable: 'ARTIFACTORY_CREDS')]) {

                def rtServiceId = sh(returnStdout: true, script: "curl -s -u ${ARTIFACTORY_CREDS} -X GET ${rtFullUrl}/api/system/service_id").trim()

                def aqlQuery = """
                items.find({
                  \\\"\$and\\\": [
                    {
                      \\\"repo\\\": \\\"${mavenPromotionRepo}\\\"
                    },
                    {
                      \\\"@build.name\\\": \\\"${mavenBuildName}\\\"
                    },
                    {
                      \\\"@build.number\\\": \\\"${buildNumber}\\\"
                    }
                  ]
                })
                """.replaceAll(" ", "").replaceAll("\n", "")

                def releaseBundleBody = """
                {
                  \"name\": \"${releaseBundleName}\",
                  \"version\": \"${buildNumber}\",
                  \"dry_run\": false,
                  \"sign_immediately\": true,
                  \"description\": \"Release bundle for the example java-project\",
                  \"spec\": {
                    \"source_artifactory_id\": \"${rtServiceId}\",
                    \"queries\": [
                      {
                        \"aql\": \"${aqlQuery}\",
                        \"query_name\": \"java-project-query\"
                      }
                    ]
                  }
                }
            """

                def releaseBundleBodyJsonFile = 'release-bundle-body.json'
                writeFile file: 'release-bundle-body.json', text: releaseBundleBody

                archiveArtifacts artifacts: 'release-bundle-body.json'

                sh "curl -s -I -f -H 'Content-Type: application/json' -u ${ARTIFACTORY_CREDS} -X POST ${distributionUrl}/api/v1/release_bundle -T ${releaseBundleBodyJsonFile}"
            }
        }

        stage('Distribute release bundle') {
            withCredentials([usernameColonPassword(credentialsId: 'artifactory-login', variable: 'ARTIFACTORY_CREDS')]) {
                sh "curl -s -I -f -H 'Content-Type: application/json' -u ${ARTIFACTORY_CREDS} -X POST ${distributionUrl}/api/v1/distribution/${releaseBundleName}/${buildNumber} -T distribute-release-bundle-body.json"

                for (i = 0; true; i++) {

                    def res = sh(returnStdout: true,
                            script: "curl -s -f -u ${ARTIFACTORY_CREDS} " +
                                    "-X GET ${distributionUrl}/api/v1/release_bundle/${releaseBundleName}/${buildNumber}/distribution").trim()

                    def jsonResult = readJSON text: res
                    def distributionStatus = jsonResult.status.unique()
                    distributionStatus = distributionStatus.collect{ it.toUpperCase() }
                    println "Current status:  ${distributionStatus}"

                    if (distributionStatus == ['COMPLETED']) {
                        print "Distribution finished successfully!"
                        break
                    } else {
                        if (i >= 30) {
                            error("Timed out while waiting for distribution to complete")
                        } else if (distributionStatus.contains('FAILED')) {
                            error("Distribution failed with error: ${jsonResult}")
                        }
                    }
                    sleep 2
                }
            }
        }
    }
}