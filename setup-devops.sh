#!/bin/bash

# =============================================================================
# SCRIPT DE INICIALIZACIÓN - JENKINS & SONARQUBE
# =============================================================================

set -e

echo "=============================================="
echo "🚀 Iniciando Jenkins y SonarQube"
echo "=============================================="

# Colores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Función para esperar que un servicio esté listo
wait_for_service() {
    local service_name=$1
    local url=$2
    local max_attempts=30
    local attempt=1

    echo -e "${YELLOW}⏳ Esperando a que $service_name esté listo...${NC}"

    while [ $attempt -le $max_attempts ]; do
        if curl -s -o /dev/null -w "%{http_code}" "$url" | grep -q "200\|403"; then
            echo -e "${GREEN}✅ $service_name está listo!${NC}"
            return 0
        fi
        echo "   Intento $attempt/$max_attempts..."
        sleep 10
        attempt=$((attempt + 1))
    done

    echo -e "${RED}❌ $service_name no respondió a tiempo${NC}"
    return 1
}

# 1. Detener contenedores existentes
echo -e "\n${YELLOW}🛑 Deteniendo contenedores existentes...${NC}"
docker-compose down -v 2>/dev/null || true

# 2. Levantar servicios
echo -e "\n${YELLOW}🐳 Levantando servicios con Docker Compose...${NC}"
docker-compose up -d

# 3. Esperar a que los servicios estén listos
echo -e "\n${YELLOW}⏳ Esperando a que los servicios inicien...${NC}"
sleep 15

# Verificar SonarQube
if wait_for_service "SonarQube" "http://localhost:9000"; then
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}📊 SonarQube:${NC}"
    echo -e "   URL: ${YELLOW}http://localhost:9000${NC}"
    echo -e "   Usuario: ${YELLOW}admin${NC}"
    echo -e "   Contraseña: ${YELLOW}admin${NC}"
    echo -e "   ${RED}⚠️  Cambiar contraseña en primer login${NC}"
fi

# Verificar Jenkins
if wait_for_service "Jenkins" "http://localhost:8080"; then
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}🔧 Jenkins:${NC}"
    echo -e "   URL: ${YELLOW}http://localhost:8080${NC}"
    echo -e "   ${YELLOW}📝 Obtener password inicial:${NC}"
    echo -e "   docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword"
fi

# Verificar MongoDB
if wait_for_service "MongoDB" "http://localhost:27017"; then
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}🍃 MongoDB:${NC}"
    echo -e "   URL: ${YELLOW}mongodb://localhost:27017${NC}"
    echo -e "   Usuario: ${YELLOW}admin${NC}"
    echo -e "   Contraseña: ${YELLOW}admin123${NC}"
fi

echo -e "\n${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ Todos los servicios están listos!${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

echo -e "\n${YELLOW}📋 SIGUIENTES PASOS:${NC}"
echo -e "1. Configurar Jenkins:"
echo -e "   - Instalar plugins: Maven Integration, SonarQube Scanner, Docker"
echo -e "   - Configurar Maven y JDK en Global Tool Configuration"
echo -e "   - Agregar credenciales de SonarQube"
echo -e ""
echo -e "2. Configurar SonarQube:"
echo -e "   - Cambiar contraseña por defecto"
echo -e "   - Crear proyecto 'vg-ms-users'"
echo -e "   - Generar token de autenticación"
echo -e ""
echo -e "3. Ejecutar análisis local:"
echo -e "   ${YELLOW}mvn clean verify sonar:sonar${NC}"
echo -e ""
echo -e "${GREEN}🎉 ¡Listo para empezar!${NC}"
