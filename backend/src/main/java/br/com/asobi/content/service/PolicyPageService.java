package br.com.asobi.content.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.common.exception.NotFoundException;
import br.com.asobi.content.dto.PoliciesForm;
import br.com.asobi.content.dto.PolicyPageResponse;
import br.com.asobi.content.model.PolicyPage;
import br.com.asobi.content.repository.PolicyPageRepository;

@Service
@Transactional
public class PolicyPageService {

	private final PolicyPageRepository policyPageRepository;

	public PolicyPageService(PolicyPageRepository policyPageRepository) {
		this.policyPageRepository = policyPageRepository;
	}

	@Transactional(readOnly = true)
	public PolicyPageResponse getPolicy(String slug) {
		return PolicyPageResponse.from(load(slug));
	}

	@Transactional(readOnly = true)
	public PoliciesForm toForm() {
		PoliciesForm form = new PoliciesForm();
		List<PoliciesForm.PolicyForm> policies = policyPageRepository.findAllByOrderByPositionAsc().stream()
				.map(page -> {
					PoliciesForm.PolicyForm policy = new PoliciesForm.PolicyForm();
					policy.setSlug(page.getSlug());
					policy.setTitle(page.getTitle());
					policy.setBody(page.getBody());
					return policy;
				}).toList();
		form.setPolicies(policies);
		return form;
	}

	public void update(PoliciesForm form, String adminEmail) {
		for (PoliciesForm.PolicyForm policy : form.getPolicies()) {
			PolicyPage page = load(policy.getSlug());
			String body = policy.getBody().strip().replace("\r\n", "\n");
			if (!body.equals(page.getBody())) {
				page.setBody(body);
				page.setUpdatedByEmail(adminEmail);
			}
		}
	}

	private PolicyPage load(String slug) {
		return policyPageRepository.findById(String.valueOf(slug))
				.orElseThrow(() -> new NotFoundException("Política não encontrada."));
	}
}
