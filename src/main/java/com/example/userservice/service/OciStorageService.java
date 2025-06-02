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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Component
public class OciStorageService {
    private final ObjectStorage objectStorage;   // OCI Object Storage 클라이언트
    private final String namespace;              // application.yml: oci.objectstorage.namespace
    private final String bucket;                 // application.yml: oci.objectstorage.bucket
    private final String preAuthUrlPrefix;       // application.yml: oci.objectstorage.par-url-prefix

    @Autowired
    public OciStorageService(
            ObjectStorage objectStorage,
            @Value("${oci.objectstorage.namespace}") String namespace,
            @Value("${oci.objectstorage.bucket}") String bucket,
            @Value("${oci.objectstorage.par-url-prefix}") String preAuthUrlPrefix
    ) {
        this.objectStorage     = objectStorage;
        this.namespace         = namespace;
        this.bucket            = bucket;
        this.preAuthUrlPrefix  = preAuthUrlPrefix.endsWith("/")
                ? preAuthUrlPrefix
                : preAuthUrlPrefix + "/";  // 접미사 '/' 보장
    }

    /**
     * 기본 프로필 이미지 PAR URL 반환
     */
    public String getDefaultImageUrl() {
        // PAR prefix 뒤에 경로를 그대로 붙임 (인코딩 금지)
        return preAuthUrlPrefix + "default.png";
    }

    /**
     * 프로필 이미지 업로드 후, PAR URL 반환
     */
    public String uploadProfileImage(String userId, MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            // 확장자 추출
            String original = file.getOriginalFilename();
            String extension = (original != null && original.contains("."))
                    ? original.substring(original.lastIndexOf('.'))
                    : "";

            // 객체명 생성: profiles/{userId}/{randomUUID}{extension}
            String objectName = String.format("profiles/%s/%s%s",
                    userId, UUID.randomUUID(), extension);

            // 업로드 요청
            PutObjectRequest request = PutObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucket)
                    .objectName(objectName)
                    .contentLength(file.getSize())
                    .contentType(file.getContentType())
                    .putObjectBody(in)
                    .build();
            objectStorage.putObject(request);

            // PAR URL 반환 (슬래시 인코딩 없이 경로 그대로)
            return preAuthUrlPrefix + objectName;

        } catch (Exception e) {
            throw new RuntimeException("프로필 이미지 업로드 실패", e);
        }
    }
}
