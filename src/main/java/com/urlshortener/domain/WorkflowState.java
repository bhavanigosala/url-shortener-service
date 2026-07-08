package com.urlshortener.domain;

import java.time.Instant;
import java.util.Map;

/**
 * Sealed class for Workflow States - demonstrates bounded polymorphism
 * Pattern matching support for exhaustive state handling
 */
public sealed interface WorkflowState {
    Instant timestamp();
    
    // Pending state - workflow created but not started
    record PendingState(
        String taskId,
        Instant timestamp,
        Map<String, Object> context
    ) implements WorkflowState {}
    
    // Running state - workflow in progress
    record RunningState(
        String taskId,
        Instant timestamp,
        String currentStep,
        Map<String, Object> context
    ) implements WorkflowState {}
    
    // Approval pending state - awaiting human approval
    record ApprovalPendingState(
        String taskId,
        Instant timestamp,
        String approvalReason,
        Map<String, Object> context
    ) implements WorkflowState {}
    
    // Completed state - workflow succeeded
    record CompletedState(
        String taskId,
        Instant timestamp,
        Map<String, Object> result,
        Map<String, Object> context
    ) implements WorkflowState {}
    
    // Failed state - workflow failed with error
    record FailedState(
        String taskId,
        Instant timestamp,
        String errorMessage,
        Exception cause,
        Map<String, Object> context
    ) implements WorkflowState {}
    
    // Rolled back state - workflow was rolled back
    record RolledBackState(
        String taskId,
        Instant timestamp,
        String reason,
        Map<String, Object> context
    ) implements WorkflowState {}
}
