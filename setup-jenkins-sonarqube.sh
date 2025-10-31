#!/bin/bash

##############################################################################
# Script de configuración del Pipeline SonarQube en Jenkins
# Proyecto: VG MS Users Rev
# Fecha: 31 de Octubre de 2025
##############################################################################

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuración
JENKINS_URL="http://localhost:8080"
JENKINS_USER="admin"
JENKINS_PASSWORD="admin"
JOB_NAME="vg-ms-users-sonarqube"
REPO_URL="https://github.com/IsaelFatamaDev/vg-ms-users-rev.git"
BRANCH="*/main"
JENKINSFILE_PATH="Jenkinsfile-SonarQube"

echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║     Configuración Pipeline SonarQube - VG MS Users Rev        ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Función para esperar a que Jenkins esté listo
wait_for_jenkins() {
    echo -e "${YELLOW}⏳ Esperando a que Jenkins esté disponible...${NC}"
    max_attempts=30
    attempt=0

    while [ $attempt -lt $max_attempts ]; do
        if curl -s -f "${JENKINS_URL}/login" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ Jenkins está disponible${NC}"
            return 0
        fi
        attempt=$((attempt + 1))
        echo -e "${YELLOW}   Intento $attempt/$max_attempts...${NC}"
        sleep 2
    done

    echo -e "${RED}❌ Jenkins no respondió después de $max_attempts intentos${NC}"
    return 1
}

# Esperar a Jenkins
wait_for_jenkins || exit 1

echo ""
echo -e "${BLUE}📋 Configuración del Pipeline:${NC}"
echo -e "   Nombre del Job: ${GREEN}${JOB_NAME}${NC}"
echo -e "   Repositorio: ${GREEN}${REPO_URL}${NC}"
echo -e "   Branch: ${GREEN}${BRANCH}${NC}"
echo -e "   Jenkinsfile: ${GREEN}${JENKINSFILE_PATH}${NC}"
echo ""

# Crear XML del job
cat > /tmp/sonarqube-job-config.xml << 'EOF'
<?xml version='1.1' encoding='UTF-8'?>
<flow-definition plugin="workflow-job@1436.vfa_244484591f">
  <actions/>
  <description>Pipeline de análisis de calidad de código con SonarQube para el microservicio VG MS Users</description>
  <keepDependencies>false</keepDependencies>
  <properties>
    <jenkins.model.BuildDiscarderProperty>
      <strategy class="hudson.tasks.LogRotator">
        <daysToKeep>30</daysToKeep>
        <numToKeep>10</numToKeep>
        <artifactDaysToKeep>-1</artifactDaysToKeep>
        <artifactNumToKeep>-1</artifactNumToKeep>
      </strategy>
    </jenkins.model.BuildDiscarderProperty>
    <org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty>
      <triggers>
        <hudson.triggers.SCMTrigger>
          <spec>H/5 * * * *</spec>
          <ignorePostCommitHooks>false</ignorePostCommitHooks>
        </hudson.triggers.SCMTrigger>
      </triggers>
    </org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty>
  </properties>
  <definition class="org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition" plugin="workflow-cps@3920.va_6b_e363b_1a_5d">
    <scm class="hudson.plugins.git.GitSCM" plugin="git@5.4.1">
      <configVersion>2</configVersion>
      <userRemoteConfigs>
        <hudson.plugins.git.UserRemoteConfig>
          <url>REPO_URL_PLACEHOLDER</url>
        </hudson.plugins.git.UserRemoteConfig>
      </userRemoteConfigs>
      <branches>
        <hudson.plugins.git.BranchSpec>
          <name>BRANCH_PLACEHOLDER</name>
        </hudson.plugins.git.BranchSpec>
      </branches>
      <doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>
      <submoduleCfg class="empty-list"/>
      <extensions/>
    </scm>
    <scriptPath>JENKINSFILE_PLACEHOLDER</scriptPath>
    <lightweight>true</lightweight>
  </definition>
  <triggers/>
  <disabled>false</disabled>
</flow-definition>
EOF

# Reemplazar placeholders
sed -i "s|REPO_URL_PLACEHOLDER|${REPO_URL}|g" /tmp/sonarqube-job-config.xml
sed -i "s|BRANCH_PLACEHOLDER|${BRANCH}|g" /tmp/sonarqube-job-config.xml
sed -i "s|JENKINSFILE_PLACEHOLDER|${JENKINSFILE_PATH}|g" /tmp/sonarqube-job-config.xml

