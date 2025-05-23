# 1) 베이스 이미지: Java 17 런타임 사용
FROM eclipse-temurin:17-jre

# 2) 작업 디렉토리 설정 (/app)
WORKDIR /app

# 3) 빌드된 JAR 파일 복사
COPY target/UserService-0.0.1-SNAPSHOT.jar user-service.jar

# 4) 컨테이너가 리스닝할 포트 노출
EXPOSE 8081

# 5) 컨테이너 시작 시 애플리케이션 실행
ENTRYPOINT ["java","-jar","user-service.jar"]