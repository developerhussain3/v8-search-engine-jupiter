FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY . .
RUN javac JupiterServer.java
CMD ["java", "JupiterServer"]
