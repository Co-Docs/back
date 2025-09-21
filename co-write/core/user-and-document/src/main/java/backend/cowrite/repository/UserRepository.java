package backend.cowrite.repository;

import backend.cowrite.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<Long, User> {

    @Query(value = "select u from User u where u.userId = :userId")
    Optional<User> findById(@Param("id")Long userId);

    @Query(value = "select u from User u where u.nickname = :nickname")
    Optional<User> findByName(@Param("nickname") String nickname);
}
