package com.example.RetailService.configurations;


import com.example.RetailService.service.UserService;
import com.example.RetailService.utils.Utility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.DefaultServerRedirectStrategy;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;

@Component
@Profile("!test")
public class OAuth2LoginSuccessHandler implements ServerAuthenticationSuccessHandler {

    @Value("${url.frontend.home}")
    private String redirectUrl;

    @Value("${url.frontend.new-user}")
    private String addNewUserUrl;

    @Autowired
    private UserService userService;


    private final ServerRedirectStrategy redirectStrategy = new DefaultServerRedirectStrategy();

    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        ServerWebExchange exchange = webFilterExchange.getExchange();

        try {
            userService.getUser().onErrorResume(_ -> {
                redirectUrl = addNewUserUrl;
                return Mono.empty();
            }).subscribe();

            return this.redirectStrategy.sendRedirect(exchange, new URI(redirectUrl));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
