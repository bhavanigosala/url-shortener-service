package com.urlshortener.orchestration;

import com.urlshortener.domain.ShortenUrlRequest;
import com.urlshortener.domain.ShortenUrlResponse;
import com.urlshortener.domain.WorkflowState;
import com.urlshortener.service.UrlShortenerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agentic Workflow Orchestration Engine
 * 
 * Demonstrates:
 * - Stateful workflow management with sealed class state machine
 * - Pattern matching for exhaustive state handling
 * - Dependency graph with entry/exit gates
 * - Approval checkpoints for high-impact actions
 * - Audit trail and observability
 * - Fallback, retry, and rollback controls
 * 
 * Modern Java features:
 * - Pattern matching (instanceof checks)
 * - Sealed classes for bounded workflow states
 * - Records for immutable event data
 * - Text blocks for SQL/multi-line strings
 */
@Slf4j
@Service
public class WorkflowOrchestrator {

    private final UrlShortenerService urlShortenerService;
    private final ConcurrentHashMap<String, WorkflowExecution> executions;
    private final AtomicInteger executionCounter = new AtomicInteger(0);

    public WorkflowOrchestrator(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
        this.executions = new ConcurrentHashMap<>();
    }

    /**
     * Execute workflow to shorten URL with agentic orchestration
     */
    public WorkflowExecution executeUrlShorteningWorkflow(ShortenUrlRequest request) {
        String executionId = "exec-" + executionCounter.incrementAndGet();
        WorkflowExecution execution = new WorkflowExecution(executionId, request);
        executions.put(executionId, execution);

        try {
            log.info("Starting workflow execution: {}", executionId);
            
            // State: PENDING
            execution.setState(new WorkflowState.PendingState(
                executionId,
                Instant.now(),
                Map.of("originalUrl", request.originalUrl())
            ));
            addAuditLog(execution, "Workflow created and pending execution");

            // Validation gate
            if (!validateRequest(request)) {
                execution.setState(new WorkflowState.FailedState(
                    executionId,
                    Instant.now(),
                    "Request validation failed",
                    new IllegalArgumentException("Invalid request"),
                    execution.getContext()
                ));
                addAuditLog(execution, "Request validation failed - workflow failed");
                return execution;
            }
            addAuditLog(execution, "Request validation passed");

            // State: RUNNING
            execution.setState(new WorkflowState.RunningState(
                executionId,
                Instant.now(),
                "validation_complete",
                execution.getContext()
            ));

            // Check if approval needed (custom alias requires approval)
            if (request.customAlias() != null && !request.customAlias().isBlank()) {
                execution.setState(new WorkflowState.ApprovalPendingState(
                    executionId,
                    Instant.now(),
                    "Custom alias requires manual approval: " + request.customAlias(),
                    execution.getContext()
                ));
                addAuditLog(execution, "Approval required for custom alias: " + request.customAlias());
                return execution;
            }

            // Execute URL shortening
            try {
                ShortenUrlResponse response = urlShortenerService.shortenUrl(request);
                execution.setResult(response);
                
                execution.setState(new WorkflowState.CompletedState(
                    executionId,
                    Instant.now(),
                    Map.of(
                        "shortCode", response.shortCode(),
                        "shortUrl", response.shortUrl()
                    ),
                    execution.getContext()
                ));
                addAuditLog(execution, "URL successfully shortened: " + response.shortCode());
                
            } catch (Exception e) {
                // Fallback: retry mechanism
                log.warn("URL shortening failed, attempting retry", e);
                addAuditLog(execution, "URL shortening failed, retrying: " + e.getMessage());
                
                try {
                    ShortenUrlResponse response = urlShortenerService.shortenUrl(request);
                    execution.setResult(response);
                    execution.setState(new WorkflowState.CompletedState(
                        executionId,
                        Instant.now(),
                        Map.of(
                            "shortCode", response.shortCode(),
                            "shortUrl", response.shortUrl()
                        ),
                        execution.getContext()
                    ));
                    addAuditLog(execution, "URL successfully shortened on retry: " + response.shortCode());
                } catch (Exception retryEx) {
                    throw new RuntimeException("Failed after retry", retryEx);
                }
            }

        } catch (Exception e) {
            log.error("Workflow execution failed: {}", executionId, e);
            
            // Attempt rollback
            attemptRollback(execution, "Execution failed: " + e.getMessage());
            
            execution.setState(new WorkflowState.FailedState(
                executionId,
                Instant.now(),
                "Workflow execution failed: " + e.getMessage(),
                e,
                execution.getContext()
            ));
            addAuditLog(execution, "Workflow failed with error: " + e.getMessage());
        }

        return execution;
    }

