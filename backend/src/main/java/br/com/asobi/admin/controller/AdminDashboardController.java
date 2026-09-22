package br.com.asobi.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import br.com.asobi.admin.service.AdminDashboardService;

@Controller
public class AdminDashboardController {

	private final AdminDashboardService dashboardService;

	public AdminDashboardController(AdminDashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@GetMapping("/admin")
	public String dashboard(Model model) {
		model.addAttribute("summary", dashboardService.summary());
		return "admin/dashboard";
	}
}
