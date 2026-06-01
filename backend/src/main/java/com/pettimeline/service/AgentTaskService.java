package com.pettimeline.service;

import java.util.List;
import java.util.Map;

public interface AgentTaskService {
    List<Map<String, Object>> generateTasks(Long userId, Long petId);
    Map<String, Object> completeTask(Long userId, Long petId, String taskId);
}
