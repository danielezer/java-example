node {
    stage("checkout") {
        checkout scm
    }

    stage("Build+Deploy") {
        def server = Artifactory.server "local-artifactory"
        def rtMaven = Artifactory.newMavenBuild()
        rtMaven.deployer server: server, releaseRepo: 'local-repo', snapshotRepo: 'local-repo'
        rtMaven.tool = 'maven-3.5.3'
        String mvnGoals = "clean install -DartifactVersion=${env.BUILD_NUMBER} -s settings.xml"
        def buildInfo = rtMaven.run pom: 'pom.xml', goals: mvnGoals
        server.publishBuildInfo buildInfo
        def scanConfig = [
                'buildName'      : buildInfo.name,
                'buildNumber'    : buildInfo.number
        ]
        def scanResult = server.xrayScan scanConfig
        echo scanResult as String

    }
}