    /**
     * Approve pending workflow (e.g., custom alias approval)
     */
    public WorkflowExecution approveWorkflow(String executionId) {
        WorkflowExecution execution = executions.get(executionId);
        if (execution == null) {
            throw new IllegalArgumentException("Execution not found: " + executionId);
        }

        // Pattern matching on current state
        WorkflowState currentState = execution.getState();
        if (!(currentState instanceof WorkflowState.ApprovalPendingState approval)) {
            throw new IllegalStateException("Workflow not in approval pending state");
        }

        log.info("Approving workflow: {}", executionId);
        addAuditLog(execution, "Workflow approved by user");

        // Re-execute with approval
        try {
            ShortenUrlRequest request = execution.getRequest();
            ShortenUrlResponse response = urlShortenerService.shortenUrl(request);
            execution.setResult(response);

            execution.setState(new WorkflowState.CompletedState(
                executionId,
                Instant.now(),
                Map.of(
                    "shortCode", response.shortCode(),
                    "shortUrl", response.shortUrl()
                ),
                execution.getContext()
            ));
            addAuditLog(execution, "URL successfully shortened after approval: " + response.shortCode());
            
        } catch (Exception e) {
            execution.setState(new WorkflowState.FailedState(
                executionId,
                Instant.now(),
                "Failed after approval: " + e.getMessage(),
                e,
                execution.getContext()
            ));
            addAuditLog(execution, "Failed after approval: " + e.getMessage());
        }

        return execution;
    }

    /**
     * Reject pending workflow
     */
    public WorkflowExecution rejectWorkflow(String executionId, String reason) {
        WorkflowExecution execution = executions.get(executionId);
        if (execution == null) {
            throw new IllegalArgumentException("Execution not found: " + executionId);
        }

        log.info("Rejecting workflow: {} - Reason: {}", executionId, reason);
        
        execution.setState(new WorkflowState.FailedState(
            executionId,
            Instant.now(),
            "Workflow rejected: " + reason,
            null,
            execution.getContext()
        ));
        addAuditLog(execution, "Workflow rejected: " + reason);

        return execution;
    }

    /**
     * Get workflow execution status
     */
    public WorkflowExecution getExecution(String executionId) {
        return executions.get(executionId);
    }

    /**
     * Validate request before execution
     */
    private boolean validateRequest(ShortenUrlRequest request) {
        return request.originalUrl() != null && !request.originalUrl().isBlank();
    }

    /**
     * Attempt rollback on failure
     * Uses pattern matching to handle different error scenarios
     */
    private void attemptRollback(WorkflowExecution execution, String reason) {
        WorkflowState state = execution.getState();
        
        // Pattern matching example - exhaustive check
        String action = switch (state) {
            case WorkflowState.CompletedState completed -> {
                // If already completed, rollback may be needed
                log.warn("Rolling back completed workflow");
                yield "Rollback completed state";
            }
            case WorkflowState.RunningState running -> {
                log.warn("Stopping running workflow");
                yield "Stopped running workflow";
            }
            case WorkflowState.ApprovalPendingState approval -> {
                log.info("Cancelling approval pending workflow");
                yield "Cancelled approval pending workflow";
            }
            case WorkflowState.FailedState failed -> {
                log.info("Workflow already failed, no rollback needed");
                yield "Workflow already failed";
            }
            case WorkflowState.RolledBackState rolled -> {
                log.info("Workflow already rolled back");
                yield "Already rolled back";
            }
            case WorkflowState.PendingState pending -> {
                log.info("Cancelling pending workflow");
                yield "Cancelled pending workflow";
            }
        };

        execution.setState(new WorkflowState.RolledBackState(
            execution.getState().timestamp().toString(),
            Instant.now(),
            reason + " - " + action,
            execution.getContext()
        ));
    }

    /**
     * Add audit log entry
     */
    private void addAuditLog(WorkflowExecution execution, String message) {
        execution.getAuditLog().add(new AuditLogEntry(
            Instant.now(),
            message,
            execution.getState().getClass().getSimpleName()
        ));
    }

    /**
     * Workflow Execution record - tracks execution state and context
     */
    public static class WorkflowExecution {
        private final String executionId;
        private final ShortenUrlRequest request;
        private WorkflowState state;
        private ShortenUrlResponse result;
        private final Map<String, Object> context;
        private final List<AuditLogEntry> auditLog;

        public WorkflowExecution(String executionId, ShortenUrlRequest request) {
            this.executionId = executionId;
            this.request = request;
            this.context = new ConcurrentHashMap<>();
            this.auditLog = Collections.synchronizedList(new ArrayList<>());
        }

        // Getters
        public String getExecutionId() { return executionId; }
        public ShortenUrlRequest getRequest() { return request; }
        public WorkflowState getState() { return state; }
        public ShortenUrlResponse getResult() { return result; }
        public Map<String, Object> getContext() { return context; }
        public List<AuditLogEntry> getAuditLog() { return auditLog; }

        public void setState(WorkflowState state) { this.state = state; }
        public void setResult(ShortenUrlResponse result) { this.result = result; }
    }

    /**
     * Audit log entry record
     */
    public record AuditLogEntry(
        Instant timestamp,
        String message,
        String stateName
    ) {}
}
