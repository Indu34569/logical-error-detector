FROM maven:3.9.9-eclipse-temurin-17

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
COPY test-input ./test-input

RUN mvn clean package -DskipTests

EXPOSE 10000

CMD ["sh", "-c", "java -cp target/classes:$(find ~/.m2/repository/com/github/javaparser/javaparser-core -name 'javaparser-core-*.jar' | head -1):$(find ~/.m2/repository/tools/aqua/z3-turnkey -name 'z3-turnkey-*.jar' | head -1) com.project.server.AnalysisServer"]