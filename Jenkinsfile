// Jenkinsfile = pipeline-as-code. This file LIVES IN THE REPO, so the CI job
// definition is version-controlled with the tests themselves.
//
// TOOL STRATEGY (interview point): the pipeline is SELF-PROVISIONING — it
// downloads and caches its own JDK, Maven and Allure into .tools/ using plain
// curl. No Jenkins Global Tools configuration and no extra plugins required,
// so the same pipeline runs on any Jenkins out of the box.

pipeline {

    // Run on any available Jenkins agent.
    agent any

    environment {
        // Pinned tool versions — an upgrade is a ONE-LINE change here.
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
                // Download Temurin JDK 11 from the Adoptium API (platform is
                // resolved automatically for mac/linux, x64/aarch64) and cache
                // it in .tools/jdk. Reused on every rerun of the same agent.
                sh """
                    if [ ! -d ".tools/jdk" ]; then
                        mkdir -p .tools
                        ARCH=\$(case \$(uname -m) in arm64|aarch64) echo aarch64 ;; *) echo x64 ;; esac)
                        OS=\$(uname | tr '[:upper:]' '[:lower:]')
                        curl -fsSL "https://api.adoptium.net/v3/binary/latest/11/ga/\${OS}/\${ARCH}/jdk/hotspot/normal/eclipse" -o /tmp/jdk.tgz
                        mkdir -p .tools/jdk
                        tar -xzf /tmp/jdk.tgz -C .tools/jdk --strip-components=1
                        rm -f /tmp/jdk.tgz
                    fi
                    \$PWD/.tools/jdk/bin/java -version 2>&1 | head -1
                """
            }
        }

        stage('Setup Maven') {
            steps {
                // Download the official Maven binary tarball once per agent and
                // cache it in the workspace .tools folder for reuse.
                sh """
                    if [ ! -d ".tools/apache-maven-${MAVEN_VERSION}" ]; then
                        mkdir -p .tools
                        curl -fsSL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz | tar -xz -C .tools
                    fi
                    echo "Maven ready"
                """
            }
        }

        stage('Setup Allure') {
            steps {
                // Same cache pattern for the Allure commandline tool. The tgz
                // extracts a folder named allure-<version> which matches our path.
                sh """
                    if [ ! -d ".tools/allure-${ALLURE_VERSION}" ]; then
                        mkdir -p .tools
                        curl -fsSL https://github.com/allure-framework/allure2/releases/download/${ALLURE_VERSION}/allure-${ALLURE_VERSION}.tgz | tar -xz -C .tools
                    fi
                    echo "Allure ready"
                """
            }
        }

        stage('Build & Run API Tests') {
            steps {
                // Export the downloaded JDK for THIS stage, then run the suite:
                // "mvn clean test" = clean old target, compile, run testng.xml.
                // The suite runs all 3 test classes: smoke, CRUD, end-to-end.
                withEnv(["JAVA_HOME=${env.WORKSPACE}/.tools/jdk",
                         "PATH+JDK=${env.WORKSPACE}/.tools/jdk/bin"]) {
                    sh ".tools/apache-maven-${MAVEN_VERSION}/bin/mvn clean test"
                }
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

            // Show the Allure report as a build sidebar link and trend graph.
            // Requires the "Allure Jenkins Plugin" to be installed in Jenkins.
            allure([
                includeProperties: false,
                jdk: '',
                properties: [],
                reportBuildPolicy: 'ALWAYS',
                results: [[path: 'target/allure-results']]
            ])

            // Also archive raw results + generated HTML report as build
            // artifacts for the audit trail. Empty-safe on early failure.
            archiveArtifacts artifacts: 'target/allure-results/**', allowEmptyArchive: true
            archiveArtifacts artifacts: 'target/site/allure-report/**', allowEmptyArchive: true
        }
    }
}
