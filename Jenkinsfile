import groovy.json.JsonOutput

properties(
    [
        parameters(
            [
                string(name: 'distributionUrl',defaultValue: 'http://35.195.75.184', description: 'Distribution server URL')
            ]
        )
    ]
)

timestamps {

    node('generic') {

        def server
        def rtFullUrl
        def rtIpAddress
        def buildNumber
        def mavenBuildName
        def dockerBuildName
        def mavenPromotionRepo = 'stable-maven-repo'
        def distributionUrl = params.distributionUrl
        def releaseBundleName = 'java-project-bundle'
        def pipelineUtils
        def artifactoryCredentialId = 'artifactory-login'

        stage("checkout") {
            checkout scm
            buildNumber = env.BUILD_NUMBER
            def jobName = env.JOB_NAME
            mavenBuildName = "maven-${jobName}"
            dockerBuildName = "docker-${jobName}"
            server = Artifactory.server "local-artifactory"
            rtFullUrl = server.url
            rtIpAddress = rtFullUrl - ~/^http?.:\/\// - ~/\/artifactory$/
            pipelineUtils = load 'pipelineUtils.groovy'
        }

        stage("Build+Deploy") {
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
            docker.build(dockerImageTag, "--build-arg DOCKER_REGISTRY_URL=${rtIpAddress} .")
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

            server.promote dockerPromotionConfig
        }

        stage("Create release bundle") {

            rtServiceId = pipelineUtils.restGet("${rtFullUrl}/api/system/service_id", artifactoryCredentialId)


            def aqlQuery = """
            items.find({
                \"\$and\": [
                        {
                            \"repo\": {
                            \"\$match\": \"stable-*-repo\"
                        }
                        },
                        {
                            \"\$or\": [
                                {
                                    \"@build.name\": \"${mavenBuildName}\"
                                },
                                {
                                    \"@build.name\": \"${dockerBuildName}\"
                                }
                        ]
                        },
                        {
                            \"@build.number\": \"${buildNumber}\"
                        }
                ]
            })
            """.replaceAll(" ", "").replaceAll("\n", "")

            def releaseBundleBody = [
                    'name': "${releaseBundleName}",
                    'version': "${buildNumber}",
                    'dry_run': false,
                    'sign_immediately': true,
                    'description': 'Release bundle for the example java-project',
                    'spec': [
                            'source_artifactory_id': "${rtServiceId}",
                            'queries': [
                                    [
                                            'aql': "${aqlQuery}",
                                            'query_name': 'java-project-query'
                                    ]
                            ]
                    ]
            ]

            releaseBundleBodyJson = JsonOutput.toJson(releaseBundleBody)


            res = pipelineUtils.restPost("${distributionUrl}/api/v1/release_bundle", artifactoryCredentialId, releaseBundleBodyJson)
        }

        stage('Distribute release bundle') {
            def distributeReleaseBundleBody = readJSON file: 'distribute-release-bundle-body.json'
            res = pipelineUtils.restPost("${distributionUrl}/api/v1/distribution/${releaseBundleName}/${buildNumber}", artifactoryCredentialId, distributeReleaseBundleBody.toString())

            for (i = 0; true; i++) {
                res = pipelineUtils.restGet("${distributionUrl}/api/v1/release_bundle/${releaseBundleName}/${buildNumber}/distribution", artifactoryCredentialId)

                def jsonResult = readJSON text: res
                def distributionStatus = jsonResult.status.unique()
                distributionStatus = distributionStatus.collect { it.toUpperCase() }
                println "Current status:  ${distributionStatus}"

                if (distributionStatus == ['COMPLETED']) {
                    println "Distribution finished successfully"
                    break
                } else if (distributionStatus.contains('FAILED')) {
                    error("Distribution failed. Response body: ${jsonResult}")
                } else if (i >= 30) {
                    error("Timed out waiting for distribution to complete")
                }

                sleep 2

            }
        }
    }
}