# 🔄 Pipeline de Validación de Pruebas Unitarias

## 📊 Estructura del Pipeline

```
┌──────────────────────────────────────────────────────────────────┐
│                    JENKINS UNIT TESTS PIPELINE                   │
│                     vg-ms-users-unit-tests                       │
└──────────────────────────────────────────────────────────────────┘

┌─────────────────┐
│  1. CHECKOUT    │  📦 Clonar código del repositorio
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 2. ENV INFO     │  🔧 Verificar Java 17 y Maven 3.9
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  3. COMPILE     │  🔨 mvn clean compile -DskipTests
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 4. RUN TESTS    │  🧪 Ejecutar pruebas según parámetro TEST_SCOPE
└────────┬────────┘     ├─ ALL_TESTS → mvn test
         │              ├─ USER_SERVICE_ONLY → mvn test -Dtest=UserServiceImplTest
         │              └─ SPECIFIC_TEST → mvn test -Dtest=<clase>
         ▼
┌─────────────────┐
│ 5. DISPLAY      │  📋 Mostrar resumen de resultados
│    RESULTS      │     ✅ Tests ejecutados, fallidos, errores
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 6. COVERAGE     │  📊 Generar reporte JaCoCo
│    REPORT       │     target/site/jacoco/index.html
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 7. PUBLISH      │  📤 Publicar reportes en Jenkins
│    REPORTS      │     ├─ JUnit Test Results
└────────┬────────┘     └─ JaCoCo Code Coverage
         │
         ▼
┌─────────────────┐
│ 8. ARCHIVE      │  📦 Archivar artefactos
│    ARTIFACTS    │     ├─ target/surefire-reports/**
└────────┬────────┘     └─ target/site/jacoco/**
         │
         ▼
┌─────────────────┐
│  POST ACTIONS   │  🎯 Acciones finales
└─────────────────┘     ├─ SUCCESS: ✅ Notificar éxito
                        ├─ FAILURE: ❌ Notificar fallo
                        └─ ALWAYS: 🧹 Mostrar resumen
```

---

## 🧪 Pruebas que se Validan

### Test Suite: UserServiceImplTest

```
📦 UserServiceImplTest (6 tests)
│
├─ ✅ Test 1: testListarUsuariosActivos_DebeRetornarListaDeUsuarios
│   └─ Valida: Lista 2 usuarios activos (Juan Pérez, María Gómez)
│
├─ ✅ Test 2: testCrearUsuario_DebeRetornarUsuarioCreado
│   └─ Valida: Crear usuario "Pedro Sánchez" con código USR-004
│
├─ ✅ Test 3: testBuscarUsuarioPorId_DebeRetornarUsuario
│   └─ Valida: Buscar usuario por ID "user-001"
│
├─ ✅ Test 4: testEliminarUsuarioLogicamente_DebeCambiarEstadoAInactivo
│   └─ Valida: Soft delete (ACTIVE → INACTIVE, setear deletedAt)
│
├─ ✅ Test 5: testRestaurarUsuarioEliminado_DebeCambiarEstadoAActivo
│   └─ Valida: Restaurar usuario (INACTIVE → ACTIVE, limpiar deletedAt)
│
└─ ✅ Test 6: testBuscarUsuarioPorIdInexistente_DebeRetornarError
    └─ Valida: Manejo de errores para ID inexistente
```

---

## 📈 Métricas de Calidad

### Umbrales Configurados en el Pipeline

| Métrica | Umbral Mínimo | Descripción |
|---------|---------------|-------------|
| 📊 Instrucciones | ≥ 50% | Porcentaje de instrucciones ejecutadas |
| 🌿 Branches | ≥ 40% | Porcentaje de ramas de decisión cubiertas |
| 📝 Líneas | ≥ 50% | Porcentaje de líneas de código ejecutadas |
| 🔧 Métodos | ≥ 50% | Porcentaje de métodos cubiertos |
| 📦 Clases | ≥ 50% | Porcentaje de clases cubiertas |

---

## 🎛️ Parámetros de Ejecución

### Opción 1: ALL_TESTS

```bash
Ejecuta TODAS las pruebas del proyecto
Comando: mvn test
Total: ~30 tests
Duración: ~15s
```

### Opción 2: USER_SERVICE_ONLY ⭐ (Recomendado)

```bash
Ejecuta solo las pruebas de UserServiceImplTest
Comando: mvn test -Dtest=UserServiceImplTest
Total: 6 tests
Duración: ~3s
```

### Opción 3: SPECIFIC_TEST

