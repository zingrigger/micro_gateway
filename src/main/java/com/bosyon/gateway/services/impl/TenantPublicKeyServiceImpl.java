package com.bosyon.gateway.services.impl;

import com.bosyon.gateway.model.TenantInfo;
import com.bosyon.gateway.services.TenantPublicKeyService;
import org.springframework.stereotype.Service;

@Service
public class TenantPublicKeyServiceImpl implements TenantPublicKeyService {

    @Override
    public TenantInfo getTenantByKid(String kid) {
        return null;
    }
}
