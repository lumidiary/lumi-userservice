package com.example.userservice.config;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class OciStorageConfig {

    @Bean
    public ObjectStorage objectStorage(
            @Value("${oci.config.file:${user.home}/.oci/config}") String configFile,
            @Value("${oci.config.profile:DEFAULT}") String profile
    ) throws Exception {
        // ~/.oci/config 파일에서 설정 읽기
        ConfigFileReader.ConfigFile config = ConfigFileReader.parse(configFile, profile);
        log.debug("Loaded OCI Config [tenancy={} user={} region={}]",
                config.get("tenancy"), config.get("user"), config.get("region"));
        AuthenticationDetailsProvider provider =
                new ConfigFileAuthenticationDetailsProvider(config);
        // ObjectStorageClient 생성
        return ObjectStorageClient.builder()
                .region(Region.fromRegionCode(config.get("region")))
                .build(provider);
    }
}
