# 🧪 Jenkins Pipeline - Validación de Pruebas Unitarias

Este documento describe cómo configurar y ejecutar el pipeline de Jenkins dedicado específicamente a la **validación de pruebas unitarias** del microservicio `vg-ms-users`.

---

## 📋 Tabla de Contenidos

1. [Requisitos Previos](#requisitos-previos)
2. [Configuración del Pipeline en Jenkins](#configuración-del-pipeline-en-jenkins)
3. [Parámetros del Pipeline](#parámetros-del-pipeline)
4. [Etapas del Pipeline](#etapas-del-pipeline)
5. [Ejecución del Pipeline](#ejecución-del-pipeline)
6. [Visualización de Resultados](#visualización-de-resultados)
7. [Solución de Problemas](#solución-de-problemas)

---

## ✅ Requisitos Previos

Antes de configurar el pipeline, asegúrate de tener:

- ✅ Jenkins corriendo en `http://localhost:8080`
- ✅ Contenedor Docker de Jenkins levantado (`docker-compose up -d`)
- ✅ Plugins de Jenkins instalados:
  - Pipeline
  - Git
  - JUnit
  - JaCoCo
  - Maven Integration

### Verificar que Jenkins está corriendo

```bash
docker ps --filter "name=jenkins"
```

Deberías ver algo como:

```
NAMES     STATUS                    PORTS
jenkins   Up X minutes (healthy)    0.0.0.0:8080->8080/tcp
```

---

## 🔧 Configuración del Pipeline en Jenkins

### Paso 1: Acceder a Jenkins

1. Abre tu navegador en: **<http://localhost:8080>**
2. Inicia sesión con tus credenciales

### Paso 2: Obtener la contraseña inicial (primera vez)

Si es la primera vez que accedes a Jenkins:

```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

### Paso 3: Crear un Nuevo Pipeline

1. En el dashboard de Jenkins, haz clic en **"New Item"** (Nueva tarea)
2. Ingresa el nombre: **`vg-ms-users-unit-tests`**
3. Selecciona **"Pipeline"**
4. Haz clic en **"OK"**

### Paso 4: Configurar el Pipeline

En la página de configuración del job:

#### 📝 General

- ✅ Marca **"This project is parameterized"**
- Agrega los parámetros (ver sección siguiente)

#### 📝 Pipeline Definition

1. En **"Pipeline"** → **"Definition"**: Selecciona **"Pipeline script from SCM"**
2. En **"SCM"**: Selecciona **"Git"**
3. En **"Repository URL"**: Ingresa la URL de tu repositorio

   ```
   https://github.com/IsaelFatamaDev/vg-ms-users-rev.git
   ```

4. En **"Branch Specifier"**: `*/main`
5. En **"Script Path"**: `Jenkinsfile-UnitTests`
6. Haz clic en **"Save"**

#### 📝 Configurar Herramientas

Antes de ejecutar el pipeline, configura Maven y JDK en Jenkins:

1. Ve a **"Manage Jenkins"** → **"Global Tool Configuration"**

2. **Configurar JDK:**
   - Sección **"JDK"** → Clic en **"Add JDK"**
   - Name: `JDK-17`
   - ✅ Marca **"Install automatically"**
   - Selecciona **"Install from adoptium.net"**
   - Version: **"jdk-17.0.x+x"** (última disponible)

3. **Configurar Maven:**
   - Sección **"Maven"** → Clic en **"Add Maven"**
   - Name: `Maven-3.9`
   - ✅ Marca **"Install automatically"**
   - Version: **"3.9.9"** (o la más reciente)

4. Haz clic en **"Save"**

---

## 🎛️ Parámetros del Pipeline

El pipeline acepta los siguientes parámetros configurables:

| Parámetro | Tipo | Descripción | Valores |
|-----------|------|-------------|---------|
| **TEST_SCOPE** | Choice | Alcance de las pruebas a ejecutar | `ALL_TESTS`, `USER_SERVICE_ONLY`, `SPECIFIC_TEST` |
| **SPECIFIC_TEST_CLASS** | String | Clase de prueba específica | `UserServiceImplTest` (default) |

### Opciones de TEST_SCOPE

1. **ALL_TESTS**: Ejecuta todas las pruebas unitarias del proyecto

   ```bash
   mvn test
   ```

2. **USER_SERVICE_ONLY**: Ejecuta solo las pruebas de `UserServiceImplTest`

   ```bash
   mvn test -Dtest=UserServiceImplTest
   ```

3. **SPECIFIC_TEST**: Ejecuta una clase de prueba específica

   ```bash
   mvn test -Dtest=<SPECIFIC_TEST_CLASS>
   ```

---

## 🔄 Etapas del Pipeline

El pipeline consta de las siguientes etapas:

### 1️⃣ **Checkout** 📦

- Clona el código fuente del repositorio
- Muestra información del branch y commit

### 2️⃣ **Environment Info** 🔧

- Muestra versiones de Java y Maven
- Verifica el ambiente de ejecución

### 3️⃣ **Compile** 🔨

- Compila el proyecto con Maven
- Comando: `mvn clean compile -DskipTests`

### 4️⃣ **Run Unit Tests** 🧪

- Ejecuta las pruebas unitarias según el parámetro `TEST_SCOPE`
- Genera reportes XML en `target/surefire-reports/`

### 5️⃣ **Display Test Results** 📋

- Muestra un resumen de las pruebas ejecutadas
- Estadísticas: Total, Fallidas, Errores, Omitidas

### 6️⃣ **Code Coverage Report** 📊

- Genera reporte de cobertura con JaCoCo
- Ubicación: `target/site/jacoco/index.html`

### 7️⃣ **Publish Test Reports** 📤

- Publica reportes JUnit en Jenkins
- Publica métricas de cobertura de JaCoCo
- Establece umbrales mínimos de cobertura (50%)

### 8️⃣ **Archive Artifacts** 📦

- Archiva reportes de pruebas y cobertura
- Disponibles para descarga posterior

---

## ▶️ Ejecución del Pipeline

### Opción 1: Ejecutar desde la Interfaz Web

1. Ve al job **"vg-ms-users-unit-tests"**
2. Haz clic en **"Build with Parameters"**
3. Selecciona los parámetros deseados:
   - **TEST_SCOPE**: Elige el alcance (recomendado: `USER_SERVICE_ONLY`)
   - **SPECIFIC_TEST_CLASS**: Deja `UserServiceImplTest` (si aplica)
4. Haz clic en **"Build"**

### Opción 2: Ejecutar desde CLI (usando Jenkins CLI)

```bash
# Descargar Jenkins CLI
wget http://localhost:8080/jnlpJars/jenkins-cli.jar

# Ejecutar el job con parámetros
java -jar jenkins-cli.jar -s http://localhost:8080/ \
  -auth admin:password \
  build vg-ms-users-unit-tests \
  -p TEST_SCOPE=USER_SERVICE_ONLY \
  -p SPECIFIC_TEST_CLASS=UserServiceImplTest
```

### Opción 3: Trigger Automático (Webhook)

Puedes configurar un webhook de Git para ejecutar el pipeline automáticamente en cada push:

1. En la configuración del job → **"Build Triggers"**
2. Marca **"GitHub hook trigger for GITScm polling"**
3. Configura el webhook en GitHub apuntando a:

   ```
   http://localhost:8080/github-webhook/
   ```

---

## 📊 Visualización de Resultados

### Resultados de Pruebas Unitarias

1. Después de ejecutar el build, ve al job
2. Haz clic en el número de build (ej: **#1**)
3. En la barra lateral, verás:

   - **Console Output**: Logs completos de ejecución
   - **Test Result**: Resumen de pruebas JUnit
   - **Code Coverage**: Reporte de cobertura JaCoCo

### Ejemplo de Resultado Exitoso

```
╔════════════════════════════════════════╗
║     RESUMEN FINAL DE PRUEBAS          ║
╠════════════════════════════════════════╣
║ Total:    6                            ║
║ Exitosos: 6                            ║
║ Fallidos: 0                            ║
║ Omitidos: 0                            ║
╚════════════════════════════════════════╝

✅✅✅ PIPELINE EXITOSO ✅✅✅
```

### Detalles de las Pruebas Ejecutadas

Las **5 pruebas principales** de `UserServiceImplTest` que se validan:

1. ✅ **Test 1**: Listar Usuarios Activos
   - Verifica que se listan 2 usuarios activos (Juan Pérez, María Gómez)

2. ✅ **Test 2**: Crear Nuevo Usuario
   - Crea usuario "Pedro Sánchez" y verifica código USR-004

3. ✅ **Test 3**: Buscar Usuario por ID
   - Encuentra usuario por ID "user-001" (Juan Pérez)

4. ✅ **Test 4**: Eliminar Usuario Lógicamente
   - Soft delete: Cambia estado a INACTIVE y setea deletedAt

5. ✅ **Test 5**: Restaurar Usuario Eliminado
   - Restaura usuario Carlos Díaz: INACTIVE → ACTIVE

### Métricas de Cobertura

- **Instrucciones**: ≥ 50%
- **Branches**: ≥ 40%
- **Líneas**: ≥ 50%
- **Métodos**: ≥ 50%
- **Clases**: ≥ 50%

---

## 🔍 Monitoreo en Tiempo Real

### Ver Console Output en Tiempo Real

```bash
# Obtener el último build number
BUILD_NUMBER=$(curl -s http://localhost:8080/job/vg-ms-users-unit-tests/lastBuild/buildNumber)

# Ver logs en tiempo real
curl -s http://localhost:8080/job/vg-ms-users-unit-tests/${BUILD_NUMBER}/consoleText
```

### Ver Resultados de Tests

```bash
# Ver resultado de tests en JSON
curl -s http://localhost:8080/job/vg-ms-users-unit-tests/lastBuild/testReport/api/json
```

---

## 🐛 Solución de Problemas

### Problema 1: "Maven not found"

**Solución:**

```bash
# Verificar que Maven está instalado en el contenedor
docker exec jenkins mvn -version

# Si no está instalado, configurar en Jenkins Global Tool Configuration
```

### Problema 2: "JDK 17 not found"

**Solución:**

1. Ve a **"Manage Jenkins"** → **"Global Tool Configuration"**
2. Agrega JDK-17 con instalación automática desde Adoptium

### Problema 3: "Tests failing"

**Solución:**

```bash
# Ejecutar tests localmente para verificar
mvn test -Dtest=UserServiceImplTest

# Ver logs detallados en Jenkins Console Output
```

### Problema 4: "Permission denied"

**Solución:**

```bash
# Dar permisos al workspace de Jenkins
docker exec -u root jenkins chmod -R 777 /var/jenkins_home/workspace
```

### Problema 5: "Cannot connect to Git repository"

**Solución:**

1. Verifica que la URL del repositorio es correcta
2. Si es repositorio privado, agrega credenciales en Jenkins:
   - **"Manage Jenkins"** → **"Credentials"**
   - Agrega GitHub username/token

---

## 📈 Mejores Prácticas

### 1. Ejecutar el Pipeline Regularmente

- ✅ Antes de cada merge a `main`
- ✅ Después de cada feature completado
- ✅ Diariamente (scheduled build)

### 2. Configurar Scheduled Build

En la configuración del job → **"Build Triggers"**:

```
# Ejecutar todos los días a las 2 AM
H 2 * * *

# Ejecutar cada 4 horas
H */4 * * *
```

### 3. Notificaciones

Configura notificaciones por email o Slack cuando:

- ❌ El build falla
- ⚠️ La cobertura cae por debajo del 50%
- ✅ Después de 5 builds exitosos consecutivos

---

## 🎯 Validación Manual desde Terminal

Si quieres ejecutar las pruebas manualmente en tu máquina local:

```bash
# Navegar al proyecto
cd /workspaces/vg-ms-users-rev

# Ejecutar todas las pruebas
mvn test

# Ejecutar solo UserServiceImplTest
mvn test -Dtest=UserServiceImplTest

# Generar reporte de cobertura
mvn jacoco:report

# Ver reporte en el navegador
"$BROWSER" target/site/jacoco/index.html
```

---

## 📚 Recursos Adicionales

- [Jenkins Pipeline Documentation](https://www.jenkins.io/doc/book/pipeline/)
- [JUnit Plugin](https://plugins.jenkins.io/junit/)
- [JaCoCo Plugin](https://plugins.jenkins.io/jacoco/)
- [Maven Integration](https://www.jenkins.io/doc/book/installing/maven/)

---

## ✅ Checklist de Validación

Antes de considerar el pipeline como funcional, verifica:

- [ ] Jenkins está corriendo y accesible en <http://localhost:8080>
- [ ] JDK-17 y Maven-3.9 están configurados en Global Tool Configuration
- [ ] El job `vg-ms-users-unit-tests` está creado
- [ ] El Jenkinsfile-UnitTests está en el repositorio
- [ ] Se puede ejecutar el build con parámetros
- [ ] Los reportes JUnit se publican correctamente
- [ ] El reporte de cobertura JaCoCo se genera
- [ ] Los 6 tests de UserServiceImplTest pasan exitosamente
- [ ] El Console Output muestra los logs detallados con datos mock

---

## 🎉 ¡Listo

Tu pipeline de validación de pruebas unitarias está configurado y listo para usar. Cada vez que ejecutes el build, Jenkins validará automáticamente que todas las pruebas pasen y generará reportes detallados.

**Comando rápido para ejecutar el pipeline:**

```bash
# Desde tu terminal local
curl -X POST http://localhost:8080/job/vg-ms-users-unit-tests/buildWithParameters \
  --user admin:password \
  --data TEST_SCOPE=USER_SERVICE_ONLY
```

¡Disfruta de tu CI/CD automatizado! 🚀
