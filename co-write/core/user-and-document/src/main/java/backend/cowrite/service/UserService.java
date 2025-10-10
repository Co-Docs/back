package backend.cowrite.service;

import backend.cowrite.entity.User;
import backend.cowrite.exception.CustomException;
import backend.cowrite.exception.ErrorCode;
import backend.cowrite.repository.UserRepository;
import backend.cowrite.service.response.UserCacheDto;
import backend.cowrite.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final Snowflake snowflake;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Transactional(readOnly = true)
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "해당 userId의 유저가 존재하지 않습니다:" + userId));
    }

    @Transactional(readOnly = true)
    public User findByName(String name) {
        return userRepository.findByName(name)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND,"해당 닉네임의 유저가 존재하지 않습니다." + name));
    }

    public Long register(String username, String password, String passwordConfirm, String nickname, LocalDateTime birth, String email, String phoneNumber) {
        User.checkPasswordConfirm(password,passwordConfirm);
        String encodedPassword = bCryptPasswordEncoder.encode(password);
        String encodedConfirmPassword = bCryptPasswordEncoder.encode(passwordConfirm);
        User user = User.registerUser(snowflake.nextId(), username, encodedPassword, encodedConfirmPassword, nickname, birth, email, phoneNumber);
        User savedUser = userRepository.save(user);
        return savedUser.getUserId();
    }


    @CacheEvict(cacheNames = "user", key = "'user:username:' + #username",cacheManager = "userCacheManager")
    public void logout(String username) {
    }

    @Cacheable(cacheManager = "userCacheManager", cacheNames = "user", key = "'user:username:' + #username"
    )
    public UserCacheDto findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "해당 username의 유저가 존재하지 않습니다:" + username));

        return new UserCacheDto(user.getUserId(), user.getUsername(), user.getPassword(), user.getNickname(), user.getRole());
    }


    public User changePassword(String username, String password, String passwordConfirm) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "해당 username의 유저가 존재하지 않습니다:" + username));
        user.changePassword(password,passwordConfirm);
        return user;
    }
}
