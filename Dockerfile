FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY . .
RUN javac -cp ".:*" *.java
CMD ["java", "-cp", ".:*", "JupiterServer"]
