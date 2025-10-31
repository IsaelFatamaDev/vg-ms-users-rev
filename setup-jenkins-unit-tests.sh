#!/bin/bash

###############################################################################
# Script: setup-jenkins-unit-tests.sh
# Descripción: Configura automáticamente el pipeline de pruebas unitarias
#              en Jenkins
# Uso: ./setup-jenkins-unit-tests.sh
###############################################################################

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuración
JENKINS_URL="http://localhost:8080"
JOB_NAME="vg-ms-users-unit-tests"
JENKINS_USER="${JENKINS_USER:-admin}"
JENKINS_PASSWORD="${JENKINS_PASSWORD:-}"

echo -e "${BLUE}"
echo "╔════════════════════════════════════════════════════════════╗"
echo "║  CONFIGURADOR DE PIPELINE DE PRUEBAS UNITARIAS - JENKINS  ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# Función para mostrar mensajes
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Verificar que Jenkins está corriendo
log_info "Verificando que Jenkins está corriendo..."
if ! docker ps | grep -q jenkins; then
    log_error "Jenkins no está corriendo. Por favor ejecuta: docker-compose up -d"
    exit 1
fi
log_success "Jenkins está corriendo"

# Esperar a que Jenkins esté listo
log_info "Esperando a que Jenkins esté completamente disponible..."
MAX_ATTEMPTS=30
ATTEMPT=0
while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    if curl -s -f "${JENKINS_URL}" > /dev/null 2>&1; then
        log_success "Jenkins está disponible"
        break
    fi
    ATTEMPT=$((ATTEMPT + 1))
    echo -n "."
    sleep 2
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    log_error "Jenkins no respondió después de ${MAX_ATTEMPTS} intentos"
    exit 1
fi

# Obtener la contraseña inicial si no se proporcionó
if [ -z "$JENKINS_PASSWORD" ]; then
    log_info "Obteniendo contraseña inicial de Jenkins..."
    if docker exec jenkins test -f /var/jenkins_home/secrets/initialAdminPassword 2>/dev/null; then
        JENKINS_PASSWORD=$(docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword 2>/dev/null || echo "")
        if [ -n "$JENKINS_PASSWORD" ]; then
            log_success "Contraseña inicial obtenida"
            echo -e "${YELLOW}Contraseña inicial: ${JENKINS_PASSWORD}${NC}"
        else
            log_warning "No se pudo obtener la contraseña inicial automáticamente"
            read -sp "Por favor ingresa la contraseña de Jenkins: " JENKINS_PASSWORD
            echo
        fi
    else
        read -sp "Por favor ingresa la contraseña de Jenkins: " JENKINS_PASSWORD
        echo
    fi
fi

# Verificar credenciales
log_info "Verificando credenciales de Jenkins..."
AUTH_RESPONSE=$(curl -s -w "%{http_code}" -u "${JENKINS_USER}:${JENKINS_PASSWORD}" "${JENKINS_URL}/api/json" -o /dev/null)
if [ "$AUTH_RESPONSE" = "200" ]; then
    log_success "Credenciales válidas"
else
    log_error "Credenciales inválidas (HTTP ${AUTH_RESPONSE})"
    exit 1
fi

# Mostrar información del pipeline a crear
echo ""
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}Pipeline a Configurar:${NC}"
echo -e "  Nombre: ${YELLOW}${JOB_NAME}${NC}"
echo -e "  Jenkinsfile: ${YELLOW}Jenkinsfile-UnitTests${NC}"
echo -e "  Repositorio: ${YELLOW}https://github.com/IsaelFatamaDev/vg-ms-users-rev.git${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo ""

# Preguntar si desea continuar
read -p "¿Deseas continuar con la creación del pipeline? (s/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Ss]$ ]]; then
    log_warning "Operación cancelada por el usuario"
    exit 0
fi

# Crear XML de configuración del job
log_info "Generando configuración del pipeline..."
cat > /tmp/jenkins-job-config.xml << 'EOF'
<?xml version='1.1' encoding='UTF-8'?>
<flow-definition plugin="workflow-job@2.40">
  <actions/>
  <description>Pipeline de validación de pruebas unitarias para el microservicio vg-ms-users</description>
  <keepDependencies>false</keepDependencies>
  <properties>
    <hudson.model.ParametersDefinitionProperty>
      <parameterDefinitions>
        <hudson.model.ChoiceParameterDefinition>
          <name>TEST_SCOPE</name>
          <description>Alcance de las pruebas a ejecutar</description>
          <choices class="java.util.Arrays$ArrayList">
            <a class="string-array">
              <string>ALL_TESTS</string>
              <string>USER_SERVICE_ONLY</string>
              <string>SPECIFIC_TEST</string>
            </a>
          </choices>
        </hudson.model.ChoiceParameterDefinition>
        <hudson.model.StringParameterDefinition>
          <name>SPECIFIC_TEST_CLASS</name>
          <description>Clase de prueba específica (solo si TEST_SCOPE = SPECIFIC_TEST)</description>
          <defaultValue>UserServiceImplTest</defaultValue>
          <trim>true</trim>
        </hudson.model.StringParameterDefinition>
      </parameterDefinitions>
    </hudson.model.ParametersDefinitionProperty>
    <org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty>
      <triggers/>
    </org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty>
  </properties>
  <definition class="org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition" plugin="workflow-cps@2.90">
    <scm class="hudson.plugins.git.GitSCM" plugin="git@4.7.1">
      <configVersion>2</configVersion>
      <userRemoteConfigs>
        <hudson.plugins.git.UserRemoteConfig>
          <url>https://github.com/IsaelFatamaDev/vg-ms-users-rev.git</url>
        </hudson.plugins.git.UserRemoteConfig>
      </userRemoteConfigs>
      <branches>
        <hudson.plugins.git.BranchSpec>
          <name>*/main</name>
        </hudson.plugins.git.BranchSpec>
      </branches>
      <doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>
      <submoduleCfg class="list"/>
      <extensions/>
    </scm>
    <scriptPath>Jenkinsfile-UnitTests</scriptPath>
    <lightweight>true</lightweight>
  </definition>
  <triggers/>
  <disabled>false</disabled>
