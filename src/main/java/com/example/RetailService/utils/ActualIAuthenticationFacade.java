package com.example.RetailService.utils;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class ActualIAuthenticationFacade implements IAuthenticationFacade{

    @Override
    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Override
    public void setAuthentication(Authentication authentication) {

    }
}
