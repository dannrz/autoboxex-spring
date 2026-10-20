package com.autoboxex.repository;

import com.autoboxex.dto.UsuarioResponse;
import com.autoboxex.model.Usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Usuario, Long> {
    List<UsuarioResponse> findAllProjectedBy();

    @EntityGraph(attributePaths = "role")
    Optional<Usuario> findByUsername(String username);
}
