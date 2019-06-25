package com.ivoka.authJWT.model;

public class TaskVoteCount {
    private Long taskId;
    private Long voteCount;

    public TaskVoteCount(Long taskId, Long voteCount) {
        this.taskId = taskId;
        this.voteCount = voteCount;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(Long voteCount) {
        this.voteCount = voteCount;
    }
}
