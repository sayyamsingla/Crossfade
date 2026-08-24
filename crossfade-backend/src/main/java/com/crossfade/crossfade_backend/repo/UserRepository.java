package com.crossfade.crossfade_backend.repo;

import com.crossfade.crossfade_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}
