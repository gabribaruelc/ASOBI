package br.com.asobi.content.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.asobi.content.dto.AboutPageResponse;
import br.com.asobi.content.dto.PolicyPageResponse;
import br.com.asobi.content.dto.SiteInfoResponse;
import br.com.asobi.content.service.AboutPageService;
import br.com.asobi.content.service.PolicyPageService;
import br.com.asobi.content.service.SiteInfoService;

@RestController
@RequestMapping("/api/content")
public class ContentController {

	private final AboutPageService aboutPageService;
	private final SiteInfoService siteInfoService;
	private final PolicyPageService policyPageService;

	public ContentController(AboutPageService aboutPageService, SiteInfoService siteInfoService,
			PolicyPageService policyPageService) {
		this.aboutPageService = aboutPageService;
		this.siteInfoService = siteInfoService;
		this.policyPageService = policyPageService;
	}

	@GetMapping("/about")
	public AboutPageResponse getAboutPage() {
		return aboutPageService.getAboutPage();
	}

	@GetMapping("/site-info")
	public SiteInfoResponse getSiteInfo() {
		return siteInfoService.getSiteInfo();
	}

	/** slug: privacy, returns ou shipping. */
	@GetMapping("/policies/{slug}")
	public PolicyPageResponse getPolicy(@PathVariable String slug) {
		return policyPageService.getPolicy(slug);
	}
}
