// This Jenkinsfile is the complete CI pipeline written as code.
// Every push to GitHub makes Jenkins: download the code, run the API test
// suite with Maven, and publish an Allure report — so bugs are caught
// within minutes of a commit.
//
// TOOL STRATEGY (simple and proven): instead of downloading tools inside the
// pipeline, we REUSE the tools already installed on the machine via Homebrew
// (mvn, allure, java). Jenkins starts its shell with a minimal PATH, so we
// prepend /opt/homebrew/bin — that single line makes every Homebrew tool
// available to all stages. This is a classic CI lesson: the CI machine's
// shell is NOT the same as your interactive terminal.

pipeline {

    // "agent any" = run on any available Jenkins machine.
    agent any

    // Environment variables for the whole pipeline.
    // The PATH line is the key fix: Jenkins' shell cannot see Homebrew tools
    // (mvn, allure) unless we add /opt/homebrew/bin to the front of PATH.
    environment {
        PATH = "/opt/homebrew/bin:${env.PATH}"
    }

    // Keep only the last 5 builds so Jenkins' disk does not fill up
    // with old logs and reports. Housekeeping as code, not a manual tweak.
    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }

    stages {

        // Stage 1: download the exact commit that triggered the build.
        // This guarantees traceability — every build result maps to one commit.
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        // Stage 2: run the API test suite.
        // "mvn clean test" = delete old output, compile fresh, run testng.xml
        // (all 11 tests: auth/negative, CRUD, end-to-end) via Surefire.
        // The Allure TestNG adapter writes raw JSON results into
        // target/allure-results during this stage.
        stage('Run Tests') {
            steps {
                sh 'mvn clean test'
            }
        }

        // Stage 3: convert the raw JSON results into the rich HTML Allure
        // report and attach it to the build page (via the Allure plugin).
        stage('Generate Allure Report') {
            steps {
                allure includeProperties: false,
                        jdk: '',
                        results: [[path: 'target/allure-results']]
            }
        }
    }

    // "post" actions run after the stages finish, whatever the outcome.
    post {

        // Always runs — success or failure.
        always {
            echo 'Pipeline finished — see the Allure report for full details'
        }

        // Runs only when the build failed; points the engineer to the report.
        failure {
            echo 'Build FAILED — check the Allure report and logs/framework.log'
        }
    }
}
