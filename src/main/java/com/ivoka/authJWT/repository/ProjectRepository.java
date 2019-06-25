package com.ivoka.authJWT.repository;

import com.ivoka.authJWT.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findById(Long projectId);

    Page<Project> findByCreatedBy(Long userId, Pageable pageable);

    long countByCreatedBy(Long userId);

    List<Project> findByIdIn(List<Long> projectIds);

    List<Project> findByIdIn(List<Long> projectIds, Sort sort);
}
