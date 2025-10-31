# =============================================================================
# DOCKERFILE OPTIMIZADO - MS-USERS (250 MiB LIMIT)
# =============================================================================
FROM maven:3.9.0-eclipse-temurin-17-alpine AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

# Compilar con optimización de memoria
RUN mvn clean package -DskipTests -Dmaven.compiler.debug=false

FROM eclipse-temurin:17-jre-alpine

# Instalar curl para health checks
RUN apk add --no-cache curl

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8085

# =============================================================================
# OPTIMIZACIÓN AGRESIVA PARA 250 MiB
# =============================================================================
# Distribución de memoria:
# - Heap: 150 MiB (suficiente para operaciones CRUD + caché)
# - Metaspace: 70 MiB (reducido, solo lo esencial)
# - Stack: 228k por thread
# - Native: ~30 MiB para OS y buffers
# Total esperado: ~220-240 MiB en uso normal
# =============================================================================
ENTRYPOINT ["java", \
     # === MEMORIA HEAP (AGRESIVAMENTE OPTIMIZADO) === \
     "-Xms96m", \
     "-Xmx150m", \
     "-XX:MaxMetaspaceSize=70m", \
     "-XX:MetaspaceSize=48m", \
     "-XX:CompressedClassSpaceSize=20m", \
     "-Xss228k", \
     # === GARBAGE COLLECTOR SERIAL (MENOS OVERHEAD) === \
     "-XX:+UseSerialGC", \
     "-XX:MinHeapFreeRatio=10", \
     "-XX:MaxHeapFreeRatio=20", \
     "-XX:GCTimeRatio=9", \
     "-XX:AdaptiveSizePolicyWeight=90", \
     # === OPTIMIZACIONES DE CÓDIGO === \
     "-XX:+TieredCompilation", \
     "-XX:TieredStopAtLevel=1", \
     "-XX:+UseStringDeduplication", \
     "-XX:+UseCompressedOops", \
     "-XX:+UseCompressedClassPointers", \
     # === REDUCCIÓN AGRESIVA DE OVERHEAD === \
     "-Djava.security.egd=file:/dev/./urandom", \
     "-Dspring.jmx.enabled=false", \
     "-Dspring.main.lazy-initialization=true", \
     "-Dmanagement.metrics.enable.jvm=false", \
     "-Dmanagement.metrics.enable.process=false", \
     "-Dmanagement.metrics.enable.http=false", \
     "-Dmanagement.health.mongo.enabled=false", \
     "-Dlogging.pattern.console=%msg%n", \
     # === CONTAINER AWARENESS === \
     "-XX:+UseContainerSupport", \
     "-XX:MaxRAMPercentage=60.0", \
     "-XX:InitialRAMPercentage=40.0", \
     # === REACTOR NETTY OPTIMIZADO === \
     "-Dreactor.netty.ioWorkerCount=2", \
     "-Dreactor.netty.pool.maxConnections=50", \
     "-Dio.netty.allocator.numDirectArenas=1", \
     "-Dio.netty.allocator.numHeapArenas=1", \
     # === PERFIL PRODUCCIÓN === \
     "-Dspring.profiles.active=prod", \
     # === JAR === \
     "-jar", "app.jar"]
