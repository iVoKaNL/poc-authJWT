package com.ivoka.authJWT.controller;

import com.ivoka.authJWT.model.Project;
import com.ivoka.authJWT.payload.*;
import com.ivoka.authJWT.repository.ProjectRepository;
import com.ivoka.authJWT.repository.UserRepository;
import com.ivoka.authJWT.repository.VoteRepository;
import com.ivoka.authJWT.security.CurrentUser;
import com.ivoka.authJWT.security.UserPrincipal;
import com.ivoka.authJWT.service.ProjectService;
import com.ivoka.authJWT.util.AppConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectService projectService;

    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);

    @GetMapping
    public PagedResponse<ProjectResponse> getProjects(@CurrentUser UserPrincipal currentUser,
                                                   @RequestParam(value = "page", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
                                                   @RequestParam(value = "size", defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        return projectService.getAllProjects(currentUser, page, size);
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createProject(@Valid @RequestBody ProjectRequest projectRequest) {
        Project project = projectService.createProject(projectRequest);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{projectId}")
                .buildAndExpand(project.getId()).toUri();

        return ResponseEntity.created(location)
                .body(new ApiResponse(true, "Project Created Successfully"));
    }

    @GetMapping("/{projectId}")
    public ProjectResponse getProjectById(@CurrentUser UserPrincipal currentUser,
                                    @PathVariable Long projectId) {
        return projectService.getProjectById(projectId, currentUser);
    }

    @PostMapping("/{projectId}/votes")
    @PreAuthorize("hasRole('USER')")
    public ProjectResponse castVote(@CurrentUser UserPrincipal currentUser,
                                 @PathVariable Long projectId,
                                 @Valid @RequestBody VoteRequest voteRequest) {
        return projectService.castVoteAndGetUpdatedProject(projectId, voteRequest, currentUser);
    }



}
