node('generic') {
    stage("checkout") {
        checkout scm
    }

    stage("Build") {
        sh "mvn -B -s settings.xml clean install"
    }
}