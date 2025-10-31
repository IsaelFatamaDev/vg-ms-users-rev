#!/bin/bash

##############################################################################
# Script para configurar el token de SonarQube en Jenkins
##############################################################################

JENKINS_URL="http://localhost:8080"
JENKINS_USER="admin"
JENKINS_PASS="admin"
TOKEN="sqp_357f86a27a3d42e6bcdbe7134ae55e7bb1565299"
CREDENTIAL_ID="sonarqube-token"

echo "🔐 Configurando token de SonarQube en Jenkins..."

# XML para crear la credencial
cat > /tmp/sonarqube-credential.xml << EOF
<com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl>
  <scope>GLOBAL</scope>
  <id>${CREDENTIAL_ID}</id>
  <description>Token de autenticación para SonarQube</description>
  <username>token</username>
  <password>${TOKEN}</password>
</com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl>
EOF

# Esperar a que Jenkins esté listo
echo "⏳ Esperando a Jenkins..."
sleep 3

# Verificar si la credencial ya existe
if curl -s -u "${JENKINS_USER}:${JENKINS_PASS}" \
    "${JENKINS_URL}/credentials/store/system/domain/_/credential/${CREDENTIAL_ID}/api/json" \
    2>/dev/null | grep -q "id"; then
    echo "⚠️  La credencial '${CREDENTIAL_ID}' ya existe. Actualizando..."

    # Actualizar credencial existente
    curl -X POST -u "${JENKINS_USER}:${JENKINS_PASS}" \
        -H "Content-Type: application/xml" \
        --data-binary @/tmp/sonarqube-credential.xml \
        "${JENKINS_URL}/credentials/store/system/domain/_/credential/${CREDENTIAL_ID}/config.xml"
else
    echo "🆕 Creando nueva credencial '${CREDENTIAL_ID}'..."

    # Crear nueva credencial
    curl -X POST -u "${JENKINS_USER}:${JENKINS_PASS}" \
        -H "Content-Type: application/xml" \
        --data-binary @/tmp/sonarqube-credential.xml \
        "${JENKINS_URL}/credentials/store/system/domain/_/createCredentials"
fi

if [ $? -eq 0 ]; then
    echo "✅ Token de SonarQube configurado exitosamente en Jenkins"
    echo ""
    echo "📋 Detalles:"
    echo "   Credential ID: ${CREDENTIAL_ID}"
    echo "   Token: ${TOKEN:0:15}..."
else
    echo "❌ Error al configurar el token"
    exit 1
fi

# Limpiar archivo temporal
rm /tmp/sonarqube-credential.xml

echo ""
echo "🎯 Próximo paso:"
echo "   Ejecuta el pipeline nuevamente en Jenkins"
echo "   ${JENKINS_URL}/job/vg-ms-users-sonarqube/build"
