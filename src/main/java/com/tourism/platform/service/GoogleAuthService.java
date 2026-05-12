package com.tourism.platform.service;

import com.tourism.platform.dto.UserDto;

public interface GoogleAuthService {
    /**
     * Authenticate a user using an OAuth2 authorization code obtained from Google.
     *
     * @param code authorization code returned by Google's OAuth2 flow
     * @return UserDto representing the authenticated or newly created user
     */
    UserDto authenticateWithAuthorizationCode(String code);

    /**
     * Same as {@link #authenticateWithAuthorizationCode(String)} but for codes issued to a native
     * client (Android/iOS server auth). Google's token endpoint expects an empty {@code redirect_uri}.
     */
    UserDto authenticateWithMobileServerAuthCode(String code);
}