</flow-definition>
EOF

log_success "Configuración del pipeline generada"

# Crear el job en Jenkins
log_info "Creando pipeline '${JOB_NAME}' en Jenkins..."
CREATE_RESPONSE=$(curl -s -w "%{http_code}" -X POST \
    -u "${JENKINS_USER}:${JENKINS_PASSWORD}" \
    "${JENKINS_URL}/createItem?name=${JOB_NAME}" \
    --header "Content-Type: application/xml" \
    --data-binary @/tmp/jenkins-job-config.xml \
    -o /dev/null)

if [ "$CREATE_RESPONSE" = "200" ]; then
    log_success "Pipeline '${JOB_NAME}' creado exitosamente"
elif [ "$CREATE_RESPONSE" = "400" ]; then
    log_warning "El pipeline '${JOB_NAME}' ya existe. Actualizando configuración..."

    # Actualizar el job existente
    UPDATE_RESPONSE=$(curl -s -w "%{http_code}" -X POST \
        -u "${JENKINS_USER}:${JENKINS_PASSWORD}" \
        "${JENKINS_URL}/job/${JOB_NAME}/config.xml" \
        --header "Content-Type: application/xml" \
        --data-binary @/tmp/jenkins-job-config.xml \
        -o /dev/null)

    if [ "$UPDATE_RESPONSE" = "200" ]; then
        log_success "Pipeline '${JOB_NAME}' actualizado exitosamente"
    else
        log_error "No se pudo actualizar el pipeline (HTTP ${UPDATE_RESPONSE})"
        exit 1
    fi
else
    log_error "No se pudo crear el pipeline (HTTP ${CREATE_RESPONSE})"
    exit 1
fi

# Limpiar archivo temporal
rm -f /tmp/jenkins-job-config.xml

# Mostrar instrucciones finales
echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║            ✅ CONFIGURACIÓN COMPLETADA ✅                  ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}El pipeline ha sido configurado exitosamente.${NC}"
echo ""
echo -e "${YELLOW}Próximos pasos:${NC}"
echo -e "  1. Accede a Jenkins: ${GREEN}${JENKINS_URL}${NC}"
echo -e "  2. Ve al job: ${GREEN}${JENKINS_URL}/job/${JOB_NAME}${NC}"
echo -e "  3. Haz clic en ${GREEN}'Build with Parameters'${NC}"
echo -e "  4. Selecciona ${GREEN}TEST_SCOPE = USER_SERVICE_ONLY${NC}"
echo -e "  5. Haz clic en ${GREEN}'Build'${NC}"
echo ""
echo -e "${BLUE}También puedes ejecutarlo desde la línea de comandos:${NC}"
echo -e "${GREEN}curl -X POST '${JENKINS_URL}/job/${JOB_NAME}/buildWithParameters?TEST_SCOPE=USER_SERVICE_ONLY' \\${NC}"
echo -e "${GREEN}  --user '${JENKINS_USER}:${JENKINS_PASSWORD}'${NC}"
echo ""
echo -e "${BLUE}Para ver los logs en tiempo real:${NC}"
echo -e "${GREEN}${JENKINS_URL}/job/${JOB_NAME}/lastBuild/console${NC}"
echo ""
echo -e "${YELLOW}📚 Documentación completa: JENKINS-UNIT-TESTS-SETUP.md${NC}"
echo ""

# Preguntar si desea ejecutar el pipeline inmediatamente
read -p "¿Deseas ejecutar el pipeline ahora? (s/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Ss]$ ]]; then
    log_info "Ejecutando pipeline con TEST_SCOPE=USER_SERVICE_ONLY..."

    BUILD_RESPONSE=$(curl -s -w "%{http_code}" -X POST \
        -u "${JENKINS_USER}:${JENKINS_PASSWORD}" \
        "${JENKINS_URL}/job/${JOB_NAME}/buildWithParameters?TEST_SCOPE=USER_SERVICE_ONLY" \
        -o /dev/null)

    if [ "$BUILD_RESPONSE" = "201" ]; then
        log_success "Pipeline iniciado exitosamente"
        echo ""
        echo -e "${BLUE}Puedes ver el progreso en:${NC}"
        echo -e "${GREEN}${JENKINS_URL}/job/${JOB_NAME}/lastBuild/console${NC}"
        echo ""

        # Abrir en el navegador
        if command -v "$BROWSER" &> /dev/null; then
            log_info "Abriendo Jenkins en el navegador..."
            "$BROWSER" "${JENKINS_URL}/job/${JOB_NAME}/lastBuild/console" &
        fi
    else
        log_error "No se pudo iniciar el pipeline (HTTP ${BUILD_RESPONSE})"
    fi
else
    log_info "Pipeline configurado pero no ejecutado. Puedes ejecutarlo manualmente cuando desees."
fi

echo ""
echo -e "${GREEN}¡Configuración finalizada con éxito! 🚀${NC}"
