node('generic') {

    def server

    stage("checkout") {
        checkout scm
    }

    stage("Build+Deploy") {
        server = Artifactory.server "local-artifactory"
        def rtMaven = Artifactory.newMavenBuild()
        rtMaven.deployer server: server, releaseRepo: 'local-repo', snapshotRepo: 'local-repo'
        rtMaven.tool = 'maven-3.5.3'
        String mvnGoals = "-B clean install -DartifactVersion=${env.BUILD_NUMBER} -s settings.xml"
        def buildInfo = rtMaven.run pom: 'pom.xml', goals: mvnGoals
        buildInfo.env.collect()
        dockerBuildInfo.name = "java-${env.JOB_NAME}"
        buildInfo.number = env.BUILD_NUMBER
        server.publishBuildInfo buildInfo
        def scanConfig = [
                'buildName'      : buildInfo.name,
                'buildNumber'    : buildInfo.number,
                'failBuild'      : true
        ]
        def scanResult = server.xrayScan scanConfig
        echo scanResult as String

        stage("Build docker image") {
            def rtDocker = Artifactory.docker server: server
            def dockerBuildInfo = rtDocker.push "35.205.28.253/docker-java:${env.BUILD_NUMBER}", 'docker-repo'
            dockerBuildInfo.env.capture = true
            dockerBuildInfo.name = "docker-${env.JOB_NAME}"
            buildInfo.number = env.BUILD_NUMBER
            server.publishBuildInfo dockerBuildInfo
            def dockerScanConfig = [
                    'buildName'      : buildInfo.name,
                    'buildNumber'    : buildInfo.number,
                    'failBuild'      : true
            ]
            def dockerScanResult = server.xrayScan dockerScanConfig
            echo dockerScanResult as String
        }
    }
}