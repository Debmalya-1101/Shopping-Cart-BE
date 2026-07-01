package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.DashboardAnalyticsDTO;
import com.demoproject.shoppingcart.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Get complete dashboard analytics
     * GET /api/admin/analytics/dashboard
     *
     * Returns:
     * - Total Users (excluding admins)
     * - Total Orders (successful payments only)
     * - Total Revenue
     * - Orders breakdown by status
     * - Monthly sales graph (12 months)
     * - Top 10 selling products
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardAnalyticsDTO> getDashboardAnalytics() {
        DashboardAnalyticsDTO analytics = analyticsService.getDashboardAnalytics();
        return ResponseEntity.ok(analytics);
    }
}

