# 🔍 Guía Completa: Integración Jenkins + SonarQube

> **Proyecto:** VG MS Users Rev
> **Fecha:** 31 de Octubre de 2025
> **Objetivo:** Configurar análisis de calidad de código con SonarQube y notificaciones a Slack

---

## 📋 Tabla de Contenidos

1. [Prerrequisitos](#prerrequisitos)
2. [Configuración de SonarQube](#configuración-de-sonarqube)
3. [Configuración de Jenkins](#configuración-de-jenkins)
4. [Configuración del Pipeline](#configuración-del-pipeline)
5. [Ejecución y Verificación](#ejecución-y-verificación)
6. [Interpretación de Resultados](#interpretación-de-resultados)
7. [Solución de Problemas](#solución-de-problemas)

---

## ✅ Prerrequisitos

Antes de comenzar, verifica que tengas:

- [ ] Docker y Docker Compose instalados
- [ ] Servicios corriendo: Jenkins, SonarQube, PostgreSQL
- [ ] Acceso a Slack con un token configurado
- [ ] Repositorio Git accesible

### Verificar servicios

```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
```

**Deberías ver:**

- ✅ `jenkins` - Up (healthy)
- ✅ `sonarqube` - Up (healthy)
- ✅ `sonarqube-db` - Up (healthy)

---

## 🔧 1. Configuración de SonarQube

### 1.1 Acceder a SonarQube

1. Abre tu navegador y ve a: **<http://localhost:9000>**
2. **Credenciales por defecto:**
   - Usuario: `admin`
   - Contraseña: `admin`
3. Te pedirá cambiar la contraseña en el primer acceso (opcional, puedes mantener `admin`)

---

### 1.2 Crear Proyecto en SonarQube

#### Opción A: Crear Proyecto Local (Recomendado para este caso)

1. En la página principal de SonarQube, haz clic en **"Create a local project"** (botón azul)

2. **Configurar el proyecto:**

   ```
   Project display name: VG MS Users Rev
   Project key: vg-ms-users-rev
   Main branch name: main
   ```

3. Haz clic en **"Next"**

4. **Configurar análisis:**
   - Selecciona: **"Use the global setting"**
   - Haz clic en **"Create project"**

5. **Generar Token:**
   - Te preguntará cómo analizar el proyecto
   - Selecciona: **"Locally"**
   - En "Provide a token", haz clic en **"Generate"**
   - **Nombre del token:** `jenkins-integration`
   - Haz clic en **"Generate"**
   - **⚠️ IMPORTANTE:** Copia el token generado (algo como: `sqp_1234567890abcdef...`)
   - **GUARDA ESTE TOKEN**, lo necesitarás en Jenkins

6. **Configurar tipo de proyecto:**
   - Selecciona: **"Maven"**
   - Verás el comando para ejecutar el análisis (no lo necesitas ejecutar ahora)

---

#### Opción B: Importar desde GitHub (Si prefieres conectar con tu repo)

1. Haz clic en **"Import from GitHub"** → **"Setup"**
2. Sigue los pasos para conectar tu cuenta de GitHub
3. Selecciona el repositorio: `IsaelFatamaDev/vg-ms-users-rev`
4. SonarQube creará el proyecto automáticamente

---

### 1.3 Configurar Quality Gates (Opcional pero Recomendado)

1. Ve a **"Quality Gates"** en el menú superior
2. Verás el Quality Gate por defecto: **"Sonar way"**
3. Puedes crear uno personalizado o usar el por defecto
4. **Para este proyecto, usaremos el por defecto** ✅

**¿Qué es un Quality Gate?**
Es un conjunto de condiciones que debe cumplir tu código:

- ✅ Cobertura de código > 80%
- ✅ Sin bugs críticos
- ✅ Sin vulnerabilidades de seguridad
- ✅ Duplicación de código < 3%

---

## 🔐 2. Configuración de Jenkins

### 2.1 Instalar Plugins Necesarios

1. Ve a Jenkins: **<http://localhost:8080>**
2. Inicia sesión con: `admin` / `admin`
3. Ve a: **"Manage Jenkins"** → **"Plugins"** → **"Available plugins"**

4. **Busca e instala estos plugins:**
   - [ ] **SonarQube Scanner** (para análisis de código)
   - [ ] **Slack Notification Plugin** (ya debería estar instalado)
   - [ ] **Pipeline** (ya debería estar instalado)

5. Marca los checkboxes y haz clic en **"Install"**
6. Espera a que se instalen y reinicia Jenkins si es necesario

---

### 2.2 Configurar SonarQube Server en Jenkins

1. Ve a: **"Manage Jenkins"** → **"System"**
2. Desplázate hasta encontrar la sección **"SonarQube servers"**
3. Haz clic en **"Add SonarQube"**

4. **Configura así:**

   ```
   Name: SonarQube-Server
   Server URL: http://sonarqube:9000
   ☑ Server authentication token: [selecciona el credential que crearemos abajo]
   ```

5. **Crear el credential del token:**
   - Haz clic en el botón **"Add"** junto a "Server authentication token"
   - Selecciona **"Jenkins"**
   - En el formulario:

     ```
     Kind: Secret text
     Scope: Global
     Secret: [Pega aquí el token que copiaste de SonarQube]
     ID: sonarqube-token
     Description: Token de autenticación para SonarQube
     ```

   - Haz clic en **"Add"**
   - Ahora selecciona: **"sonarqube-token"** en el dropdown

6. Haz clic en **"Save"** al final de la página

---

### 2.3 Configurar Scanner de SonarQube

1. Ve a: **"Manage Jenkins"** → **"Tools"**
2. Desplázate hasta **"SonarQube Scanner installations"**
3. Haz clic en **"Add SonarQube Scanner"**

4. **Configura así:**

   ```
   Name: SonarQube Scanner
   ☑ Install automatically
   Version: [Selecciona la última versión disponible]
   ```

5. Haz clic en **"Save"**

---

### 2.4 Verificar otras configuraciones

#### Verificar JDK17

1. Ve a: **"Manage Jenkins"** → **"Tools"**
2. Busca: **"JDK installations"**
3. Verifica que exista **"JDK17"** con la ruta: `/usr/lib/jvm/java-17-openjdk-amd64`

#### Verificar Maven3

1. En la misma página de Tools
2. Busca: **"Maven installations"**
3. Verifica que exista **"Maven3"**

#### Verificar Slack (Ya debería estar configurado)

1. Ve a: **"Manage Jenkins"** → **"System"**
2. Busca: **"Slack"**
3. Verifica que exista la credencial: **"Slack-Tokencito"**
4. Canal por defecto: **#jenkins-notifications**

---

## 🚀 3. Configuración del Pipeline

### 3.1 Crear el Job en Jenkins

#### Opción A: Usando el script automático (Más fácil)

```bash
# Ejecuta el script desde la terminal
./setup-jenkins-sonarqube.sh
```

El script creará automáticamente el job con toda la configuración necesaria.

---

#### Opción B: Manualmente (Paso a paso)

1. En Jenkins, haz clic en **"New Item"**

2. **Configurar el job:**

   ```
   Enter an item name: vg-ms-users-sonarqube
   Tipo: Pipeline
   ```

3. Haz clic en **"OK"**

4. **En la configuración del job:**

   **General:**
   - ☑ Discard old builds
     - Days to keep builds: `30`
     - Max # of builds to keep: `10`

   **Build Triggers:**
   - ☑ Poll SCM
     - Schedule: `H/5 * * * *` (cada 5 minutos)

   **Pipeline:**
   - Definition: `Pipeline script from SCM`
   - SCM: `Git`
   - Repository URL: `https://github.com/IsaelFatamaDev/vg-ms-users-rev.git`
   - Branches to build: `*/main`
   - Script Path: `Jenkinsfile-SonarQube`

5. Haz clic en **"Save"**

---

### 3.2 Estructura del Pipeline

El archivo `Jenkinsfile-SonarQube` contiene 4 etapas principales:

```groovy
1. 🔍 Checkout      → Obtiene el código del repositorio
2. 🔧 Compilar      → Compila el proyecto con Maven
3. 📊 Análisis      → Ejecuta el análisis de SonarQube
4. 🚦 Quality Gate  → Espera el resultado del Quality Gate
```

**Notificaciones a Slack:**

- ✅ **Success** (verde): Análisis completado y Quality Gate aprobado
- ⚠️ **Unstable** (amarillo): Análisis con advertencias
- ❌ **Failure** (rojo): Compilación falló o Quality Gate rechazado

---

## ▶️ 4. Ejecución y Verificación

### 4.1 Primera Ejecución

1. Ve al job: **<http://localhost:8080/job/vg-ms-users-sonarqube/>**
2. Haz clic en **"Build Now"**
3. Observa la ejecución en **"Build History"**
4. Haz clic en el número del build (ej: #1)
5. Haz clic en **"Console Output"** para ver los logs

---

### 4.2 Qué esperar durante la ejecución

```
[Pipeline] Start
└─ 📥 Checkout
   └─ Clonando repositorio...
   └─ ✅ Código obtenido

└─ ⚙️ Compilar
   └─ mvn clean compile...
   └─ ✅ Compilación exitosa

└─ 🔍 Análisis SonarQube
   └─ mvn sonar:sonar...
   └─ Analizando código...
   └─ Enviando resultados a SonarQube...
   └─ ✅ Análisis completado

└─ 🚦 Quality Gate
   └─ Esperando resultado...
   └─ ✅ Quality Gate PASSED (o FAILED)

└─ 📬 Notificación Slack
   └─ ✅ Mensaje enviado a #jenkins-notifications
```

**Tiempo estimado:** 2-5 minutos

---

### 4.3 Verificar Notificación en Slack

1. Ve a tu workspace de Slack: **fatama**
2. Abre el canal: **#jenkins-notifications**
3. Deberías ver un mensaje como:

```
✅ Pipeline SonarQube - EXITOSO

Proyecto: VG MS Users Rev
Build: #1
Branch: main
Commit: abc1234

📊 Análisis SonarQube
• Estado: Completado ✅
• Quality Gate: PASSED ✅

🔗 Enlaces:
Ver Build
Ver Dashboard SonarQube

⏱ Duración: 3m 45s
```

---

## 📊 5. Interpretación de Resultados en SonarQube

### 5.1 Ver el Dashboard

1. Ve a SonarQube: **<http://localhost:9000>**
2. Verás el proyecto: **"VG MS Users Rev"**
3. Haz clic en el proyecto para ver el dashboard completo

---

### 5.2 Métricas Principales

#### 🐛 Bugs

- **Qué son:** Errores probables en el código que pueden causar comportamiento incorrecto
- **Ejemplo:** NullPointerException, ArrayIndexOutOfBounds
- **Objetivo:** 0 bugs

#### 🔒 Vulnerabilities (Vulnerabilidades)

- **Qué son:** Problemas de seguridad en el código
- **Ejemplo:** SQL Injection, Cross-Site Scripting (XSS)
- **Objetivo:** 0 vulnerabilidades

#### 💨 Code Smells (Olores de Código)

- **Qué son:** Problemas de mantenibilidad y legibilidad
- **Ejemplo:** Métodos muy largos, duplicación de código, complejidad ciclomática alta
- **Objetivo:** Minimizar (depende del proyecto)

#### 📈 Coverage (Cobertura)

- **Qué es:** Porcentaje de código cubierto por tests
- **Ejemplo:** 75% significa que el 75% del código tiene pruebas unitarias
- **Objetivo:** > 80% (ideal)

#### 🔄 Duplications (Duplicación)

- **Qué es:** Porcentaje de código duplicado
- **Ejemplo:** Bloques de código copiados y pegados
- **Objetivo:** < 3%

---

### 5.3 Niveles de Severidad

| Icono | Nivel | Descripción | Acción |
|-------|-------|-------------|--------|
| 🔴 | **BLOCKER** | Debe arreglarse inmediatamente | ❗ Crítico |
| 🟠 | **CRITICAL** | Muy importante | ❗ Alta prioridad |
| 🟡 | **MAJOR** | Importante | ⚠️ Media prioridad |
| 🟢 | **MINOR** | Menor importancia | ℹ️ Baja prioridad |
| ⚪ | **INFO** | Informativo | 💡 Sugerencia |

---

### 5.4 Explorar Issues (Problemas)

1. En el dashboard, haz clic en el número junto a **"Bugs"**, **"Vulnerabilities"** o **"Code Smells"**
2. Verás una lista de todos los issues encontrados
3. Haz clic en un issue para ver:
   - 📄 **Ubicación:** Archivo y línea exacta
   - 📝 **Descripción:** Qué problema detectó
   - 💡 **Solución:** Cómo arreglarlo
   - 📚 **Referencias:** Documentación adicional

**Ejemplo de un Bug:**

```
Issue: Null pointer dereference
File: UserServiceImpl.java
Line: 145
Severity: CRITICAL

Problem:
  NullPointerException might be thrown as 'user' can be null

Solution:
  Add null check before using 'user' object:
  if (user != null) {
      // use user
  }
```

---

### 5.5 Ver Cobertura de Tests

1. En el dashboard, haz clic en **"Coverage"**
2. Verás:
   - **Overall Coverage:** Cobertura total del proyecto
   - **Coverage per file:** Cobertura por cada archivo
3. Haz clic en un archivo para ver:
   - 🟢 **Líneas verdes:** Cubiertas por tests
   - 🔴 **Líneas rojas:** NO cubiertas por tests
   - 🟡 **Líneas amarillas:** Parcialmente cubiertas

---

### 5.6 Quality Gate Status

En la parte superior del dashboard verás:

#### ✅ **PASSED** (Verde)

```
Quality Gate Passed ✅
Your code meets the quality standards
```

**Significado:** El código cumple con todos los criterios de calidad

#### ❌ **FAILED** (Rojo)

```
Quality Gate Failed ❌
Your code does not meet the quality standards

Failed conditions:
• Coverage is 45.2% (is less than 80%)
• 3 New Bugs (is greater than 0)
```

**Significado:** El código NO cumple con algunos criterios. Debes arreglar los problemas indicados.

---

## 🔍 6. Interpretación del Reporte Completo

### 6.1 Ejemplo de Reporte Completo

Cuando ejecutes el pipeline, verás algo así en SonarQube:

```
┌─────────────────────────────────────────────────┐
│          VG MS Users Rev                        │
│          Last Analysis: 31/10/2025 10:30       │
└─────────────────────────────────────────────────┘

🎯 Quality Gate: PASSED ✅

📊 Métricas Overview:
┌──────────────────┬─────────┬─────────────┐
│ Métrica          │ Valor   │ Estado      │
├──────────────────┼─────────┼─────────────┤
│ Bugs             │ 0       │ ✅          │
│ Vulnerabilities  │ 0       │ ✅          │
│ Code Smells      │ 15      │ ⚠️          │
│ Coverage         │ 75.8%   │ ⚠️ (< 80%)  │
│ Duplications     │ 1.2%    │ ✅          │
│ Security Hotspots│ 2       │ 🔍 Revisar  │
└──────────────────┴─────────┴─────────────┘

📝 Detalles:
• Lines of Code: 2,450
• Technical Debt: 1h 30min
• Maintainability Rating: A
• Reliability Rating: A
• Security Rating: A
```

---

### 6.2 ¿Qué hacer con cada resultado?

#### Si el Quality Gate es **PASSED** ✅

1. ✅ **¡Excelente!** Tu código cumple con los estándares de calidad
2. Revisa los **Code Smells** y mejora gradualmente
3. Trabaja en aumentar la **Coverage** si está por debajo del 80%
4. Continúa con el desarrollo

#### Si el Quality Gate es **FAILED** ❌

1. ❗ **No hagas merge/deploy** hasta arreglar los problemas críticos
2. Lee las **Failed conditions** (condiciones fallidas)
3. Arregla primero:
   - 🐛 **Bugs BLOCKER y CRITICAL**
   - 🔒 **Vulnerabilities BLOCKER y CRITICAL**
4. Luego mejora:
   - 📈 **Coverage** (agrega más tests)
   - 💨 **Code Smells** (refactoriza el código)
5. Ejecuta el pipeline nuevamente

---

### 6.3 Security Hotspots (Puntos Calientes de Seguridad)

**¿Qué son?**
Son áreas del código que SonarQube considera **potencialmente inseguras** y requieren revisión manual.

**Ejemplo:**

```java
// Security Hotspot: Using crypto key
private static final String SECRET_KEY = "my-secret-123";

❓ Is this key properly secured?
✅ Yes, it's from environment variable
❌ No, it's hardcoded → SECURITY RISK
```

**Cómo revisarlos:**

1. Ve a **"Security Hotspots"** en el menú
2. Lee cada uno y decide:
   - **✅ Safe:** Marca como "Safe" si está bien
   - **❌ Vulnerable:** Marca como "To Review" y arréglalo

---

## 🛠️ 7. Solución de Problemas Comunes

### Problema 1: "Quality Gate Failed"

**Síntoma:**

```
❌ Quality Gate Failed
• Coverage is 45% (is less than 80%)
```

**Solución:**

1. Agrega más tests unitarios
2. Ejecuta: `mvn test` para verificar localmente
3. Verifica cobertura: `mvn clean test jacoco:report`
4. Abre: `target/site/jacoco/index.html`
5. Escribe tests para las líneas rojas
6. Ejecuta el pipeline nuevamente

---

### Problema 2: "Connection refused to SonarQube"

**Síntoma:**

```
ERROR: SonarQube server [http://sonarqube:9000] can not be reached
```

**Solución:**

```bash
# 1. Verifica que SonarQube esté corriendo
docker ps | grep sonarqube

# 2. Si no está corriendo, inícialo
docker compose up -d sonarqube

# 3. Espera a que esté healthy (30-60 segundos)
docker ps

# 4. Ejecuta el pipeline nuevamente
```

---

### Problema 3: "Authentication token is invalid"

**Síntoma:**

```
ERROR: Not authorized. Please check the user token
```

**Solución:**

1. Ve a SonarQube: <http://localhost:9000>
2. Navega a: **User → My Account → Security**
3. Genera un nuevo token:
   - Name: `jenkins-integration-new`
   - Type: `Global Analysis Token`
4. Copia el nuevo token
5. Ve a Jenkins: **Manage Jenkins → Credentials**
6. Edita el credential `sonarqube-token`
7. Reemplaza con el nuevo token
8. Ejecuta el pipeline nuevamente

---

### Problema 4: "No se recibe notificación en Slack"

**Síntoma:**

```
⚠️ No se pudo enviar notificación a Slack
```

**Solución:**

1. Verifica la credencial en Jenkins:

   ```
   Manage Jenkins → Credentials → Slack-Tokencito
   ```

2. Verifica el canal en Slack existe: `#jenkins-notifications`
3. Verifica que el bot de Jenkins esté agregado al canal
4. En Slack, escribe: `/invite @Jenkins` en el canal
5. Ejecuta el pipeline nuevamente

---

### Problema 5: "Build timeout en Quality Gate"

**Síntoma:**

```
ERROR: Timeout waiting for Quality Gate result
```

**Solución:**

1. El análisis de SonarQube puede tardar
2. Aumenta el timeout en el Jenkinsfile:

   ```groovy
   timeout(time: 10, unit: 'MINUTES') {  // Era 5, ahora 10
       waitForQualityGate()
   }
   ```

3. O ejecuta el análisis manualmente para ver el error:

   ```bash
   mvn clean verify sonar:sonar \
       -Dsonar.projectKey=vg-ms-users-rev \
       -Dsonar.host.url=http://localhost:9000 \
       -Dsonar.login=tu-token-aqui
   ```

---

## 📚 Recursos Adicionales

### URLs Importantes

| Servicio | URL | Credenciales |
|----------|-----|--------------|
| Jenkins | <http://localhost:8080> | admin / admin |
| SonarQube | <http://localhost:9000> | admin / admin |
| Slack Workspace | fatama.slack.com | - |
| Canal Slack | #jenkins-notifications | - |

### Archivos del Proyecto

- `Jenkinsfile-SonarQube` - Pipeline de análisis
- `setup-jenkins-sonarqube.sh` - Script de configuración automática
- `pom.xml` - Configuración de Maven con SonarQube

### Comandos Útiles

```bash
# Ver logs de SonarQube
docker logs -f sonarqube

# Ver logs de Jenkins
docker logs -f jenkins

# Reiniciar servicios
docker compose restart sonarqube jenkins

# Ejecutar análisis localmente
mvn clean verify sonar:sonar \
    -Dsonar.projectKey=vg-ms-users-rev \
    -Dsonar.host.url=http://localhost:9000 \
    -Dsonar.login=tu-token

# Ver cobertura localmente
mvn clean test jacoco:report
open target/site/jacoco/index.html
```

---

## ✅ Checklist Final

Antes de dar por completada la configuración, verifica:

- [ ] SonarQube está corriendo y accesible en <http://localhost:9000>
- [ ] Proyecto creado en SonarQube con key: `vg-ms-users-rev`
- [ ] Token de SonarQube generado y guardado
- [ ] Plugin de SonarQube instalado en Jenkins
- [ ] SonarQube Server configurado en Jenkins
- [ ] Credencial `sonarqube-token` creada en Jenkins
- [ ] Credencial `Slack-Tokencito` configurada
- [ ] Job `vg-ms-users-sonarqube` creado en Jenkins
- [ ] Pipeline ejecutado exitosamente al menos una vez
- [ ] Notificación recibida en Slack
- [ ] Dashboard de SonarQube muestra métricas del proyecto

---

## 🎯 Próximos Pasos

Una vez que todo esté configurado:

1. **Configura CI/CD completo:**
   - Agrega el análisis de SonarQube al pipeline principal
   - Configura Quality Gates personalizados
   - Bloquea merges si el Quality Gate falla

2. **Mejora la calidad del código:**
   - Revisa y arregla los Code Smells
   - Aumenta la cobertura de tests al 80%+
   - Revisa los Security Hotspots

3. **Automatiza más:**
   - Configura análisis automático en Pull Requests
   - Agrega badges de SonarQube al README
   - Configura reportes periódicos en Slack

---

## 🆘 Soporte

Si tienes problemas:

1. 📖 Revisa la sección [Solución de Problemas](#solución-de-problemas-comunes)
2. 🔍 Revisa los logs: `docker logs -f jenkins` y `docker logs -f sonarqube`
3. 📝 Revisa la documentación oficial:
   - [SonarQube Docs](https://docs.sonarqube.org/)
   - [Jenkins Pipeline Docs](https://www.jenkins.io/doc/book/pipeline/)

---

**🎉 ¡Listo! Ahora tienes análisis de calidad de código automatizado con SonarQube y Jenkins.**

*Última actualización: 31 de Octubre de 2025*
