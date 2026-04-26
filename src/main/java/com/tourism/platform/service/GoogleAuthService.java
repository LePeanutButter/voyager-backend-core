package com.tourism.platform.service;

import com.tourism.platform.dto.UserDto;

public interface GoogleAuthService {
    UserDto authenticateWithAuthorizationCode(String code);
}

