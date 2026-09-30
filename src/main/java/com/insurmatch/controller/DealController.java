package com.insurmatch.controller;

import com.insurmatch.dto.ApiResponse;
import com.insurmatch.dto.deal.CreateDealRequest;
import com.insurmatch.dto.deal.DealResponse;
import com.insurmatch.dto.deal.UpdateStageRequest;
import com.insurmatch.entity.Activity;
import com.insurmatch.entity.Note;
import com.insurmatch.entity.Task;
import com.insurmatch.service.DealService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * DealController — Khớp 100% nghiệp vụ với Frontend (/dashboard/staff/deals):
 *   GET    /api/deals?search=&stage=&pipeline=&carrier=&owner=
 *   GET    /api/deals/:id
 *   POST   /api/deals
 *   PUT    /api/deals/:id
 *   PUT    /api/deals/:id/stage
 *   POST   /api/deals/:id/notes
 *   POST   /api/deals/:id/tasks
 *   GET    /api/deals/:id/activities
 */
@RestController
@RequestMapping("/api/deals")
@RequiredArgsConstructor
public class DealController {

    private final DealService dealService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DealResponse>>> getDeals(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String pipeline,
            @RequestParam(required = false) String carrier,
            @RequestParam(required = false) String owner) {
        List<DealResponse> deals = dealService.getAllDeals(search, stage, pipeline, carrier, owner);
        return ResponseEntity.ok(ApiResponse.success(deals));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DealResponse>> getDeal(@PathVariable String id) {
        DealResponse deal = dealService.getDealResponseById(id);
        return ResponseEntity.ok(ApiResponse.success(deal));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DealResponse>> createDeal(@RequestBody CreateDealRequest req) {
        DealResponse created = dealService.createDeal(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Hợp đồng bảo hiểm được tạo thành công!", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DealResponse>> updateDeal(
            @PathVariable String id,
            @RequestBody CreateDealRequest req) {
        DealResponse updated = dealService.updateDeal(id, req);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hợp đồng thành công!", updated));
    }

    @PutMapping("/{id}/stage")
    public ResponseEntity<ApiResponse<DealResponse>> updateDealStage(
            @PathVariable String id,
            @RequestBody UpdateStageRequest req) {
        DealResponse updated = dealService.updateStage(id, req);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công!", updated));
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<ApiResponse<Note>> addNote(
            @PathVariable String id,
            @RequestBody Note note) {
        Note saved = dealService.addNote(id, note);
        return ResponseEntity.ok(ApiResponse.success("Đã thêm ghi chú cho hợp đồng!", saved));
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<ApiResponse<Task>> addTask(
            @PathVariable String id,
            @RequestBody Task task) {
        Task saved = dealService.addTask(id, task);
        return ResponseEntity.ok(ApiResponse.success("Đã tạo nhiệm vụ cho hợp đồng!", saved));
    }

    @GetMapping("/{id}/activities")
    public ResponseEntity<ApiResponse<List<Activity>>> getDealActivities(@PathVariable String id) {
        List<Activity> activities = dealService.getActivities(id);
        return ResponseEntity.ok(ApiResponse.success(activities));
    }
}
