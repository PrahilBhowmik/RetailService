package com.example.RetailService.testUtils;

import com.example.RetailService.utils.IAuthenticationFacade;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class TestIAuthenticationFacade implements IAuthenticationFacade {

    private Authentication authentication;

    @Override
    public Authentication getAuthentication() {
        return this.authentication;
    }

    @Override
    public void setAuthentication(Authentication authentication) {
        this.authentication=authentication;
    }
}
