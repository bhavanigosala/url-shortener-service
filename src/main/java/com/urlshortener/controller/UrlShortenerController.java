package com.urlshortener.controller;

import com.urlshortener.domain.ShortenUrlRequest;
import com.urlshortener.domain.ShortenUrlResponse;
import com.urlshortener.service.UrlShortenerService;
import com.urlshortener.orchestration.WorkflowOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST Controller for URL Shortening APIs
 * 
 * Endpoints:
 * POST   /api/urls              - Shorten URL
 * POST   /api/urls/orchestrated - Shorten with agentic orchestration
 * GET    /api/urls/{code}       - Get URL details
 * GET    /s/{code}              - Redirect to original URL
 * DELETE /api/urls/{code}       - Delete shortened URL
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;
    private final WorkflowOrchestrator workflowOrchestrator;

    public UrlShortenerController(UrlShortenerService urlShortenerService,
                                 WorkflowOrchestrator workflowOrchestrator) {
        this.urlShortenerService = urlShortenerService;
        this.workflowOrchestrator = workflowOrchestrator;
    }

    /**
     * Shorten a URL - Direct API
     * POST /api/urls
     */
    @PostMapping("/urls")
    public ResponseEntity<ShortenUrlResponse> shortenUrl(@RequestBody ShortenUrlRequest request) {
        log.info("API Request: Shorten URL");
        
        try {
            ShortenUrlResponse response = urlShortenerService.shortenUrl(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error shortening URL", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Shorten URL with Agentic Orchestration
     * POST /api/urls/orchestrated
     * 
     * Returns workflow execution with state, audit trail, and approval status
     */
    @PostMapping("/urls/orchestrated")
    public ResponseEntity<WorkflowExecutionResponse> shortenUrlOrchestrated(
            @RequestBody ShortenUrlRequest request) {
        log.info("Orchestrated Request: Shorten URL");

        try {
            WorkflowOrchestrator.WorkflowExecution execution =
                workflowOrchestrator.executeUrlShorteningWorkflow(request);

            WorkflowExecutionResponse response = buildExecutionResponse(execution);
            
            // Check if approval is pending
            if (response.state().equals("ApprovalPendingState")) {
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            }

            // Check if execution succeeded
            if (response.state().equals("CompletedState")) {
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            }

            // Execution failed
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);

        } catch (Exception e) {
            log.error("Error in orchestrated workflow", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Approve pending workflow
     * POST /api/workflows/{executionId}/approve
     */
    @PostMapping("/workflows/{executionId}/approve")
    public ResponseEntity<WorkflowExecutionResponse> approveWorkflow(
            @PathVariable String executionId) {
        log.info("Approving workflow: {}", executionId);

        try {
            WorkflowOrchestrator.WorkflowExecution execution =
                workflowOrchestrator.approveWorkflow(executionId);
            
            WorkflowExecutionResponse response = buildExecutionResponse(execution);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error approving workflow", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Reject pending workflow
     * POST /api/workflows/{executionId}/reject
     */
    @PostMapping("/workflows/{executionId}/reject")
    public ResponseEntity<WorkflowExecutionResponse> rejectWorkflow(
            @PathVariable String executionId,
            @RequestBody RejectRequest request) {
        log.info("Rejecting workflow: {}", executionId);

        try {
            WorkflowOrchestrator.WorkflowExecution execution =
                workflowOrchestrator.rejectWorkflow(executionId, request.reason());
            
            WorkflowExecutionResponse response = buildExecutionResponse(execution);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error rejecting workflow", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get URL details
     * GET /api/urls/{code}
     */
    @GetMapping("/urls/{code}")
    public ResponseEntity<ShortenUrlResponse> getUrlDetails(@PathVariable String code) {
        log.debug("Get URL details: {}", code);

        Optional<ShortenUrlResponse> response = urlShortenerService.getUrlDetails(code);
        return response.map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get workflow execution details
     * GET /api/workflows/{executionId}
     */
    @GetMapping("/workflows/{executionId}")
    public ResponseEntity<WorkflowExecutionResponse> getWorkflowStatus(
            @PathVariable String executionId) {
        log.debug("Get workflow status: {}", executionId);

        WorkflowOrchestrator.WorkflowExecution execution =
            workflowOrchestrator.getExecution(executionId);

        if (execution == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(buildExecutionResponse(execution));
    }

    /**
     * Delete shortened URL
     * DELETE /api/urls/{code}
     */
    @DeleteMapping("/urls/{code}")
    public ResponseEntity<Void> deleteUrl(@PathVariable String code) {
        log.info("Delete URL: {}", code);

        try {
            urlShortenerService.deleteUrl(code);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting URL", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Build WorkflowExecutionResponse from execution
     */
    private WorkflowExecutionResponse buildExecutionResponse(
            WorkflowOrchestrator.WorkflowExecution execution) {
        
        return new WorkflowExecutionResponse(
            execution.getExecutionId(),
            execution.getState().getClass().getSimpleName(),
            execution.getResult(),
            execution.getContext(),
            execution.getAuditLog()
        );
    }

    /**
     * Response DTOs
     */
    public record WorkflowExecutionResponse(
        String executionId,
        String state,
        ShortenUrlResponse result,
        Object context,
        Object auditLog
    ) {}

    public record RejectRequest(String reason) {}
}
