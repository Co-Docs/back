package backend.cowrite.auth;

import backend.cowrite.entity.User;
import backend.cowrite.exception.CustomException;
import backend.cowrite.exception.ErrorCode;
import backend.cowrite.repository.UserRepository;
import backend.cowrite.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
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
        User user = userService.findByUsername(username);
        log.debug("로그인 시 customUserDetailService 실행 username ={} user = {}", username, user.getNickname());
        return CustomUserDetails.create(user);
    }
}
