/*
 * Copyright (c) 2020 SZTAKI, DSD - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited without the
 * prior written permission of SZTAKI.
 *
 * Authors: Balázs E. Pataki <balazs.pataki@sztaki.hu>, Zoltán Tóth <zoltan.toth@sztaki.hu>
 */
package com.beepsoft.hasuraconf.controllers;

/**
 * JWT token details after successful login
 */
public class JwtAuthenticationResponse {
    private String accessToken;
    private String tokenType = "Bearer";

    public JwtAuthenticationResponse(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}
