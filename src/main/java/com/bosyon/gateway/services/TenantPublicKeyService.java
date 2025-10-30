package com.bosyon.gateway.services;

import com.bosyon.gateway.model.TenantInfo;

public interface TenantPublicKeyService {

    TenantInfo getTenantByKid(String kid);

}
