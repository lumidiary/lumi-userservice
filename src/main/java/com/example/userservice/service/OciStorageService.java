package com.example.userservice.service;

import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.PutObjectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

@Component
public class OciStorageService {
    private final ObjectStorage objectStorage;
    private final String namespace;
    private final String bucket;
    private final String uriPrefix;

    @Autowired
    public OciStorageService(
            ObjectStorage objectStorage,
            @Value("${oci.objectstorage.namespace}") String namespace,
            @Value("${oci.objectstorage.bucket}") String bucket,
            @Value("${oci.objectstorage.uri-prefix}") String uriPrefix
    ) {
        this.objectStorage = objectStorage;
        this.namespace     = namespace;   // YAML 의 namespace
        this.bucket        = bucket;      // YAML 의 bucket
        this.uriPrefix     = uriPrefix;   // YAML 의 uri-prefix
    }

    public String getDefaultImageUrl() {
        return String.format("%s/n/%s/b/%s/o/default.png", uriPrefix, namespace, bucket);
    }

    public String uploadProfileImage(String userId, MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            // 파일 확장자 추출
            String original = file.getOriginalFilename();
            String extension = (original != null && original.contains("."))
                    ? original.substring(original.lastIndexOf('.')) : "";
            // 객체명 생성
            String objectName = String.format("profiles/%s/%s%s",
                    userId, UUID.randomUUID(), extension);

            // PutObject 요청 빌드
            PutObjectRequest request = PutObjectRequest.builder()
                    .namespaceName(namespace)   // application.yml 의 oci.objectstorage.namespace
                    .bucketName(bucket)         // oci.objectstorage.bucket
                    .objectName(objectName)
                    .contentLength(file.getSize())
                    .contentType(file.getContentType())
                    .putObjectBody(in)
                    .build();
            // 업로드 실행
            objectStorage.putObject(request);

            // 공개 URL 반환 (uriPrefix: oci.objectstorage.uri-prefix)
            return String.format("%s/n/%s/b/%s/o/%s",
                    uriPrefix, namespace, bucket, objectName);
        } catch (Exception e) {
            throw new RuntimeException("프로필 이미지 업로드 실패", e);
        }
    }
}