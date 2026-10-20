package br.com.asobi.common;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class StatusController {

	@GetMapping("/")
	public String index(Model model) {
		model.addAttribute("serviceName", "asobi-backend");
		return "index";
	}

	@GetMapping("/api/health")
	@ResponseBody
	public Map<String, String> health() {
		return Map.of("status", "ok");
	}
}
