package com.ivoka.authJWT.payload;

import javax.validation.constraints.NotNull;

public class VoteRequest {
    @NotNull
    private Long taskId;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
}