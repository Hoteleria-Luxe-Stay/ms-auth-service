# Auth Service - Sistema de Reservas de Hoteles

Microservicio de autenticación y autorización basado en **JWT**. Gestiona usuarios, roles, login, registro y validación de tokens.

## Información del Servicio

| Propiedad | Valor |
|-----------|-------|
| Puerto | 8081 |
| Java | 21 |
| Spring Boot | 3.4.0 |
| Spring Cloud | 2024.0.1 |
| Context Path | /api/v1 |
| Base de Datos | MySQL |

## Estructura del Proyecto

```
ms-auth/
├── pom.xml
├── contracts/
│   └── auth-service-api.yaml
└── src/main/
    ├── java/com/hotel/auth/
    │   ├── AuthServiceApplication.java
    │   ├── application/
    │   │   ├── mapper/AuthMapper.java
    │   │   └── service/
    │   │       ├── AuthServiceImpl.java
    │   │       ├── TokenServiceImpl.java
    │   │       └── UserServiceImpl.java
    │   ├── domain/
    │   │   ├── model/ (User, Role)
    │   │   ├── repository/
    │   │   └── service/
    │   ├── infrastructure/
    │   │   ├── config/ (SecurityConfig, RabbitConfig)
    │   │   ├── controllers/AuthController.java
    │   │   └── filters/JwtAuthenticationFilter.java
    │   └── helpers/
    │       ├── DataInit.java
    │       └── exceptions/
    └── resources/
        └── application.yml
```

## Endpoints

### Autenticación (Públicos)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Registrar nuevo usuario |
| POST | `/api/v1/auth/login` | Iniciar sesión |
| POST | `/api/v1/auth/refresh` | Refrescar token |
| POST | `/api/v1/auth/validate` | Validar token (interno) |

### Usuarios (Protegidos - JWT)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v1/users/me` | Obtener usuario actual |
| GET | `/api/v1/users/{id}` | Obtener usuario por ID |

## Variables de Entorno

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `SERVER_PORT` | Puerto del servicio | `8081` |
| `SPRING_DATASOURCE_URL` | URL MySQL | `jdbc:mysql://mysql:3306/auth_db` |
| `SPRING_DATASOURCE_USERNAME` | Usuario BD | `hotel_user` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña BD | `hotel_pass` |
| `SPRING_RABBITMQ_HOST` | Host RabbitMQ | `rabbitmq` |
| `SPRING_RABBITMQ_PORT` | Puerto RabbitMQ | `5672` |
| `SPRING_RABBITMQ_USERNAME` | Usuario RabbitMQ | `guest` |
| `SPRING_RABBITMQ_PASSWORD` | Contraseña RabbitMQ | `guest` |
| `JWT_SECRET_KEY` | Clave secreta JWT (256 bits) | `mi-clave-secreta...` |
| `JWT_EXPIRATION` | Expiración token (ms) | `86400000` |
| `JWT_REFRESH_EXPIRATION` | Expiración refresh (ms) | `604800000` |
| `EUREKA_URL` | URL Eureka | `http://discovery-service:8761/eureka` |
| `CONFIG_SERVER_URL` | URL Config Server | `http://config-server:8888` |

---

## Docker

### Dockerfile

```dockerfile
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
```

### docker-compose.yml

```yaml
version: '3.8'

services:
  auth-service:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: auth-service
    ports:
      - "8081:8081"
    environment:
      - SERVER_PORT=8081
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/auth_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
      - SPRING_DATASOURCE_USERNAME=hotel_user
      - SPRING_DATASOURCE_PASSWORD=hotel_pass
      - SPRING_RABBITMQ_HOST=rabbitmq
      - SPRING_RABBITMQ_PORT=5672
      - SPRING_RABBITMQ_USERNAME=guest
      - SPRING_RABBITMQ_PASSWORD=guest
      - JWT_SECRET_KEY=tu-clave-secreta-muy-segura-de-256-bits-minimo-para-hs256
      - JWT_EXPIRATION=86400000
      - JWT_REFRESH_EXPIRATION=604800000
      - EUREKA_URL=http://discovery-service:8761/eureka
      - CONFIG_SERVER_URL=http://config-server:8888
      - JAVA_OPTS=-Xms256m -Xmx512m
    depends_on:
      mysql:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      config-server:
        condition: service_healthy
      discovery-service:
        condition: service_healthy
    networks:
      - hotel-network
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8081/api/v1/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 90s
    restart: unless-stopped

networks:
  hotel-network:
    external: true
```

