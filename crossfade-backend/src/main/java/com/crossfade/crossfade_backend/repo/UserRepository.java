package com.crossfade.crossfade_backend.repo;

import com.crossfade.crossfade_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select u from User u where u.id <> :excludedId " +
            "and (lower(u.displayName) like lower(concat('%', :query, '%')) " +
            "or lower(u.handle) like lower(concat('%', :query, '%')))")
    List<User> searchByDisplayNameOrHandle(@Param("query") String query, @Param("excludedId") Long excludedId);

    Optional<User> findBySpotifyUserId(String spotifyUserId);

    boolean existsByHandleIgnoreCaseAndIdNot(String handle, Long id);
}
