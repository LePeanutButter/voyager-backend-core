package com.tourism.platform.service;

import com.tourism.platform.dto.UserDto;

public interface MicrosoftAuthService {
    UserDto authenticateWithAuthorizationCode(String code);
}

