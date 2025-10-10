package backend.cowrite.service.response;

import backend.cowrite.common.Role;

public record UserCacheDto(
        Long userId,
        String username,
        String password,
        String nickname,
        Role roleName
) {
}
