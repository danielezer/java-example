node {
    stage("checkout") {
        checkout scm
    }

    stage("Build+Deploy") {
        def server = Artifactory.newServer url: 'host.docker.internal', username: 'test', password: 'test'
        def rtMaven = Artifactory.newMavenBuild()
        rtMaven.resolver server: server, releaseRepo: 'all-repos', snapshotRepo: 'all-repos'
        rtMaven.deployer server: server, releaseRepo: 'local-repo', snapshotRepo: 'local-repo'
        env.MAVEN_HOME = '/usr/local/opt/maven@3.5'
        def buildInfo = rtMaven.run pom: 'maven-example/pom.xml', goals: 'clean install'
        server.publishBuildInfo buildInfo
    }
}