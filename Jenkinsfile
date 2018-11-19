node {
    stage("checkout") {
        checkout scm
    }

    stage("Build+Deploy") {
        def server = Artifactory.newServer url: 'http://host.docker.internal:8081/artifactory', username: 'test', password: 'test'
        def rtMaven = Artifactory.newMavenBuild()
        rtMaven.resolver server: server, releaseRepo: 'all-repos', snapshotRepo: 'all-repos'
        rtMaven.deployer server: server, releaseRepo: 'local-repo', snapshotRepo: 'local-repo'
        rtMaven.tool = 'maven-3.5.3'
        def buildInfo = rtMaven.run pom: 'pom.xml', goals: 'clean install'
        server.publishBuildInfo buildInfo
    }
}