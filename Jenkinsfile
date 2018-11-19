node {
    stage("checkout") {
        checkout scm
    }

    stage("Build") {
        sh '"./mvnw -B -s settings.xml clean install"'
    }
}