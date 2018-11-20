node {
    stage("checkout") {
        checkout scm
    }

    stage("Build+Deploy") {
        def server = Artifactory.server "local-artifactory"
        def rtMaven = Artifactory.newMavenBuild()
        rtMaven.tool = 'maven-3.5.3'
        rtMaven.deployer.deployArtifacts = true
        String mvnGoals = "clean install -s settings.xml -DartifactVersion=${env.BUILD_NUMBER}"
        def buildInfo = rtMaven.run pom: 'pom.xml', goals: mvnGoals
        server.publishBuildInfo buildInfo
        rtMaven.deployer.deployArtifacts buildInfo
    }
}