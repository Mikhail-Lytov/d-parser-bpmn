package com.lytov.diplom.dparser.external.sppr_bd;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(value = "sppr-bd", url = "${external.sppr.bd}")
public interface SpprBdConnector {
}
