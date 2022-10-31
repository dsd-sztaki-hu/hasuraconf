/*
 * Copyright (c) 2020 SZTAKI, DSD - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited without the
 * prior written permission of SZTAKI.
 *
 * Authors: Balázs E. Pataki <balazs.pataki@sztaki.hu>, Zoltán Tóth <zoltan.toth@sztaki.hu>
 */
package com.beepsoft.hasuraconf.controllers;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * User registration and authentication endpoint.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController
{
    @PostMapping("/signin")
    public ResponseEntity<JwtAuthenticationResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        return ResponseEntity.ok(new JwtAuthenticationResponse("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"));
    }
}
