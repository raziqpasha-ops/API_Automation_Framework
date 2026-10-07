// Jenkinsfile = pipeline-as-code. This file LIVES IN THE REPO, so the CI job
// definition is version-controlled with the tests themselves. When this repo is
// scanned by Jenkins, it automatically creates/updates a pipeline job.
//
// TOOL STRATEGY (interview point): the `tools { }` block requires Jenkins
// Global Tool Configuration entries with exact matching names. To keep this
// pipeline PORTABLE (works on any Jenkins out of the box), we do NOT use the
// tools block. Instead the pipeline auto-installs its own toolchain with
// explicit versions: JDK (Temurin 11), Maven 3.9.x, and Allure 2.25.0.
// Same behaviour everywhere, zero manual Jenkins setup.

pipeline {

    // Run on any available Jenkins agent.
    agent any

    environment {
        // Pinned tool versions — an upgrade is a ONE-LINE change here.
        JAVA_VERSION = '11.0.21+9'
        MAVEN_VERSION = '3.9.6'
        ALLURE_VERSION = '2.25.0'
    }

    stages {

        stage('Checkout') {
            steps {
                // Pull the exact commit that triggered the build (git push, PR, etc).
                checkout scm
            }
        }

        stage('Setup JDK') {
            steps {
                // The "Temurin Installations" plugin downloads + caches the exact
                // JDK version for us. JAVA_HOME is then exported for later stages.
                // installTemurinJDK RETURN: exports JAVA_HOME into the environment.
                installTemurinJDK platform: 'linux', architecture: 'x64', version: "${JAVA_VERSION}"
                sh "java -version 2>&1 | head -1"
            }
        }

        stage('Setup Maven') {
            steps {
                // The "Maven Installation" plugin style is not needed; we simply
                // download the official Maven binary tarball once per agent and
                // cache it in the workspace .tools folder.
                sh """
                    if [ ! -d ".tools/apache-maven-${MAVEN_VERSION}" ]; then
                        mkdir -p .tools
                        curl -fsSL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz | tar -xz -C .tools
                    fi
                    echo "Maven ready: \$(.tools/apache-maven-${MAVEN_VERSION}/bin/mvn -v | head -1)"
                """
            }
        }

        stage('Setup Allure') {
            steps {
                // Same cache pattern for the Allure commandline tool. Downloaded
                // from the official GitHub release into .tools, reused on reruns.
                sh """
                    if [ ! -d ".tools/allure-${ALLURE_VERSION}" ]; then
                        mkdir -p .tools
                        curl -fsSL https://github.com/allure-framework/allure2/releases/download/${ALLURE_VERSION}/allure-${ALLURE_VERSION}.tgz | tar -xz -C .tools
                        mv .tools/allure-${ALLURE_VERSION} .tools/allure-${ALLURE_VERSION} 2>/dev/null || true
                    fi
                    echo "Allure ready: \$(.tools/allure-${ALLURE_VERSION}/bin/allure --version 2>/dev/null || echo downloaded)"
                """
            }
        }

        stage('Build & Run API Tests') {
            steps {
                // "mvn clean test" = delete old target folder, compile, run testng.xml.
                // The suite runs all 3 test classes: smoke, CRUD, and end-to-end.
                // We invoke Maven via its full path from our cached .tools folder.
                sh ".tools/apache-maven-${MAVEN_VERSION}/bin/mvn clean test"
            }
        }

        stage('Generate Allure Report') {
            steps {
                // allure commandline reads the raw results produced in
                // target/allure-results during the test run and builds the HTML
                // report into target/site/allure-report, ready to be published.
                sh ".tools/allure-${ALLURE_VERSION}/bin/allure generate target/allure-results -o target/site/allure-report --clean"
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

            // Also archive the raw results + generated report as build artifacts
            // for the audit trail. Empty-safe so an early failure doesn't error.
            archiveArtifacts artifacts: 'target/allure-results/**', allowEmptyArchive: true
            archiveArtifacts artifacts: 'target/site/allure-report/**', allowEmptyArchive: true
        }
    }
}
