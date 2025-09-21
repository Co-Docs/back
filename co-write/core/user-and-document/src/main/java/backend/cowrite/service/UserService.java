package backend.cowrite.service;

import backend.cowrite.entity.User;
import backend.cowrite.exception.CustomException;
import backend.cowrite.exception.ErrorCode;
import backend.cowrite.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "해당 userId의 유저가 존재하지 않습니다:" + userId));
    }

    public User findByName(String name) {
        return userRepository.findByName(name)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND,"해당 닉네임의 유저가 존재하지 않습니다." + name));
    }
}