### Comandos Docker

```bash
# Compilar
mvn clean package -DskipTests

# Construir imagen
docker build -t auth-service:latest .

# Ejecutar
docker run -d \
  --name auth-service \
  -p 8081:8081 \
  -e SERVER_PORT=8081 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/auth_db \
  -e SPRING_DATASOURCE_USERNAME=hotel_user \
  -e SPRING_DATASOURCE_PASSWORD=hotel_pass \
  -e JWT_SECRET_KEY=tu-clave-secreta-muy-segura-256-bits \
  -e EUREKA_URL=http://discovery-service:8761/eureka \
  --network hotel-network \
  auth-service:latest

# Verificar
curl http://localhost:8081/api/v1/actuator/health

# Test login
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@hotel.com","password":"admin123"}'
```

---

## Kubernetes

### Deployment

```yaml
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
  namespace: hotel-system
  labels:
    app: auth-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: auth-service
  template:
    metadata:
      labels:
        app: auth-service
    spec:
      containers:
        - name: auth-service
          image: ${ACR_NAME}.azurecr.io/auth-service:latest
          ports:
            - containerPort: 8081
          env:
            - name: SERVER_PORT
              value: "8081"
            - name: SPRING_DATASOURCE_URL
              value: "jdbc:mysql://mysql:3306/auth_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
            - name: SPRING_DATASOURCE_USERNAME
              valueFrom:
                secretKeyRef:
                  name: hotel-secrets
                  key: mysql-user
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: hotel-secrets
                  key: mysql-password
            - name: SPRING_RABBITMQ_HOST
              value: "rabbitmq"
            - name: SPRING_RABBITMQ_PORT
              value: "5672"
            - name: JWT_SECRET_KEY
              valueFrom:
                secretKeyRef:
                  name: hotel-secrets
                  key: jwt-secret
            - name: JWT_EXPIRATION
              value: "86400000"
            - name: JWT_REFRESH_EXPIRATION
              value: "604800000"
            - name: EUREKA_URL
              value: "http://discovery-service:8761/eureka"
            - name: CONFIG_SERVER_URL
              value: "http://config-server:8888"
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /api/v1/actuator/health/liveness
              port: 8081
            initialDelaySeconds: 90
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /api/v1/actuator/health/readiness
              port: 8081
            initialDelaySeconds: 60
            periodSeconds: 5
```

### Service

```yaml
# k8s/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: auth-service
  namespace: hotel-system
spec:
  type: ClusterIP
  selector:
    app: auth-service
  ports:
    - port: 8081
      targetPort: 8081
      name: http
```

### Secret

```yaml
# k8s/secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: auth-service-secrets
  namespace: hotel-system
type: Opaque
stringData:
  jwt-secret: "tu-clave-secreta-muy-segura-de-256-bits-minimo-para-hs256"
```

### Comandos Kubernetes

```bash
# Aplicar manifiestos
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml

# Verificar
kubectl get pods -n hotel-system -l app=auth-service
kubectl logs -f deployment/auth-service -n hotel-system

# Port-forward para testing local
kubectl port-forward svc/auth-service 8081:8081 -n hotel-system

# Test
curl http://localhost:8081/api/v1/actuator/health
```

---

## Azure

### 1. Construir y Subir a ACR

```bash
# Variables
export ACR_NAME="acrhotelreservas"
export RESOURCE_GROUP="rg-hotel-reservas"

# Login
az acr login --name $ACR_NAME

# Build en ACR
az acr build \
  --registry $ACR_NAME \
  --image auth-service:v1.0.0 \
  --image auth-service:latest \
  .
```

### 2. Deployment en AKS

```yaml
# k8s/azure-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
  namespace: hotel-system
spec:
  replicas: 2
  selector:
    matchLabels:
      app: auth-service
  template:
    metadata:
      labels:
        app: auth-service
    spec:
      containers:
        - name: auth-service
          image: acrhotelreservas.azurecr.io/auth-service:v1.0.0
          ports:
            - containerPort: 8081
          env:
            - name: SERVER_PORT
              value: "8081"
            - name: SPRING_DATASOURCE_URL
              value: "jdbc:mysql://mysql-hotel-reservas.mysql.database.azure.com:3306/auth_db?useSSL=true&serverTimezone=UTC"
            - name: SPRING_DATASOURCE_USERNAME
              valueFrom:
                secretKeyRef:
                  name: hotel-secrets
                  key: mysql-user
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: hotel-secrets
                  key: mysql-password
            - name: JWT_SECRET_KEY
              valueFrom:
                secretKeyRef:
                  name: hotel-secrets
                  key: jwt-secret
            - name: EUREKA_URL
              value: "http://discovery-service:8761/eureka"
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "500m"
```

