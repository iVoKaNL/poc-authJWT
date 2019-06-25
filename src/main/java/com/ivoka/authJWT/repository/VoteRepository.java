package com.ivoka.authJWT.repository;

import com.ivoka.authJWT.model.TaskVoteCount;
import com.ivoka.authJWT.model.Vote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    @Query("SELECT NEW com.ivoka.authJWT.model.TaskVoteCount(v.task.id, count(v.id)) FROM Vote v WHERE v.project.id in :projectIds GROUP BY v.task.id")
    List<TaskVoteCount> countByProjectIdInGroupByTaskId(@Param("projectIds") List<Long> projectIds);

    @Query("SELECT NEW com.ivoka.authJWT.model.TaskVoteCount(v.task.id, count(v.id)) FROM Vote v WHERE v.project.id = :projectId GROUP BY v.task.id")
    List<TaskVoteCount> countByProjectIdGroupByTaskId(@Param("projectId") Long projectId);

    @Query("SELECT v FROM Vote v where v.user.id = :userId and v.project.id in :projectIds")
    List<Vote> findByUserIdAndProjectIdIn(@Param("userId") Long userId, @Param("projectIds") List<Long> projectIds);

    @Query("SELECT v FROM Vote v where v.user.id = :userId and v.project.id = :projectId")
    Vote findByUserIdAndProjectId(@Param("userId") Long userId, @Param("projectId") Long projectId);

    @Query("SELECT COUNT(v.id) from Vote v where v.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT v.project.id FROM Vote v WHERE v.user.id = :userId")
    Page<Long> findVotedProjectIdsByUserId(@Param("userId") Long userId, Pageable pageable);
}

