// Jenkinsfile = pipeline-as-code. This file LIVES IN THE REPO, so the CI job
// definition is version-controlled with the tests themselves. When this repo is
// scanned by Jenkins, it automatically creates/updates a pipeline job.

pipeline {

    // Run on any available Jenkins agent that has Maven + JDK 11 configured.
    agent any

    // environment block = variables available to every stage. Keeping versions
    // here means an Allure upgrade happens in ONE line.
    environment {
        // Allure command line tool version used to generate the HTML report.
        ALLURE_VERSION = '2.25.0'
    }

    tools {
        // Names must match the tool names configured in Jenkins > Manage Jenkins > Tools.
        maven 'Maven'
        jdk 'JDK11'
    }

    stages {

        stage('Checkout') {
            steps {
                // Pull the exact commit that triggered the build (git push, PR, etc).
                checkout scm
            }
        }

        stage('Build & Run API Tests') {
            steps {
                // "mvn clean test" = delete old target folder, compile, run testng.xml.
                // The suite runs all 3 test classes: smoke, CRUD, and end-to-end.
                sh 'mvn clean test'
            }
        }

        stage('Generate Allure Report') {
            steps {
                // allure commandline tool reads the raw results produced in
                // target/allure-results during the test run and builds the HTML report
                // into target/site/allure-report, ready to be published/archived.
                sh "${tool 'Allure'} allure generate target/allure-results -o target/site/allure-report --clean"
            }
        }
    }

    post {
        always {
            // "always" means this runs whether the build PASSED or FAILED.
            // Publishing the Allure report on failure is crucial — a failed build
            // without a report forces someone to rerun locally just to see why.

            // Show the Allure report as a build sidebar link and dashboard graph.
            // Requires the "Allure Jenkins Plugin" to be installed in Jenkins.
            allure([
                includeProperties: false,
                jdk: '',
                properties: [],
                reportBuildPolicy: 'ALWAYS',
                results: [[path: 'target/allure-results']]
            ])

            // Also archive the raw results as build artifacts for the audit trail.
            archiveArtifacts artifacts: 'target/allure-results/**', allowEmptyArchive: true
        }
    }
}
