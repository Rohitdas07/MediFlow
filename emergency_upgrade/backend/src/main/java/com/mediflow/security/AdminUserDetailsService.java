package com.mediflow.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Provides the configured HIS administrator identity to the JWT filter. */
@Service
public class AdminUserDetailsService implements UserDetailsService {
    private final String adminId;
    private final String adminPin;

    public AdminUserDetailsService(
            @Value("${app.his-admin.id:HIS-01234}") String adminId,
            @Value("${app.his-admin.pin:01234}") String adminPin) {
        this.adminId = adminId;
        this.adminPin = adminPin;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!adminId.equalsIgnoreCase(username)) {
            throw new UsernameNotFoundException("HIS administrator not found");
        }
        return new AdminUserDetails(adminId, adminPin);
    }
}
