# ─────────────────────────────────────────
# Stage 1 — Build
# ─────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Copia o Maven wrapper e o pom.xml primeiro
# Docker cacheia essa camada — só recompila dependências se o pom mudar
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Baixa as dependências (camada cacheada)
RUN ./mvnw dependency:go-offline -q

# Copia o código fonte e compila
COPY src/ src/
RUN ./mvnw package -DskipTests -q

# ─────────────────────────────────────────
# Stage 2 — Runtime
# ─────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

# Usuário não-root por segurança
RUN addgroup -S gubee && adduser -S gubee -G gubee
USER gubee

# Copia apenas o jar gerado no stage anterior
COPY --from=build /app/target/*.jar app.jar

# Porta exposta
EXPOSE 8080

# Variáveis de ambiente com valores padrão
ENV SPRING_PROFILES_ACTIVE=default \
    JAVA_OPTS="-Xms256m -Xmx512m"

# Comando de inicialização
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]