### 3. Azure DevOps Pipeline

```yaml
# azure-pipelines.yml
trigger:
  branches:
    include:
      - main
  paths:
    include:
      - ms-auth/**

variables:
  dockerRegistryServiceConnection: 'acr-connection'
  imageRepository: 'auth-service'
  containerRegistry: 'acrhotelreservas.azurecr.io'
  dockerfilePath: 'ms-auth/Dockerfile'
  tag: '$(Build.BuildId)'

pool:
  vmImage: 'ubuntu-latest'

stages:
  - stage: Build
    jobs:
      - job: Build
        steps:
          - task: Maven@3
            displayName: 'Maven Package'
            inputs:
              mavenPomFile: 'ms-auth/pom.xml'
              goals: 'clean package'
              options: '-DskipTests'
              javaHomeOption: 'JDKVersion'
              jdkVersionOption: '1.21'

          - task: Docker@2
            displayName: 'Build and Push'
            inputs:
              command: buildAndPush
              repository: $(imageRepository)
              dockerfile: $(dockerfilePath)
              containerRegistry: $(dockerRegistryServiceConnection)
              tags: |
                $(tag)
                latest

  - stage: Deploy
    dependsOn: Build
    jobs:
      - deployment: Deploy
        environment: 'production'
        strategy:
          runOnce:
            deploy:
              steps:
                - task: KubernetesManifest@0
                  inputs:
                    action: deploy
                    kubernetesServiceConnection: 'aks-connection'
                    namespace: hotel-system
                    manifests: |
                      ms-auth/k8s/*.yaml
                    containers: |
                      $(containerRegistry)/$(imageRepository):$(tag)
```

### 4. Desplegar

```bash
# Obtener credenciales AKS
az aks get-credentials --resource-group $RESOURCE_GROUP --name aks-hotel-reservas

# Crear secrets
kubectl create secret generic hotel-secrets \
  --namespace hotel-system \
  --from-literal=mysql-user="hotel_admin" \
  --from-literal=mysql-password='P@ssw0rd123!' \
  --from-literal=jwt-secret="tu-clave-secreta-muy-segura-256-bits"

# Aplicar
kubectl apply -f k8s/azure-deployment.yaml
kubectl apply -f k8s/service.yaml

# Verificar
kubectl get pods -n hotel-system -l app=auth-service
kubectl logs -f deployment/auth-service -n hotel-system
```

---

## Eventos RabbitMQ

El servicio publica eventos cuando:

| Evento | Routing Key | Trigger |
|--------|-------------|---------|
| UserRegisteredEvent | `user.registered` | Nuevo registro |
| UserLoginEvent | `user.login` | Login exitoso |

**Exchange:** `hotel.events` (TopicExchange)

---

## Seguridad

- **Algoritmo JWT:** HS256
- **Password Encoding:** BCrypt
- **Rutas Públicas:** `/auth/**`, `/swagger-ui/**`, `/api-docs/**`
- **Rutas Protegidas:** Todas las demás requieren Bearer token

---

## Datos Iniciales

Al iniciar, `DataInit` crea los roles si no existen:
- `USER` - Rol por defecto para nuevos usuarios
- `ADMIN` - Rol de administrador

---

## Troubleshooting

```bash
# Ver logs
kubectl logs -f deployment/auth-service -n hotel-system

# Verificar conexión BD
kubectl exec -it deployment/auth-service -n hotel-system -- \
  wget -qO- http://localhost:8081/api/v1/actuator/health

# Debug JWT
curl -X POST http://localhost:8081/api/v1/auth/validate \
  -H "Content-Type: application/json" \
  -d '{"token":"tu-jwt-token"}'
```

---

## Ejecución Local

```bash
# Compilar
mvn clean package -DskipTests

# Ejecutar
java -jar target/auth-service-1.0.0-SNAPSHOT.jar \
  --server.port=8081 \
  --spring.datasource.url=jdbc:mysql://localhost:3306/auth_db

# O con Maven
mvn spring-boot:run

# Swagger UI
open http://localhost:8081/api/v1/swagger-ui.html
```
