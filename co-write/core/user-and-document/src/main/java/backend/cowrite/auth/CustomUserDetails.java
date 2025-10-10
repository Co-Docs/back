package backend.cowrite.auth;

import backend.cowrite.service.response.UserCacheDto;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class CustomUserDetails implements UserDetails, OAuth2User {
    private final UserCacheDto user;
    private Map<String, Object> attributes;

    private CustomUserDetails(UserCacheDto user) {
        this.user = user;
    }

    public static CustomUserDetails create(UserCacheDto user) {
        return new CustomUserDetails(user);
    }

    public static CustomUserDetails create(UserCacheDto user, Map<String, Object> attributes) {
        CustomUserDetails userPrincipal = CustomUserDetails.create(user);
        userPrincipal.setAttributes(attributes);
        return userPrincipal;
    }

    public UserCacheDto getUser() {
        return user;
    }


    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(user.roleName().name()));
    }

    @Override
    public String getPassword() {
        return user.password();
    }

    @Override
    public String getName() {
        return user.nickname();
    }


    @Override
    public String getUsername() {
        return user.username();
    }
}
