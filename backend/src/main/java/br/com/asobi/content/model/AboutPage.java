package br.com.asobi.content.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/** Conteúdo da página "Sobre a ASOBI" (linha única, id = 1). */
@Entity
@Table(name = "about_page")
public class AboutPage {

	public static final long SINGLETON_ID = 1L;

	@Id
	private Long id;

	@Column(name = "hero_text", nullable = false, length = 4000)
	private String heroText;

	@Column(name = "mission_emoji", length = 16)
	private String missionEmoji;

	@Column(name = "mission_title", nullable = false, length = 200)
	private String missionTitle;

	@Column(name = "mission_text", nullable = false, length = 4000)
	private String missionText;

	@OneToMany(mappedBy = "aboutPage", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("position ASC")
	private List<AboutValue> values = new ArrayList<>();

	@Column(name = "updated_by_email", length = 254)
	private String updatedByEmail;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected AboutPage() {
	}

	public Long getId() {
		return id;
	}

	public String getHeroText() {
		return heroText;
	}

	public void setHeroText(String heroText) {
		this.heroText = heroText;
	}

	public String getMissionEmoji() {
		return missionEmoji;
	}

	public void setMissionEmoji(String missionEmoji) {
		this.missionEmoji = missionEmoji;
	}

	public String getMissionTitle() {
		return missionTitle;
	}

	public void setMissionTitle(String missionTitle) {
		this.missionTitle = missionTitle;
	}

	public String getMissionText() {
		return missionText;
	}

	public void setMissionText(String missionText) {
		this.missionText = missionText;
	}

	public List<AboutValue> getValues() {
		return values;
	}

	public String getUpdatedByEmail() {
		return updatedByEmail;
	}

	public void setUpdatedByEmail(String updatedByEmail) {
		this.updatedByEmail = updatedByEmail;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
