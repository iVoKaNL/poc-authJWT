package com.ivoka.authJWT.util;

import com.ivoka.authJWT.model.Project;
import com.ivoka.authJWT.model.User;
import com.ivoka.authJWT.payload.ProjectResponse;
import com.ivoka.authJWT.payload.TaskResponse;
import com.ivoka.authJWT.payload.UserSummary;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ModelMapper {

    public static ProjectResponse mapProjectToProjectResponse(Project project, Map<Long, Long> taskVotesMap, User creator, Long userVote) {
        ProjectResponse projectResponse = new ProjectResponse();
        projectResponse.setId(project.getId());
        projectResponse.setProjectName(project.getProjectName());
        projectResponse.setCreationDateTime(project.getCreatedAt());
        projectResponse.setExpirationDateTime(project.getExpirationDateTime());
        Instant now = Instant.now();
        projectResponse.setExpired(project.getExpirationDateTime().isBefore(now));

        List<TaskResponse> taskResponses = project.getTasks().stream().map(task -> {
            TaskResponse taskResponse = new TaskResponse();
            taskResponse.setId(task.getId());
            taskResponse.setText(task.getText());

            if(taskVotesMap.containsKey(task.getId())) {
                taskResponse.setVoteCount(taskVotesMap.get(task.getId()));
            } else {
                taskResponse.setVoteCount(0);
            }
            return taskResponse;
        }).collect(Collectors.toList());

        projectResponse.setTasks(taskResponses);
        UserSummary creatorSummary = new UserSummary(creator.getId(), creator.getUsername(), creator.getName());
        projectResponse.setCreatedBy(creatorSummary);

        if(userVote != null) {
            projectResponse.setSelectedTasks(userVote);
        }

        long totalVotes = projectResponse.getTasks().stream().mapToLong(TaskResponse::getVoteCount).sum();
        projectResponse.setTotalVotes(totalVotes);

        return projectResponse;
    }
}
