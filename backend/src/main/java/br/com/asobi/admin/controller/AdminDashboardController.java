package br.com.asobi.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import br.com.asobi.admin.service.AdminDashboardService;
import br.com.asobi.settings.service.StoreSettingsService;

@Controller
public class AdminDashboardController {

	private final AdminDashboardService dashboardService;
	private final StoreSettingsService settingsService;

	public AdminDashboardController(AdminDashboardService dashboardService, StoreSettingsService settingsService) {
		this.dashboardService = dashboardService;
		this.settingsService = settingsService;
	}

	@GetMapping("/admin")
	public String dashboard(@RequestParam(name = "periodo", required = false) Integer period, Model model) {
		int days = period != null && AdminDashboardService.PERIODS.contains(period)
				? period
				: AdminDashboardService.DEFAULT_PERIOD;
		model.addAttribute("summary", dashboardService.summary());
		model.addAttribute("metrics", dashboardService.metrics(days));
		model.addAttribute("periods", AdminDashboardService.PERIODS);
		int threshold = settingsService.get().getLowStockThreshold();
		model.addAttribute("lowStockThreshold", threshold);
		model.addAttribute("lowStock", dashboardService.lowStock(threshold));
		return "admin/dashboard";
	}
}
