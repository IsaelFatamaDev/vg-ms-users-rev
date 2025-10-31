#!/bin/bash

###############################################################################
# Script: setup-slack-jenkins.sh
# Descripción: Configura las credenciales de Slack en Jenkins
# Uso: ./setup-slack-jenkins.sh
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
JENKINS_USER="${JENKINS_USER:-admin}"
JENKINS_PASSWORD="${JENKINS_PASSWORD:-}"

# Credenciales de Slack (desde la imagen proporcionada)
SLACK_APP_ID="A09JBZ2J57X"
SLACK_CLIENT_ID="6677214688017.9623101617269"
SLACK_CLIENT_SECRET="097c38f32edaebd7ba9071082bb235c5"
SLACK_SIGNING_SECRET="0cac639d35b2135ef29be578b0662fb5"
SLACK_VERIFICATION_TOKEN="5rf93raqR80Voc2xxTELQc9l"

echo -e "${BLUE}"
echo "╔════════════════════════════════════════════════════════════╗"
echo "║       CONFIGURADOR DE SLACK PARA JENKINS                  ║"
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

# Mostrar información de Slack
echo ""
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}Credenciales de Slack a configurar:${NC}"
echo -e "  App ID: ${YELLOW}${SLACK_APP_ID}${NC}"
echo -e "  Client ID: ${YELLOW}${SLACK_CLIENT_ID}${NC}"
echo -e "  Token: ${YELLOW}${SLACK_SIGNING_SECRET}${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo ""

# Preguntar si desea continuar
read -p "¿Deseas continuar con la configuración? (s/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Ss]$ ]]; then
    log_warning "Operación cancelada por el usuario"
    exit 0
fi

log_info "Creando credencial de Slack en Jenkins..."

# Crear credencial de tipo Secret Text
CRUMB=$(curl -s -u "${JENKINS_USER}:${JENKINS_PASSWORD}" \
    "${JENKINS_URL}/crumbIssuer/api/json" | grep -oP '(?<="crumb":")[^"]*')

if [ -z "$CRUMB" ]; then
    log_warning "No se pudo obtener el CSRF crumb. Intentando sin él..."
fi

# XML de la credencial
CREDENTIAL_XML=$(cat <<EOF
<com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl>
  <scope>GLOBAL</scope>
  <id>slack-token</id>
  <description>Slack Bot Token para notificaciones de Jenkins</description>
  <username>slack-bot</username>
  <password>${SLACK_SIGNING_SECRET}</password>
</com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl>
EOF
)

# Crear la credencial
if [ -n "$CRUMB" ]; then
    CREATE_RESPONSE=$(curl -s -w "%{http_code}" -X POST \
        -u "${JENKINS_USER}:${JENKINS_PASSWORD}" \
        -H "Jenkins-Crumb: ${CRUMB}" \
        -H "Content-Type: application/xml" \
        "${JENKINS_URL}/credentials/store/system/domain/_/createCredentials" \
        --data "${CREDENTIAL_XML}" \
        -o /dev/null)
else
    CREATE_RESPONSE=$(curl -s -w "%{http_code}" -X POST \
        -u "${JENKINS_USER}:${JENKINS_PASSWORD}" \
        -H "Content-Type: application/xml" \
        "${JENKINS_URL}/credentials/store/system/domain/_/createCredentials" \
        --data "${CREDENTIAL_XML}" \
        -o /dev/null)
fi

if [ "$CREATE_RESPONSE" = "200" ] || [ "$CREATE_RESPONSE" = "302" ]; then
    log_success "Credencial 'slack-token' creada exitosamente"
elif [ "$CREATE_RESPONSE" = "409" ]; then
    log_warning "La credencial 'slack-token' ya existe"
