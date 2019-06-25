package com.ivoka.authJWT.service;

import com.ivoka.authJWT.exception.BadRequestException;
import com.ivoka.authJWT.exception.ResourceNotFoundException;
import com.ivoka.authJWT.model.*;
import com.ivoka.authJWT.payload.PagedResponse;
import com.ivoka.authJWT.payload.ProjectRequest;
import com.ivoka.authJWT.payload.ProjectResponse;
import com.ivoka.authJWT.payload.VoteRequest;
import com.ivoka.authJWT.repository.ProjectRepository;
import com.ivoka.authJWT.repository.UserRepository;
import com.ivoka.authJWT.repository.VoteRepository;
import com.ivoka.authJWT.security.UserPrincipal;
import com.ivoka.authJWT.util.AppConstants;
import com.ivoka.authJWT.util.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);
    
    public PagedResponse<ProjectResponse> getAllProjects(UserPrincipal currentUser, int page, int size) {
        validatePageNumberAndSize(page, size);

        // Retrieve Projects
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Project> projects = projectRepository.findAll(pageable);

        if(projects.getNumberOfElements() == 0) {
            return new PagedResponse<>(Collections.emptyList(), projects.getNumber(),
                    projects.getSize(), projects.getTotalElements(), projects.getTotalPages(), projects.isLast());
        }

        // Map Projects to ProjectResponses containing vote counts and project creator details
        List<Long> projectIds = projects.map(Project::getId).getContent();
        Map<Long, Long> taskVoteCountMap = getTaskVoteCountMap(projectIds);
        Map<Long, Long> projectUserVoteMap = getProjectUserVoteMap(currentUser, projectIds);
        Map<Long, User> creatorMap = getProjectCreatorMap(projects.getContent());

        List<ProjectResponse> projectResponses = projects.map(project -> {
            return ModelMapper.mapProjectToProjectResponse(project,
                    taskVoteCountMap,
                    creatorMap.get(project.getCreatedBy()),
                    projectUserVoteMap == null ? null : projectUserVoteMap.getOrDefault(project.getId(), null));
        }).getContent();

        return new PagedResponse<>(projectResponses, projects.getNumber(),
                projects.getSize(), projects.getTotalElements(), projects.getTotalPages(), projects.isLast());
    }

    public PagedResponse<ProjectResponse> getProjectsCreatedBy(String username, UserPrincipal currentUser, int page, int size) {
        validatePageNumberAndSize(page, size);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        // Retrieve all projectss created by the given username
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Project> projects = projectRepository.findByCreatedBy(user.getId(), pageable);

        if (projects.getNumberOfElements() == 0) {
            return new PagedResponse<>(Collections.emptyList(), projects.getNumber(),
                    projects.getSize(), projects.getTotalElements(), projects.getTotalPages(), projects.isLast());
        }

        // Map Projects to ProjectResponses containing vote counts and project creator details
        List<Long> projectIds = projects.map(Project::getId).getContent();
        Map<Long, Long> taskVoteCountMap = getTaskVoteCountMap(projectIds);
        Map<Long, Long> projectUserVoteMap = getProjectUserVoteMap(currentUser, projectIds);

        List<ProjectResponse> projectResponses = projects.map(project -> {
            return ModelMapper.mapProjectToProjectResponse(project,
                    taskVoteCountMap,
                    user,
                    projectUserVoteMap == null ? null : projectUserVoteMap.getOrDefault(project.getId(), null));
        }).getContent();

        return new PagedResponse<>(projectResponses, projects.getNumber(),
                projects.getSize(), projects.getTotalElements(), projects.getTotalPages(), projects.isLast());
    }

    public PagedResponse<ProjectResponse> getProjectsVotedBy(String username, UserPrincipal currentUser, int page, int size) {
        validatePageNumberAndSize(page, size);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        // Retrieve all projectIds in which the given username has voted
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Long> userVotedProjectIds = voteRepository.findVotedProjectIdsByUserId(user.getId(), pageable);

        if (userVotedProjectIds.getNumberOfElements() == 0) {
            return new PagedResponse<>(Collections.emptyList(), userVotedProjectIds.getNumber(),
                    userVotedProjectIds.getSize(), userVotedProjectIds.getTotalElements(),
                    userVotedProjectIds.getTotalPages(), userVotedProjectIds.isLast());
        }

        // Retrieve all project details from the voted projectIds.
        List<Long> projectIds = userVotedProjectIds.getContent();

        Sort sort = new Sort(Sort.Direction.DESC, "createdAt");
        List<Project> projects = projectRepository.findByIdIn(projectIds, sort);

        // Map Projects to ProjectResponses containing vote counts and project creator details
        Map<Long, Long> taskVoteCountMap = getTaskVoteCountMap(projectIds);
        Map<Long, Long> projectUserVoteMap = getProjectUserVoteMap(currentUser, projectIds);
        Map<Long, User> creatorMap = getProjectCreatorMap(projects);

        List<ProjectResponse> projectResponses = projects.stream().map(project -> {
            return ModelMapper.mapProjectToProjectResponse(project,
                    taskVoteCountMap,
                    creatorMap.get(project.getCreatedBy()),
                    projectUserVoteMap == null ? null : projectUserVoteMap.getOrDefault(project.getId(), null));
        }).collect(Collectors.toList());

        return new PagedResponse<>(projectResponses, userVotedProjectIds.getNumber(), userVotedProjectIds.getSize(), userVotedProjectIds.getTotalElements(), userVotedProjectIds.getTotalPages(), userVotedProjectIds.isLast());
    }

    public Project createProject(ProjectRequest projectRequest) {
        Project project = new Project();
        project.setProjectName(projectRequest.getProjectName());

        projectRequest.getTasks().forEach(taskRequest -> {
            project.addTask(new Task(taskRequest.getText()));
        });

        Instant now = Instant.now();
        Instant expirationDateTime = now.plus(Duration.ofDays(projectRequest.getProjectLength().getDays()))
                .plus(Duration.ofHours(projectRequest.getProjectLength().getHours()));

        project.setExpirationDateTime(expirationDateTime);

        return projectRepository.save(project);
    }


    public ProjectResponse getProjectById(Long projectId, UserPrincipal currentUser) {
        Project project = projectRepository.findById(projectId).orElseThrow(
                () -> new ResourceNotFoundException("Project", "id", projectId));

        // Retrieve Vote Counts of every task belonging to the current project
        List<TaskVoteCount> votes = voteRepository.countByProjectIdGroupByTaskId(projectId);

        Map<Long, Long> taskVotesMap = votes.stream()
                .collect(Collectors.toMap(TaskVoteCount::getTaskId, TaskVoteCount::getVoteCount));

        // Retrieve project creator details
        User creator = userRepository.findById(project.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", project.getCreatedBy()));

        // Retrieve vote done by logged in user
        Vote userVote = null;
        if(currentUser != null) {
            userVote = voteRepository.findByUserIdAndProjectId(currentUser.getId(), projectId);
        }

        return ModelMapper.mapProjectToProjectResponse(project, taskVotesMap,
                creator, userVote != null ? userVote.getTask().getId(): null);
    }

    public ProjectResponse castVoteAndGetUpdatedProject(Long projectId, VoteRequest voteRequest, UserPrincipal currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        if(project.getExpirationDateTime().isBefore(Instant.now())) {
            throw new BadRequestException("Sorry! This Project has already expired");
        }

        User user = userRepository.getOne(currentUser.getId());

        Task selectedTask = project.getTasks().stream()
                .filter(task -> task.getId().equals(voteRequest.getTaskId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", voteRequest.getTaskId()));

        Vote vote = new Vote();
        vote.setProject(project);
        vote.setUser(user);
        vote.setTask(selectedTask);

        try {
            vote = voteRepository.save(vote);
        } catch (DataIntegrityViolationException ex) {
            logger.info("User {} has already voted in Project {}", currentUser.getId(), projectId);
            throw new BadRequestException("Sorry! You have already cast your vote in this project");
        }

        //-- Vote Saved, Return the updated Project Response now --

        // Retrieve Vote Counts of every task belonging to the current project
        List<TaskVoteCount> votes = voteRepository.countByProjectIdGroupByTaskId(projectId);

        Map<Long, Long> taskVotesMap = votes.stream()
                .collect(Collectors.toMap(TaskVoteCount::getTaskId, TaskVoteCount::getVoteCount));

        // Retrieve project creator details
        User creator = userRepository.findById(project.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", project.getCreatedBy()));

        return ModelMapper.mapProjectToProjectResponse(project, taskVotesMap, creator, vote.getTask().getId());
    }

    private void validatePageNumberAndSize(int page, int size) {
        if(page < 0) {
            throw new BadRequestException("Page number cannot be less than zero.");
        }

        if(size > AppConstants.MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size must not be greater than " + AppConstants.MAX_PAGE_SIZE);
        }
    }

    private Map<Long, Long> getTaskVoteCountMap(List<Long> projectIds) {
        // Retrieve Vote Counts of every Task belonging to the given projectIds
        List<TaskVoteCount> votes = voteRepository.countByProjectIdInGroupByTaskId(projectIds);

        Map<Long, Long> taskVotesMap = votes.stream()
                .collect(Collectors.toMap(TaskVoteCount::getTaskId, TaskVoteCount::getVoteCount));

        return taskVotesMap;
    }

    private Map<Long, Long> getProjectUserVoteMap(UserPrincipal currentUser, List<Long> projectIds) {
        // Retrieve Votes done by the logged in user to the given projectIds
        Map<Long, Long> projectUserVoteMap = null;
        if(currentUser != null) {
            List<Vote> userVotes = voteRepository.findByUserIdAndProjectIdIn(currentUser.getId(), projectIds);

            projectUserVoteMap = userVotes.stream()
                    .collect(Collectors.toMap(vote -> vote.getProject().getId(), vote -> vote.getTask().getId()));
        }
        return projectUserVoteMap;
    }

    Map<Long, User> getProjectCreatorMap(List<Project> projects) {
        // Get Project Creator details of the given list of projects
        List<Long> creatorIds = projects.stream()
                .map(Project::getCreatedBy)
                .distinct()
                .collect(Collectors.toList());

        List<User> creators = userRepository.findByIdIn(creatorIds);
        Map<Long, User> creatorMap = creators.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return creatorMap;
    }
    
    

    
    

    
}
