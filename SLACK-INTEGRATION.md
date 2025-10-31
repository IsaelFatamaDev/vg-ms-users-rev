# 📱 Integración de Slack con Jenkins - Notificaciones de Pruebas

Esta guía describe cómo configurar las notificaciones de Slack para el pipeline de pruebas unitarias de Jenkins.

---

## 📋 Tabla de Contenidos

1. [Requisitos Previos](#requisitos-previos)
2. [Credenciales de Slack](#credenciales-de-slack)
3. [Configuración Automática](#configuración-automática)
4. [Configuración Manual](#configuración-manual)
5. [Formato de Notificaciones](#formato-de-notificaciones)
6. [Solución de Problemas](#solución-de-problemas)

---

## ✅ Requisitos Previos

- ✅ Jenkins corriendo en `http://localhost:8080`
- ✅ Pipeline `vg-ms-users-unit-tests` ya configurado
- ✅ Cuenta de Slack con permisos para crear aplicaciones
- ✅ Canal de Slack creado: `#jenkins-notifications`

---

## 🔑 Credenciales de Slack

Según la imagen proporcionada, estas son las credenciales de tu aplicación Slack:

```
App ID:              A09JB2ZJ57X
Client ID:           6677214688017.9623101617269
Client Secret:       097c38f32edaebd7ba9071082bb235c5
Signing Secret:      0cac639d35b2135ef29be578b0662fb5
Verification Token:  5rf93raqR80Voc2xxTELQc9I
```

**⚠️ IMPORTANTE:** Estas credenciales son **secretas**. No las compartas públicamente.

---

## 🚀 Configuración Automática (Recomendado)

### Paso 1: Ejecutar el Script de Configuración

```bash
# Dar permisos de ejecución
chmod +x setup-slack-jenkins.sh

# Ejecutar el script
./setup-slack-jenkins.sh
```

El script automáticamente:

- ✅ Verifica que Jenkins está corriendo
- ✅ Crea la credencial `slack-token` en Jenkins
- ✅ Instala el plugin de Slack (opcional)
- ✅ Proporciona instrucciones para completar la configuración

---

## 🔧 Configuración Manual

Si prefieres configurar manualmente, sigue estos pasos:

### Paso 1: Instalar el Plugin de Slack

1. Ve a **Jenkins** → **Manage Jenkins** → **Manage Plugins**
2. Pestaña **"Available"**
3. Busca **"Slack Notification Plugin"**
4. Marca el checkbox y haz clic en **"Install without restart"**

**O desde la terminal:**

```bash
docker exec jenkins jenkins-plugin-cli --plugins slack
```

### Paso 2: Crear Credencial en Jenkins

1. Ve a: `http://localhost:8080/credentials/`
2. Clic en **"System"** → **"Global credentials (unrestricted)"**
3. Clic en **"Add Credentials"**
4. Configura:
   - **Kind:** `Username with password`
   - **Scope:** `Global`
   - **Username:** `slack-bot`
   - **Password:** `0cac639d35b2135ef29be578b0662fb5` (Signing Secret)
   - **ID:** `slack-token`
   - **Description:** `Slack Bot Token para notificaciones`
5. Clic en **"Create"**

### Paso 3: Configurar Slack en Jenkins

1. Ve a: `http://localhost:8080/configure`
2. Desplázate hasta la sección **"Slack"**
3. Configura:
   - **Workspace:** Tu nombre de workspace de Slack (ej: `mi-empresa`)
   - **Credential:** Selecciona `slack-token`
   - **Default channel / member id:** `#jenkins-notifications`
   - **Bot user OAuth token:** Deja en blanco (se usa la credencial)
4. Haz clic en **"Test Connection"**
5. Si ves ✅ "Success", haz clic en **"Save"**

### Paso 4: Configurar el Bot en Slack

1. Ve a tu aplicación en Slack: <https://api.slack.com/apps/A09JBZ2J57X>
2. En **"OAuth & Permissions"**:
   - Agrega estos **Bot Token Scopes:**
     - `chat:write`
     - `chat:write.public`
     - `chat:write.customize`
     - `incoming-webhook`
3. Clic en **"Install to Workspace"**
4. Autoriza la aplicación

### Paso 5: Invitar el Bot al Canal

En Slack:

```
1. Ve al canal #jenkins-notifications
2. Escribe: /invite @Jenkins
3. Confirma la invitación
```

---

## 📨 Formato de Notificaciones

El pipeline enviará notificaciones en tres escenarios:

### ✅ Build Exitoso (Color Verde)

```
✅ BUILD EXITOSO - vg-ms-users-unit-tests

Build: #5
Duración: 45.2s
Alcance: USER_SERVICE_ONLY
Tests Ejecutados: 6
Tests Exitosos: 6

[Ver detalles del build]
[Ver reporte de tests]
[Ver cobertura de código]
```

### ⚠️ Build Inestable (Color Amarillo)

```
⚠️ BUILD INESTABLE - vg-ms-users-unit-tests

Build: #6
Duración: 52.1s
Alcance: USER_SERVICE_ONLY
Tests Totales: 6
Tests Fallidos: 2

[Ver detalles del build]
[Ver tests fallidos]
```

### ❌ Build Fallido (Color Rojo)

```
❌ BUILD FALLIDO - vg-ms-users-unit-tests

Build: #7
Duración: 12.5s
Alcance: USER_SERVICE_ONLY
Estado: FAILURE

Posibles causas:
• Error de compilación
• Fallos en pruebas unitarias
• Error en la configuración

[Ver logs completos]
[Ver reporte de tests]
```

---

## 🎨 Personalizar Notificaciones

Puedes personalizar las notificaciones editando el `Jenkinsfile-UnitTests`:

### Cambiar el Canal

```groovy
environment {
    SLACK_CHANNEL = '#mi-canal-personalizado'  // Cambiar aquí
    SLACK_TOKEN = credentials('slack-token')
}
```

### Cambiar los Colores

```groovy
slackSend(
    channel: env.SLACK_CHANNEL,
    color: 'good',      // 'good' (verde), 'warning' (amarillo), 'danger' (rojo)
    message: """...""",
    tokenCredentialId: 'slack-token'
)
```

### Agregar Más Información

```groovy
def gitCommit = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
def gitAuthor = sh(returnStdout: true, script: 'git log -1 --pretty=%an').trim()

slackSend(
    channel: env.SLACK_CHANNEL,
    color: 'good',
    message: """
✅ *BUILD EXITOSO* - ${env.JOB_NAME}

*Build:* #${env.BUILD_NUMBER}
*Commit:* ${gitCommit}
*Autor:* ${gitAuthor}
*Tests:* ${totalTests} ejecutados, ${passedTests} exitosos

<${env.BUILD_URL}|Ver detalles>
    """.trim(),
    tokenCredentialId: 'slack-token'
)
```

---

## 🔍 Verificar Configuración

### 1. Verificar Credencial Creada

```bash
# Listar credenciales
curl -u admin:password http://localhost:8080/credentials/api/json | grep "slack-token"
```

### 2. Verificar Plugin Instalado

```bash
# Listar plugins instalados
docker exec jenkins jenkins-plugin-cli --list | grep slack
```

Deberías ver:

```
slack    x.x.x
```

### 3. Probar Notificación

Ejecuta el pipeline y verifica que llegue la notificación a Slack:

```bash
# Ejecutar pipeline
curl -X POST 'http://localhost:8080/job/vg-ms-users-unit-tests/buildWithParameters?TEST_SCOPE=USER_SERVICE_ONLY' \
  --user 'admin:password'
```

---

## 🐛 Solución de Problemas

### Problema 1: "Credential not found"

**Solución:**

1. Verifica que la credencial existe:

   ```bash
   curl -u admin:password http://localhost:8080/credentials/
   ```

2. Recrea la credencial:

   ```bash
   ./setup-slack-jenkins.sh
   ```

### Problema 2: "Slack plugin not found"

**Solución:**

```bash
# Instalar plugin
docker exec jenkins jenkins-plugin-cli --plugins slack

# Reiniciar Jenkins
docker restart jenkins

# Esperar a que Jenkins esté listo
sleep 30
```

### Problema 3: "Test connection failed"

**Solución:**

1. Verifica que el token es correcto
2. Verifica que la app tiene los permisos necesarios:
   - `chat:write`
   - `chat:write.public`
   - `incoming-webhook`
3. Reinstala la app en tu workspace de Slack

### Problema 4: "Channel not found"

**Solución:**

1. Crea el canal en Slack:

   ```
   /channel create jenkins-notifications
   ```

2. Invita al bot:

   ```
   /invite @Jenkins
   ```

3. Verifica que el canal es público o el bot tiene acceso

### Problema 5: No llegan las notificaciones

**Solución:**

1. Verifica los logs de Jenkins:

   ```bash
   docker logs jenkins | grep -i slack
   ```

2. Verifica que el bot está en el canal:

   ```
   En Slack: Ir al canal → Ver detalles → Integraciones
   ```

3. Prueba enviar una notificación manualmente desde Jenkins:

   ```groovy
   slackSend channel: '#jenkins-notifications', message: 'Test'
   ```

---

## 📊 Ejemplo de Uso Completo

### 1. Configurar Slack

```bash
# Ejecutar script de configuración
./setup-slack-jenkins.sh
```

### 2. Crear Canal en Slack

```
En Slack:
1. Clic en "+" al lado de "Channels"
2. Nombre: jenkins-notifications
3. Descripción: Notificaciones de builds de Jenkins
4. Hacer público
5. Crear canal
```

### 3. Invitar Bot al Canal

```
En #jenkins-notifications:
/invite @Jenkins
```

### 4. Ejecutar Pipeline

```bash
# Ejecutar build
curl -X POST 'http://localhost:8080/job/vg-ms-users-unit-tests/buildWithParameters?TEST_SCOPE=USER_SERVICE_ONLY' \
  --user 'admin:password'
```

### 5. Verificar Notificación

En Slack, deberías ver un mensaje como:

```
✅ BUILD EXITOSO - vg-ms-users-unit-tests

Build: #1
Duración: 45s
Alcance: USER_SERVICE_ONLY
Tests Ejecutados: 6
Tests Exitosos: 6

[Ver detalles del build]
[Ver reporte de tests]
[Ver cobertura de código]
```

---

## 🎯 Checklist de Validación

Antes de dar por terminada la integración, verifica:

- [ ] ✅ Plugin de Slack instalado en Jenkins
- [ ] ✅ Credencial `slack-token` creada
- [ ] ✅ Configuración de Slack en Jenkins completada
- [ ] ✅ Test de conexión exitoso
- [ ] ✅ Canal `#jenkins-notifications` creado en Slack
- [ ] ✅ Bot invitado al canal
- [ ] ✅ Pipeline ejecutado exitosamente
- [ ] ✅ Notificación recibida en Slack
- [ ] ✅ Links en la notificación funcionan correctamente

---

## 📚 Recursos Adicionales

- [Slack API Documentation](https://api.slack.com/docs)
- [Jenkins Slack Plugin](https://plugins.jenkins.io/slack/)
- [Creating Slack Apps](https://api.slack.com/apps)
- [OAuth Scopes](https://api.slack.com/scopes)

---

## 🔐 Seguridad

**IMPORTANTE:**

- ❌ No compartas las credenciales de Slack públicamente
- ❌ No las incluyas en el código fuente del repositorio
- ✅ Usa Jenkins Credentials para almacenarlas de forma segura
- ✅ Rota los tokens periódicamente
- ✅ Restringe los permisos del bot a lo mínimo necesario

---

## 🎉 ¡Listo

Tu pipeline de Jenkins ahora notificará automáticamente a Slack cada vez que:

- ✅ Las pruebas pasen exitosamente (verde)
- ⚠️ Algunas pruebas fallen (amarillo)
- ❌ El build falle completamente (rojo)

¡Disfruta de tus notificaciones en Slack! 🚀📱