else
    log_error "No se pudo crear la credencial (HTTP ${CREATE_RESPONSE})"
    log_info "Puedes crear la credencial manualmente en Jenkins:"
    echo "  1. Ve a: ${JENKINS_URL}/credentials"
    echo "  2. Clic en 'System' → 'Global credentials'"
    echo "  3. Clic en 'Add Credentials'"
    echo "  4. Kind: 'Username with password'"
    echo "  5. ID: 'slack-token'"
    echo "  6. Username: 'slack-bot'"
    echo "  7. Password: '${SLACK_SIGNING_SECRET}'"
    echo ""
    read -p "¿Has creado la credencial manualmente? (s/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Ss]$ ]]; then
        exit 1
    fi
fi

# Instrucciones para instalar el plugin de Slack
echo ""
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}IMPORTANTE: Instalar el plugin de Slack en Jenkins${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo ""
echo "Para que las notificaciones funcionen, debes instalar el plugin de Slack:"
echo ""
echo "1. Ve a: ${JENKINS_URL}/pluginManager/available"
echo "2. Busca 'Slack Notification Plugin'"
echo "3. Marca el checkbox"
echo "4. Clic en 'Install without restart'"
echo ""
echo "O ejecuta este comando:"
echo -e "${GREEN}docker exec jenkins jenkins-plugin-cli --plugins slack${NC}"
echo ""

read -p "¿Deseas instalar el plugin de Slack ahora? (s/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Ss]$ ]]; then
    log_info "Instalando plugin de Slack..."

    if docker exec jenkins jenkins-plugin-cli --plugins slack 2>/dev/null; then
        log_success "Plugin de Slack instalado"
        log_warning "Es necesario reiniciar Jenkins para que el plugin tome efecto"

        read -p "¿Deseas reiniciar Jenkins ahora? (s/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Ss]$ ]]; then
            log_info "Reiniciando Jenkins..."
            curl -s -X POST -u "${JENKINS_USER}:${JENKINS_PASSWORD}" \
                "${JENKINS_URL}/safeRestart" > /dev/null
            log_success "Jenkins se está reiniciando. Espera unos minutos antes de ejecutar el pipeline."
        fi
    else
        log_error "No se pudo instalar el plugin automáticamente. Instálalo manualmente."
    fi
fi

# Instrucciones para configurar Slack
echo ""
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}CONFIGURACIÓN DE SLACK${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo ""
echo "Configura Slack en Jenkins:"
echo ""
echo "1. Ve a: ${JENKINS_URL}/configure"
echo "2. Busca la sección 'Slack'"
echo "3. Configura:"
echo "   - Workspace: Tu workspace de Slack"
echo "   - Credential: Selecciona 'slack-token'"
echo "   - Default channel: #jenkins-notifications"
echo "4. Haz clic en 'Test Connection'"
echo "5. Si la conexión es exitosa, guarda los cambios"
echo ""

# Mostrar instrucciones finales
echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║            ✅ CONFIGURACIÓN COMPLETADA ✅                  ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}Próximos pasos:${NC}"
echo -e "  1. Asegúrate de que el plugin de Slack esté instalado"
echo -e "  2. Configura Slack en: ${GREEN}${JENKINS_URL}/configure${NC}"
echo -e "  3. Crea el canal ${GREEN}#jenkins-notifications${NC} en tu Slack"
echo -e "  4. Invita al bot de Jenkins a ese canal"
echo -e "  5. Ejecuta el pipeline: ${GREEN}${JENKINS_URL}/job/vg-ms-users-unit-tests${NC}"
echo ""
echo -e "${BLUE}Credenciales de Slack guardadas:${NC}"
echo -e "  ID: ${GREEN}slack-token${NC}"
echo -e "  Token: ${GREEN}${SLACK_SIGNING_SECRET}${NC}"
echo ""
echo -e "${YELLOW}📚 Documentación: SLACK-INTEGRATION.md${NC}"
echo ""
echo -e "${GREEN}¡Configuración finalizada con éxito! 🚀${NC}"
