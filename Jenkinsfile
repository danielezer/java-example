node {
    stage("checkout") {
        checkout scm
    }

    stage("Build") {
        docker.image('maven:3.5.3').inside { c ->
            sh '"mvn -B -s settings.xml clean install"'
        }
    }
}