echo -e "${YELLOW}🔧 Creando job en Jenkins...${NC}"

# Verificar si el job ya existe
if curl -s -u "${JENKINS_USER}:${JENKINS_PASSWORD}" \
    "${JENKINS_URL}/job/${JOB_NAME}/api/json" > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  El job '${JOB_NAME}' ya existe. Actualizando configuración...${NC}"

    # Actualizar job existente
    curl -X POST -u "${JENKINS_USER}:${JENKINS_PASSWORD}" \
        -H "Content-Type: application/xml" \
        --data-binary @/tmp/sonarqube-job-config.xml \
        "${JENKINS_URL}/job/${JOB_NAME}/config.xml"

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Job actualizado exitosamente${NC}"
    else
        echo -e "${RED}❌ Error al actualizar el job${NC}"
        exit 1
    fi
else
    # Crear nuevo job
    curl -X POST -u "${JENKINS_USER}:${JENKINS_PASSWORD}" \
        -H "Content-Type: application/xml" \
        --data-binary @/tmp/sonarqube-job-config.xml \
        "${JENKINS_URL}/createItem?name=${JOB_NAME}"

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Job creado exitosamente${NC}"
    else
        echo -e "${RED}❌ Error al crear el job${NC}"
        exit 1
    fi
fi

# Limpiar archivo temporal
rm /tmp/sonarqube-job-config.xml

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║              ✅ Configuración Completada Exitosamente          ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}📊 Información del Pipeline:${NC}"
echo -e "   ${GREEN}•${NC} Job Name: ${YELLOW}${JOB_NAME}${NC}"
echo -e "   ${GREEN}•${NC} Jenkins URL: ${YELLOW}${JENKINS_URL}/job/${JOB_NAME}/${NC}"
echo -e "   ${GREEN}•${NC} Jenkinsfile: ${YELLOW}${JENKINSFILE_PATH}${NC}"
echo ""
echo -e "${BLUE}🔗 Enlaces útiles:${NC}"
echo -e "   ${GREEN}•${NC} Pipeline: ${YELLOW}${JENKINS_URL}/job/${JOB_NAME}/${NC}"
echo -e "   ${GREEN}•${NC} SonarQube: ${YELLOW}http://localhost:9000${NC}"
echo -e "   ${GREEN}•${NC} Slack Channel: ${YELLOW}#jenkins-notifications${NC}"
echo ""
echo -e "${BLUE}📋 Próximos pasos:${NC}"
echo -e "   ${GREEN}1.${NC} Verifica que SonarQube esté configurado en Jenkins:"
echo -e "      ${YELLOW}${JENKINS_URL}/manage/configure${NC}"
echo -e "      Buscar: 'SonarQube servers' y configurar:"
echo -e "      - Name: ${GREEN}SonarQube-Server${NC}"
echo -e "      - Server URL: ${GREEN}http://sonarqube:9000${NC}"
echo -e "      - Server authentication token: ${GREEN}sonarqube-token${NC}"
echo ""
echo -e "   ${GREEN}2.${NC} Verifica las credenciales necesarias:"
echo -e "      ${YELLOW}${JENKINS_URL}/manage/credentials/${NC}"
echo -e "      - ${GREEN}sonarqube-token${NC} (Secret text)"
echo -e "      - ${GREEN}Slack-Tokencito${NC} (Secret text)"
echo ""
echo -e "   ${GREEN}3.${NC} Ejecuta el pipeline:"
echo -e "      ${YELLOW}${JENKINS_URL}/job/${JOB_NAME}/build${NC}"
echo ""
echo -e "${BLUE}🚀 Para ejecutar el pipeline ahora mismo:${NC}"
echo -e "   ${YELLOW}curl -X POST '${JENKINS_URL}/job/${JOB_NAME}/build' \\${NC}"
echo -e "   ${YELLOW}        --user '${JENKINS_USER}:${JENKINS_PASSWORD}'${NC}"
echo ""
echo -e "${GREEN}✨ ¡Listo para analizar código con SonarQube! ✨${NC}"
echo ""
