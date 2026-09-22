package br.com.asobi.content.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.asobi.content.dto.AboutPageResponse;
import br.com.asobi.content.service.AboutPageService;

@RestController
@RequestMapping("/api/content")
public class ContentController {

	private final AboutPageService aboutPageService;

	public ContentController(AboutPageService aboutPageService) {
		this.aboutPageService = aboutPageService;
	}

	@GetMapping("/about")
	public AboutPageResponse getAboutPage() {
		return aboutPageService.getAboutPage();
	}
}
