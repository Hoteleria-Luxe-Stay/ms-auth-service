# Dockerfile para Auth Service
# Multi-stage build para optimizar el tamano de la imagen

# ==================== STAGE 1: Build ====================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copiar archivos de configuracion de Maven primero (para cache de dependencias)
COPY pom.xml .

# Descargar dependencias (se cachea si pom.xml no cambia)
RUN mvn dependency:go-offline -B

# Copiar codigo fuente
COPY src ./src
COPY contracts ./contracts

# Compilar y empaquetar
RUN mvn clean package -DskipTests -B

# ==================== STAGE 2: Runtime ====================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Crear usuario no-root para seguridad
RUN addgroup -S spring && adduser -S spring -G spring

# Copiar JAR desde stage de build
COPY --from=builder /app/target/*.jar app.jar

# Cambiar a usuario no-root
USER spring:spring

# Puerto del servicio
EXPOSE 8081

# Variables de entorno por defecto
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV SPRING_PROFILES_ACTIVE=default

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/api/v1/actuator/health || exit 1

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
