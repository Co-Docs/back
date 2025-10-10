package backend.cowrite.controller;

import backend.cowrite.auth.CustomUserDetails;
import backend.cowrite.common.ResponseHandler;
import backend.cowrite.entity.User;
import backend.cowrite.service.UserService;
import backend.cowrite.service.request.FindIdRequest;
import backend.cowrite.service.request.FindPwRequest1;
import backend.cowrite.service.request.FindPwRequest2;
import backend.cowrite.service.request.RegisterRequest;
import backend.cowrite.service.response.UserCacheDto;
import backend.cowrite.service.response.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ResponseHandler<UserInfoResponse>> userInfo(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        log.debug("내 정보 확인 메서드 실행 유저 정보 = {}", customUserDetails.getUser().toString());
        UserInfoResponse user = UserInfoResponse.of(userService.findById(customUserDetails.getUser().userId()));
        return ResponseEntity.ok(ResponseHandler.success(user));
    }

    @GetMapping("/isDuplicated")
    public ResponseEntity<ResponseHandler<Boolean>> findByUsername(@RequestParam("username") String username) {
        log.debug("아이디 중복 확인을 위한 메서드 실행 username = {}", username);
        UserCacheDto user = userService.findByUsername(username);
        Boolean flag = user == null;
        return ResponseEntity.ok(ResponseHandler.success(flag));
    }

    @GetMapping("/{username}")
    public ResponseEntity<ResponseHandler<String>> findNicknameByUsername(@PathVariable("username") String username) {
        log.debug("username으로 유저 닉네임 주는 메서드 실행 username = {}", username);
        UserCacheDto user = userService.findByUsername(username);
        return ResponseEntity.ok(ResponseHandler.success(user.nickname()));
    }

    @PostMapping("/sign-in")
    public ResponseEntity<ResponseHandler<Long>> register(@RequestBody RegisterRequest registerRequest) {
        log.debug("회원가입 메서드 실행 registerRequest = {}", registerRequest);
        Long userId = userService.register(registerRequest.username(), registerRequest.password(), registerRequest.passwordConfirm(), registerRequest.nickname(),
                registerRequest.birth(), registerRequest.email(), registerRequest.phoneNumber());
        return ResponseEntity.ok(ResponseHandler.success(userId));
    }

    @GetMapping("/logout")
    public ResponseEntity<ResponseHandler<Void>> logout(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        log.debug("로그아웃 메서드 실행 유저 정보 = {}", customUserDetails.getUser().toString());
        userService.logout(customUserDetails.getUser().username());
        return ResponseEntity.ok(ResponseHandler.success(null));
    }

    @PostMapping("/findId")
    public ResponseEntity<ResponseHandler<String>> findId(@RequestBody FindIdRequest findIdRequest) {
        log.debug("아이디 찾기 메서드 실행 findIdRequest = {}", findIdRequest);
        User findUser = userService.findByName(findIdRequest.nickname());
        //todo: 메일 보내는 서비스 구현
        return ResponseEntity.ok(ResponseHandler.success("메일로 id를 전송하였습니다."));
    }

    @PatchMapping("/findPw1")
    public ResponseEntity<ResponseHandler<String>> findPw1(@RequestBody FindPwRequest1 findPwRequest) {
        log.debug("비밀번호 찾기 메서드1 실행 findIdRequest = {}", findPwRequest);
        UserCacheDto user = userService.findByUsername(findPwRequest.username());
        //todo: 메일 보내는 서비스 구현
        return ResponseEntity.ok(ResponseHandler.success("메일로 인증링크를 전송하였습니다."));
    }

    @PatchMapping("/findPw2")
    public ResponseEntity<ResponseHandler<String>> findPw2(@RequestBody FindPwRequest2 findPwRequest2) {
        log.debug("비밀번호 찾기 메서드2 실행 findIdRequest = {}", findPwRequest2);
        User user = userService.changePassword(findPwRequest2.username(), findPwRequest2.newPassword(), findPwRequest2.newPasswordConfirm());
        return ResponseEntity.ok(ResponseHandler.success("메일로 인증링크를 전송하였습니다."));
    }



}
