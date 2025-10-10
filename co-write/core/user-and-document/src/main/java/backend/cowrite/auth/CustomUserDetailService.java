package backend.cowrite.auth;

import backend.cowrite.service.UserService;
import backend.cowrite.service.response.UserCacheDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final UserService userService;

    @Override
    public CustomUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserCacheDto cacheUser = userService.findByUsername(username);
        log.info("로그인 시 customUserDetailService 실행 username ={} user = {}", username, cacheUser.nickname());
        log.info("cacheUserId = {}, cacheUserNickname = {}, cacheUserUsername = {}, cacheUserPassword = {}, cacheUserRole = {}",
                cacheUser.userId(), cacheUser.nickname(), cacheUser.username(), cacheUser.password(), cacheUser.roleName());
        return CustomUserDetails.create(cacheUser);
    }
}
