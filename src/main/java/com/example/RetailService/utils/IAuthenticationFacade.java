package com.example.RetailService.utils;

import org.springframework.security.core.Authentication;

public interface IAuthenticationFacade {
    Authentication getAuthentication();

    void  setAuthentication(Authentication authentication);
}
