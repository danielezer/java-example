node('generic') {

    def server
    def rtIpAddress

    stage("checkout") {
        checkout scm
    }

    stage("Build+Deploy") {
        server = Artifactory.server "local-artifactory"
        rtFullUrl = server.url
        rtIpAddress = rtFullUrl - ~/^http?.:\/\// - ~ /\/artifactory$/
        
        def rtMaven = Artifactory.newMavenBuild()
        rtMaven.deployer server: server, releaseRepo: 'libs-snapshot-local', snapshotRepo: 'libs-snapshot-local'
        rtMaven.tool = 'maven-3.5.3'
        String mvnGoals = "-B clean install -DartifactVersion=${env.BUILD_NUMBER} -s settings.xml"
        def buildInfo = Artifactory.newBuildInfo()
        buildInfo.name = "java-${env.JOB_NAME}"
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
                'buildName'          : buildInfo.name,
                'buildNumber'        : buildInfo.number,
                'targetRepo'         : 'stable-maven-repo',
                'comment'            : 'This is a stable java-project version',
                'status'             : 'Released',
                'sourceRepo'         : 'libs-snapshot-local',
                'copy'               : true,
                'failFast'           : true
        ]

        server.promote promotionConfig

    }

    stage("Build docker image") {
        def dockerBuildInfo = Artifactory.newBuildInfo()
        dockerBuildInfo.name = "docker-${env.JOB_NAME}"
        def downloadSpec = """{
             "files": [
              {
                  "pattern": "libs-snapshot-local/com/mkyong/hashing/java-project/${env.BUILD_NUMBER}-SNAPSHOT/java-project-*.jar",
                  "target": "target/downloads/",
                  "flat": "true"
                }
             ]
            }"""

        server.download spec: downloadSpec, buildInfo: dockerBuildInfo
        def rtDocker = Artifactory.docker server: server
        def dockerImageTag = "${rtIpAddress}/docker-java:${env.BUILD_NUMBER}"
        docker.build(dockerImageTag)
        dockerBuildInfo.env.collect()
        rtDocker.push(dockerImageTag, 'docker-repo', dockerBuildInfo)
        server.publishBuildInfo dockerBuildInfo
        def dockerScanConfig = [
                'buildName'      : dockerBuildInfo.name,
                'buildNumber'    : dockerBuildInfo.number,
                'failBuild'      : true
        ]
        server.xrayScan dockerScanConfig

        def dockerPromotionConfig = [
                'buildName'          : dockerBuildInfo.name,
                'buildNumber'        : dockerBuildInfo.number,
                'targetRepo'         : 'stable-docker-repo',
                'comment'            : 'This is a stable java-project docker image',
                'status'             : 'Released',
                'sourceRepo'         : 'docker-repo',
                'copy'               : true,
                'failFast'           : true
        ]

        Artifactory.addInteractivePromotion server: server, promotionConfig: dockerPromotionConfig, displayName: "Promote docker image to stable repo"

    }
}