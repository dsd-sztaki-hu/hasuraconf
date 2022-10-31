/*
 * Copyright (c) 2020 SZTAKI, DSD - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited without the
 * prior written permission of SZTAKI.
 *
 * Authors: Balázs E. Pataki <balazs.pataki@sztaki.hu>, Zoltán Tóth <zoltan.toth@sztaki.hu>
 */
package com.beepsoft.hasuraconf.controllers;

import javax.validation.constraints.NotBlank;

/**
 * Data for login.
 */
public class LoginRequest {
    @NotBlank
    private String usernameOrEmail;

    @NotBlank
    private String password;

    public LoginRequest()
    {
    }

    public LoginRequest(String usernameOrEmail, String password)
    {
        this.usernameOrEmail = usernameOrEmail;
        this.password = password;
    }

    public String getUsernameOrEmail() {
        return usernameOrEmail;
    }

    public void setUsernameOrEmail(String usernameOrEmail) {
        this.usernameOrEmail = usernameOrEmail;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
