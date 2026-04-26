package com.core.service.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.core.service.entity.Link;

public interface LinkRepository extends JpaRepository<Link, Long> {
    Optional<Link> findByShortCode(String shortCode);
    Optional<Link> findByOriginalUrl(String originalUrl);
}