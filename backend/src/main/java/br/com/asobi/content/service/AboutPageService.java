package br.com.asobi.content.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import br.com.asobi.content.dto.AboutPageForm;
import br.com.asobi.content.dto.AboutPageResponse;
import br.com.asobi.content.model.AboutPage;
import br.com.asobi.content.model.AboutValue;
import br.com.asobi.content.repository.AboutPageRepository;
import br.com.asobi.common.exception.NotFoundException;

@Service
@Transactional
public class AboutPageService {

	private final AboutPageRepository aboutPageRepository;

	public AboutPageService(AboutPageRepository aboutPageRepository) {
		this.aboutPageRepository = aboutPageRepository;
	}

	@Transactional(readOnly = true)
	public AboutPageResponse getAboutPage() {
		return AboutPageResponse.from(load());
	}

	@Transactional(readOnly = true)
	public AboutPageForm toForm() {
		AboutPage page = load();
		AboutPageForm form = new AboutPageForm();
		form.setHeroText(page.getHeroText());
		form.setMissionEmoji(page.getMissionEmoji());
		form.setMissionTitle(page.getMissionTitle());
		form.setMissionText(page.getMissionText());
		form.setValues(page.getValues().stream().map(value -> {
			AboutPageForm.ValueForm valueForm = new AboutPageForm.ValueForm();
			valueForm.setIcon(value.getIcon());
			valueForm.setTitle(value.getTitle());
			valueForm.setText(value.getText());
			return valueForm;
		}).toList());
		return form;
	}

	public void update(AboutPageForm form, String adminEmail) {
		AboutPage page = load();
		page.setHeroText(form.getHeroText().trim());
		page.setMissionEmoji(blankToNull(form.getMissionEmoji()));
		page.setMissionTitle(form.getMissionTitle().trim());
		page.setMissionText(form.getMissionText().trim());
		page.setUpdatedByEmail(adminEmail);

		List<AboutValue> values = page.getValues();
		List<AboutPageForm.ValueForm> formValues = form.getValues();
		for (int i = 0; i < formValues.size(); i++) {
			if (i >= values.size()) {
				values.add(new AboutValue(page, i + 1));
			}
			AboutValue value = values.get(i);
			value.setIcon(blankToNull(formValues.get(i).getIcon()));
			value.setTitle(formValues.get(i).getTitle().trim());
			value.setText(formValues.get(i).getText().trim());
		}
		while (values.size() > formValues.size()) {
			values.remove(values.size() - 1);
		}
	}

	private AboutPage load() {
		return aboutPageRepository.findById(AboutPage.SINGLETON_ID)
				.orElseThrow(() -> new NotFoundException("Conteúdo da página Sobre não encontrado."));
	}

	private static String blankToNull(String text) {
		return StringUtils.hasText(text) ? text.trim() : null;
	}
}
