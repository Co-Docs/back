package backend.cowrite.repository;

import backend.cowrite.entity.Document;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Query(value = "select d " +
            "from Document d " +
            "where exists (" +
            "select ud " +
            "from UserDocument ud " +
            "where ud.user.userId = :userId )")
    List<Document> readAll(@Param("userId") Long userId, Pageable pageable);
}