```bash
Ejecuta una clase de prueba específica
Comando: mvn test -Dtest=<SPECIFIC_TEST_CLASS>
Total: Variable
Duración: Variable
```

---

## 📤 Artefactos Generados

```
workspace/
├── target/
│   ├── surefire-reports/          📋 Reportes XML de JUnit
│   │   ├── TEST-*.xml
│   │   └── *.txt
│   │
│   ├── site/jacoco/                📊 Reporte de cobertura
│   │   ├── index.html              (Ver en navegador)
│   │   ├── jacoco.xml
│   │   └── jacoco.csv
│   │
│   └── jacoco.exec                 🔍 Datos de ejecución
```

---

## 🚀 Inicio Rápido

### Método 1: Script Automatizado (Recomendado)

```bash
# Ejecutar script de configuración
./setup-jenkins-unit-tests.sh

# El script:
# ✅ Verifica que Jenkins está corriendo
# ✅ Obtiene credenciales automáticamente
# ✅ Crea el pipeline en Jenkins
# ✅ Opcionalmente ejecuta el primer build
```

### Método 2: Manual

```bash
# 1. Acceder a Jenkins
open http://localhost:8080

# 2. Crear nuevo job
New Item → Pipeline → "vg-ms-users-unit-tests"

# 3. Configurar
Pipeline from SCM → Git
Repository: https://github.com/IsaelFatamaDev/vg-ms-users-rev.git
Script Path: Jenkinsfile-UnitTests

# 4. Ejecutar
Build with Parameters → TEST_SCOPE=USER_SERVICE_ONLY → Build
```

### Método 3: API REST

```bash
# Ejecutar build via curl
curl -X POST 'http://localhost:8080/job/vg-ms-users-unit-tests/buildWithParameters?TEST_SCOPE=USER_SERVICE_ONLY' \
  --user 'admin:password'

# Ver logs en tiempo real
curl -s http://localhost:8080/job/vg-ms-users-unit-tests/lastBuild/consoleText
```

---

## 📊 Ejemplo de Salida Exitosa

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

════════════════════════════════════════
    ✅ BUILD SUCCESS
════════════════════════════════════════
Proyecto: vg-ms-users-unit-tests
Build: #1
Duración: 14.2s
Alcance: USER_SERVICE_ONLY
════════════════════════════════════════
```

---

## 🔗 Enlaces Útiles

| Recurso | URL |
|---------|-----|
| 🏠 Jenkins Dashboard | <http://localhost:8080> |
| 🔧 Pipeline Job | <http://localhost:8080/job/vg-ms-users-unit-tests> |
| 📋 Last Build Console | <http://localhost:8080/job/vg-ms-users-unit-tests/lastBuild/console> |
| 📊 Test Results | <http://localhost:8080/job/vg-ms-users-unit-tests/lastBuild/testReport> |
| 📈 Code Coverage | <http://localhost:8080/job/vg-ms-users-unit-tests/lastBuild/jacoco> |

---

## 🎯 Checklist de Validación

Antes de dar por terminado el pipeline, verifica:

- [ ] ✅ Jenkins está corriendo (`docker ps | grep jenkins`)
- [ ] ✅ JDK-17 configurado en Jenkins Global Tool Configuration
- [ ] ✅ Maven-3.9 configurado en Jenkins Global Tool Configuration
- [ ] ✅ Job `vg-ms-users-unit-tests` creado
- [ ] ✅ Jenkinsfile-UnitTests existe en el repositorio
- [ ] ✅ Build #1 ejecutado exitosamente
- [ ] ✅ 6 tests de UserServiceImplTest pasaron
- [ ] ✅ Reportes JUnit publicados
- [ ] ✅ Reporte JaCoCo generado
- [ ] ✅ Console output muestra logs detallados con datos mock

---

## 💡 Tips y Mejores Prácticas

### 1. Ejecutar antes de cada merge

```bash
# Pre-merge hook
git push origin feature/mi-feature
# Esperar a que Jenkins valide las pruebas
# Si pasa ✅ → Hacer merge
# Si falla ❌ → Revisar y corregir
```

### 2. Configurar build programado

```groovy
// En configuración del job → Build Triggers
// Ejecutar diariamente a las 2 AM
H 2 * * *
```

### 3. Integrar con GitHub

```bash
# Configurar webhook en GitHub
URL: http://localhost:8080/github-webhook/
Events: Push, Pull Request
```

### 4. Notificaciones

```groovy
// En Jenkinsfile, agregar:
post {
    failure {
        slackSend(channel: '#builds', message: "❌ Tests fallaron: ${env.BUILD_URL}")
    }
}
```

---

