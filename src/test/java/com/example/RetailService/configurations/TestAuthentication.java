package com.example.RetailService.configurations;

import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import javax.security.auth.Subject;
import java.util.Collection;
import java.util.List;

public class TestAuthentication implements Authentication {
    String email;

    public TestAuthentication(String email){
        this.email = email;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public boolean implies(Subject subject) {
        return Authentication.super.implies(subject);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        DefaultOidcUser oidcUser = Mockito.mock(DefaultOidcUser.class);
        Mockito.when(oidcUser.getEmail()).thenReturn(this.email);
        return oidcUser;
    }

    @Override
    public boolean isAuthenticated() {
        return false;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

    }
}
