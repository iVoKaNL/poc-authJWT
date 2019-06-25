package com.ivoka.authJWT.payload;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

public class ProjectRequest {
    @NotBlank
    @Size(max = 140)
    private String projectName;

    @NotNull
    @Size(min = 2, max = 6)
    @Valid
    private List<TaskRequest> tasks;

    @NotNull
    @Valid
    private ProjectLength projectLength;

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public List<TaskRequest> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskRequest> tasks) {
        this.tasks = tasks;
    }

    public ProjectLength getProjectLength() {
        return projectLength;
    }

    public void setProjectLength(ProjectLength projectLength) {
        this.projectLength = projectLength;
    }
}
