package com.lytov.diplom.dparser.external.sppr_bd;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

@FeignClient(value = "sppr-bd", url = "${external.sppr.bd}")
public interface SpprBdConnector {

    @PostMapping("/api/v0/minio/presign/get/{id}")
    String getDownloadUrl(@PathVariable("id") UUID id);
}
