package backend.cowrite.repository;

import backend.cowrite.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query(value = "select u from User u where u.nickname = :nickname")
    Optional<User> findByName(@Param("nickname") String nickname);

    @Query(value = "select u from User u where u.username = :username")
    Optional<User> findByUsername(@Param("username") String username);
}
