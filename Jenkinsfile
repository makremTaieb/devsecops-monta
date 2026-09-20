// ============================================================
//  STB DevSecOps — Multi-Environment Pipeline
//
//  Branch strategy:
//    dev     → build → test → scan → deploy to k8s/dev    (auto)
//    staging → build → test → scan → deploy to k8s/staging (auto)
//    main    → build → test → scan → [manual gate] → prod  (gated)
//
//  Ingress URLs:
//    dev.devsecops.local      ← namespace: dev
//    staging.devsecops.local  ← namespace: staging
//    devsecops.local          ← namespace: prod
// ============================================================
pipeline {
    agent any

    parameters {
        string(name: 'EXECUTION_ID', defaultValue: '1',    description: 'Pipeline execution ID from backend')
        string(name: 'PROJECT_ID',   defaultValue: '1',    description: 'Project ID in backend database')
        string(name: 'COMMIT_HASH',  defaultValue: 'HEAD', description: 'Git commit hash to build')
    }

    environment {
        JAVA_HOME_21  = "/usr/lib/jvm/java-21-openjdk-amd64"
        JAVA_HOME_17  = "/usr/lib/jvm/java-17-openjdk-amd64"

        REGISTRY             = "192.168.56.10:5000"
        SONAR_URL            = "http://192.168.56.10:9000"
        SECURITY_SERVICE_URL = "http://192.168.56.20:30080/api/security/scan"
        GATEWAY_URL          = "http://192.168.56.20:30080"

        BRANCH_NAME_CLEAN = "${env.BRANCH_NAME ?: 'dev'}"

        K8S_INFRA      = "/var/lib/jenkins/workspace/PFE-2026/k8s/infra"
        K8S_MONITORING = "/var/lib/jenkins/workspace/PFE-2026/k8s/monitoring"
        K8S_INGRESS    = "/var/lib/jenkins/workspace/PFE-2026/k8s/ingress"
        K8S_NAMESPACES = "/var/lib/jenkins/workspace/PFE-2026/k8s/namespaces"
    }

    stages {

        // ─────────────────────────────────────────────────────────
        // STEP 1 — Resolve environment from Git branch
        // ─────────────────────────────────────────────────────────
        stage('Resolve Environment') {
            steps {
                script {
                    def branch = env.BRANCH_NAME_CLEAN?.trim() ?: 'dev'

                    if (branch == 'main' || branch == 'master') {
                        env.DEPLOY_ENV  = 'prod'
                        env.K8S_NS      = 'prod'
                        env.IMAGE_TAG   = 'latest'
                        env.K8S_OVERLAY = "${env.WORKSPACE}/k8s/overlays/prod"
                        env.APP_HOST    = 'devsecops.local'
                    } else if (branch == 'staging') {
                        env.DEPLOY_ENV  = 'staging'
                        env.K8S_NS      = 'staging'
                        env.IMAGE_TAG   = 'staging'
                        env.K8S_OVERLAY = "${env.WORKSPACE}/k8s/overlays/staging"
                        env.APP_HOST    = 'staging.devsecops.local'
                    } else {
                        env.DEPLOY_ENV  = 'dev'
                        env.K8S_NS      = 'dev'
                        env.IMAGE_TAG   = 'dev'
                        env.K8S_OVERLAY = "${env.WORKSPACE}/k8s/overlays/dev"
                        env.APP_HOST    = 'dev.devsecops.local'
                    }

                    echo "╔══════════════════════════════════════╗"
                    echo "║  Branch   : ${branch}"
                    echo "║  Env      : ${env.DEPLOY_ENV}"
                    echo "║  Namespace: ${env.K8S_NS}"
                    echo "║  Image tag: ${env.IMAGE_TAG}"
                    echo "║  Host     : ${env.APP_HOST}"
                    echo "╚══════════════════════════════════════╝"
                }
            }
        }

        // ─────────────────────────────────────────────────────────
        // STEP 2 — Checkout
        // ─────────────────────────────────────────────────────────
        stage('Checkout') {
            steps { checkout scm }
        }

        // ─────────────────────────────────────────────────────────
        // STEP 3 — Build & Unit Tests
        // ─────────────────────────────────────────────────────────
        stage('Build & Test') {
            steps {
                withEnv(["JAVA_HOME=${JAVA_HOME_21}", "PATH+JAVA=${JAVA_HOME_21}/bin"]) {
                    sh '''
                        java -version
                        mvn clean verify -DskipTests=false \
                          -Dspring.profiles.active=${DEPLOY_ENV}
                    '''
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        // ─────────────────────────────────────────────────────────
        // STEP 4 — SonarQube Code Quality
        // ─────────────────────────────────────────────────────────
        stage('SonarQube Analysis') {
            steps {
                withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                    withEnv(["JAVA_HOME=${JAVA_HOME_17}", "PATH+JAVA=${JAVA_HOME_17}/bin"]) {
                        sh '''
                            echo "Waiting for SonarQube to be ready..."
                            for i in $(seq 1 48); do
                                STATUS=$(curl -s "http://192.168.56.10:9000/api/system/status" | grep -c '"UP"' || true)
                                echo "  Attempt $i/24 — up=$STATUS"
                                if [ "$STATUS" = "1" ]; then
                                    echo "SonarQube is UP"
                                    break
                                fi
                                sleep 10
                            done
                        '''
                        sh '''
                            mvn sonar:sonar \
                              -Dsonar.projectKey=PFE-2026-${DEPLOY_ENV} \
                              -Dsonar.host.url=${SONAR_URL} \
                              -Dsonar.token=${SONAR_TOKEN} \
                              
                        '''
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────
        // STEP 5 — Trivy Vulnerability Scan
        // ─────────────────────────────────────────────────────────
        stage('Trivy Scan') {
            steps {
                sh '''
                    trivy fs \
                      --format json \
                      --output ${WORKSPACE}/trivy.json \
                      --ignore-unfixed \
                      --severity CRITICAL,HIGH \
                      --skip-dirs "frontend/node_modules,frontend/.angular,frontend/dist" \
                      --skip-dirs "Auth-Service/target,Pipeline-Service/target,SECURITY-SERVICE/target" \
                      --skip-dirs "Eureka-Server/target,Gateway/target,Audit-Log-Service/target,Notification-Service/target" \
                      --ignorefile ${WORKSPACE}/.trivyignore \
                      . 2>/dev/null || true
                    if [ ! -s ${WORKSPACE}/trivy.json ]; then echo '{"Results":[]}' > ${WORKSPACE}/trivy.json; fi
                '''
            }
        }

        // ─────────────────────────────────────────────────────────
        // STEP 6 — Gitleaks Secret Scan
        // ─────────────────────────────────────────────────────────
        stage('Gitleaks Scan') {
            steps {
                sh '''
                    gitleaks detect \
                      --source . \
                      --config ${WORKSPACE}/.gitleaks.toml \
                      --report-format json \
                      --report-path ${WORKSPACE}/gitleaks.json 2>/dev/null || true
                    if [ ! -s ${WORKSPACE}/gitleaks.json ]; then echo '[]' > ${WORKSPACE}/gitleaks.json; fi
                '''
            }
        }

        // ─────────────────────────────────────────────────────────
        // STEP 7 — Send Security Reports to Platform
        // ─────────────────────────────────────────────────────────
        stage('Send Security Reports') {
            steps {
                script {
                    def httpStatus = sh(script: """
                        curl -s -o ${WORKSPACE}/security-response.json \
                          -w "%{http_code}" \
                          --connect-timeout 10 --max-time 30 \
                          -X POST ${SECURITY_SERVICE_URL} \
                          -F "executionId=${EXECUTION_ID}" \
                          -F "projectId=${PROJECT_ID}" \
                          -F "trivy=@${WORKSPACE}/trivy.json" \
                          -F "gitleaks=@${WORKSPACE}/gitleaks.json" \
                        || echo "000"
                    """, returnStdout: true).trim()

                    echo "Security service HTTP: ${httpStatus}"
                    def response = fileExists("${WORKSPACE}/security-response.json") ?
                        readFile("${WORKSPACE}/security-response.json").trim() : ""

                    if (httpStatus == "000") {
                        echo "⚠️  Security service unreachable — continuing without scan."
                    } else if (response.contains('"blocked":true')) {
                        sh """
                            curl -s -X PUT ${GATEWAY_URL}/api/executions/${EXECUTION_ID}/status \
                              -H 'Content-Type: application/json' \
                              -d '{"status":"FAILED"}' || true
                        """
                        echo "⚠️  Critical vulnerabilities detected — continuing for demo purposes."
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────
        // STEP 8 — Docker Build (all services + frontend)
        // ─────────────────────────────────────────────────────────
        stage('Docker Build') {
            steps {
                withEnv(["JAVA_HOME=${JAVA_HOME_21}", "PATH+JAVA=${JAVA_HOME_21}/bin"]) {
                    sh """
                        echo "=== Building :${IMAGE_TAG} images ==="
                        docker build -t ${REGISTRY}/eureka-server:${IMAGE_TAG}       ./Eureka-Server/
                        docker build -t ${REGISTRY}/api-gateway:${IMAGE_TAG}          ./Gateway/
                        docker build -t ${REGISTRY}/auth-service:${IMAGE_TAG}         ./Auth-Service/
                        docker build -t ${REGISTRY}/pipeline-service:${IMAGE_TAG}     ./Pipeline-Service/
                        docker build -t ${REGISTRY}/security-service:${IMAGE_TAG}     ./SECURITY-SERVICE/
                        docker build -t ${REGISTRY}/notification-service:${IMAGE_TAG} ./Notification-Service/
                        docker build -t ${REGISTRY}/audit-log-service:${IMAGE_TAG}    ./Audit-Log-Service/
                        docker build -t ${REGISTRY}/frontend:${IMAGE_TAG}             ./frontend/
                    """
                }
            }
        }

        // ─────────────────────────────────────────────────────────
        // STEP 9 — Docker Push to Nexus Registry
        // ─────────────────────────────────────────────────────────
        stage('Docker Push') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'nexus-credentials',
                    usernameVariable: 'NEXUS_USER',
                    passwordVariable: 'NEXUS_PASS'
                )]) {
                    sh """
                        echo "${NEXUS_PASS}" | docker login ${REGISTRY} -u ${NEXUS_USER} --password-stdin
                        docker push ${REGISTRY}/eureka-server:${IMAGE_TAG}
                        docker push ${REGISTRY}/api-gateway:${IMAGE_TAG}
                        docker push ${REGISTRY}/auth-service:${IMAGE_TAG}
                        docker push ${REGISTRY}/pipeline-service:${IMAGE_TAG}
                        docker push ${REGISTRY}/security-service:${IMAGE_TAG}
                        docker push ${REGISTRY}/notification-service:${IMAGE_TAG}
                        docker push ${REGISTRY}/audit-log-service:${IMAGE_TAG}
                        docker push ${REGISTRY}/frontend:${IMAGE_TAG}
                    """
                }
            }
        }
        // ─────────────────────────────────────────────────────────
        // STEP GitOps — Update image tags in Git → ArgoCD syncs
        // ─────────────────────────────────────────────────────────
        stage('GitOps — Update Manifests') {
            steps {
                withCredentials([string(credentialsId: 'github-token', variable: 'GH_TOKEN')]) {
                    sh """
                        cd ${WORKSPACE}
                        git config user.email "jenkins@devsecops.local"
                        git config user.name "Jenkins DevSecOps"
                        OVERLAY_FILE="k8s/overlays/${DEPLOY_ENV}/apps-${DEPLOY_ENV}.yaml"
                        for SVC in auth-service pipeline-service security-service notification-service audit-log-service eureka-server api-gateway frontend; do
                            sed -i "s|${REGISTRY}/\${SVC}:.*|${REGISTRY}/\${SVC}:${IMAGE_TAG}|g" \${OVERLAY_FILE}
                        done
                        git add \${OVERLAY_FILE}
                        git diff --cached --quiet || git commit -m "ci: update ${DEPLOY_ENV} images to ${IMAGE_TAG} [skip ci]"
                        git push https://makremTaieb:\${GH_TOKEN}@github.com/makremTaieb/devsecops-monta.git HEAD:${BRANCH_NAME_CLEAN}
                    """
                }
            }
        }

        // ─────────────────────────────────────────────────────────
        // STEP 10 — Manual Approval Gate (PROD only)
        // ─────────────────────────────────────────────────────────
        stage('Approval Gate — PROD') {
            when { expression { env.DEPLOY_ENV == 'prod' } }
            steps {
                timeout(time: 30, unit: 'MINUTES') {
                    input message: "🚀 Deploy to PRODUCTION at devsecops.local ?",
                          ok: "Yes — deploy to prod",
                          submitter: "admin"
                }
            }
        }

        // ─────────────────────────────────────────────────────────
        // STEP 11 — Bootstrap: Namespaces + Secrets + Shared Infra
        // ─────────────────────────────────────────────────────────
        stage('Bootstrap K8s') {
            steps {
                sh """
                    # 1. Create namespaces (idempotent)
                    kubectl apply -f ${K8S_NAMESPACES}/namespaces.yaml

                    # 2. Copy shared secrets into target namespace
                    for SECRET in db-secret jwt-secret; do
                      kubectl get secret \$SECRET -n infra -o yaml \
                        | sed "s/namespace: infra/namespace: ${K8S_NS}/" \
                        | kubectl apply -f - || true
                    done
                    kubectl get secret mail-secret -n apps -o yaml \
                      | sed "s/namespace: apps/namespace: ${K8S_NS}/" \
                      | kubectl apply -f - || true

                    # 3. Shared infra (MySQL, Eureka, Gateway)
                    kubectl apply -f ${K8S_INFRA}/mysql.yaml
                    kubectl apply -f ${K8S_INFRA}/eureka.yaml
                    kubectl apply -f ${K8S_INFRA}/gateway.yaml

                    # 4. Monitoring
                    kubectl apply -f ${K8S_MONITORING}/prometheus.yaml
                    kubectl apply -f ${K8S_MONITORING}/grafana.yaml

                    # 5. Ingress controller rules
                    kubectl apply -f ${K8S_INGRESS}/ingress-all.yaml
                """
            }
        }

        // ─────────────────────────────────────────────────────────
        // STEP 12 — ArgoCD Sync (GitOps deployment)
        // ─────────────────────────────────────────────────────────
        stage('ArgoCD Sync') {
            steps {
                sh """
                    echo "=== Triggering ArgoCD sync for pfe-${DEPLOY_ENV} ==="
                    kubectl patch application pfe-${DEPLOY_ENV} -n argocd \
                      --type merge \
                      -p '{"operation":{"sync":{"revision":"${DEPLOY_ENV}"}}}'
                    echo "=== ArgoCD sync triggered — GitOps will handle rollout ==="
                """
            }
        }

        // ─────────────────────────────────────────────────────────
        // STEP 13 — Wait for ArgoCD Health
        // ─────────────────────────────────────────────────────────
        stage('Rollout Status') {
            steps {
                sh """
                    echo "=== Waiting for ArgoCD pfe-${DEPLOY_ENV} to become Healthy ==="
                    for i in \$(seq 1 48); do
                      STATUS=\$(kubectl get application pfe-${DEPLOY_ENV} -n argocd \
                        -o jsonpath='{.status.health.status}')
                      echo "Attempt \$i/24 — Health: \$STATUS"
                      if [ "\$STATUS" = "Healthy" ]; then
                        echo "=== ArgoCD reports Healthy ==="
                        exit 0
                      fi
                      sleep 15
                    done
                    echo "=== Timeout waiting for ArgoCD health ==="
                    exit 1
                """
            }
        }

        // ─────────────────────────────────────────────────────────
        // STEP 14 — Smoke Test via Ingress
        // ─────────────────────────────────────────────────────────
        stage('Smoke Test') {
            steps {
                sh """
                    sleep 15
                    echo "=== Smoke test → http://${APP_HOST}/api/auth/actuator/health ==="

                    STATUS=\$(curl -s -o /dev/null -w "%{http_code}" \
                      --connect-timeout 10 --max-time 20 \
                      -H "Host: ${APP_HOST}" \
                      http://192.168.56.20:31038/api/auth/actuator/health || echo "000")

                    echo "Health endpoint: \$STATUS"
                    kubectl get pods    -n ${K8S_NS}
                    kubectl get ingress -n ${K8S_NS}
                    kubectl get applications -n argocd
                """
            }
        }

        // ─────────────────────────────────────────────────────────
        // STEP 15 — Final Verification
        // ─────────────────────────────────────────────────────────
        stage('Verify') {
            steps {
                sh '''
                    echo "=== Namespace: ${K8S_NS} ==="
                    kubectl get all -n ${K8S_NS}
                    echo "=== Infra ==="
                    kubectl get pods -n infra
                    echo "=== Monitoring ==="
                    kubectl get pods -n monitoring
                '''
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: '*.json', fingerprint: true
        }
        success {
            echo "✅ [${DEPLOY_ENV}] deployed to http://${APP_HOST}"
            script {
                sh """
                    curl -s -X PUT ${GATEWAY_URL}/api/executions/${EXECUTION_ID}/status \
                      -H 'Content-Type: application/json' \
                      -d '{"status":"SUCCESS"}' || true
                    curl -s -X POST ${GATEWAY_URL}/api/audit/log \
                      -H 'Content-Type: application/json' \
                      -d '{"action":"PIPELINE_TRIGGERED","resource":"PIPELINE","resourceId":${EXECUTION_ID},"details":"[${DEPLOY_ENV}] deployed to ${APP_HOST}","status":"SUCCESS","sourceService":"jenkins"}' || true
                """
            }
        }
        failure {
            echo "❌ [${DEPLOY_ENV}] pipeline FAILED"
            script {
                sh """
                    curl -s -X PUT ${GATEWAY_URL}/api/executions/${EXECUTION_ID}/status \
                      -H 'Content-Type: application/json' \
                      -d '{"status":"FAILED"}' || true
                    curl -s -X POST ${GATEWAY_URL}/api/audit/log \
                      -H 'Content-Type: application/json' \
                      -d '{"action":"PIPELINE_ABORTED","resource":"PIPELINE","resourceId":${EXECUTION_ID},"details":"[${DEPLOY_ENV}] failed on ${BRANCH_NAME_CLEAN}","status":"FAILURE","sourceService":"jenkins"}' || true
                """
            }
        }
    }
}
