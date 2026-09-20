FROM jenkins/jenkins:lts-jdk21

USER root

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl git jq maven docker.io ca-certificates \
    && rm -rf /var/lib/apt/lists/*

ARG TRIVY_VERSION=0.74.0
ARG GITLEAKS_VERSION=8.21.2
ARG DOCKER_CLI_VERSION=27.5.1
RUN curl -fsSL "https://github.com/aquasecurity/trivy/releases/download/v${TRIVY_VERSION}/trivy_${TRIVY_VERSION}_Linux-64bit.tar.gz" \
      | tar -xz -C /usr/local/bin trivy \
    && curl -fsSL "https://github.com/gitleaks/gitleaks/releases/download/v${GITLEAKS_VERSION}/gitleaks_${GITLEAKS_VERSION}_linux_x64.tar.gz" \
      | tar -xz -C /usr/local/bin gitleaks \
    && curl -fsSL "https://download.docker.com/linux/static/stable/x86_64/docker-${DOCKER_CLI_VERSION}.tgz" \
      | tar -xz -C /usr/local/bin --strip-components=1 docker/docker \
    && chmod +x /usr/local/bin/trivy /usr/local/bin/gitleaks

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

COPY casc/jenkins.yaml /usr/share/jenkins/ref/casc/jenkins.yaml
COPY init.groovy.d/ /usr/share/jenkins/ref/init.groovy.d/

ENV CASC_JENKINS_CONFIG=/var/jenkins_home/casc/jenkins.yaml
