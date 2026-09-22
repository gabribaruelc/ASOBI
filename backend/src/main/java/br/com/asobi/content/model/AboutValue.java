package br.com.asobi.content.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Card de valor da página Sobre (ícone + título + texto). */
@Entity
@Table(name = "about_values")
public class AboutValue {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "about_page_id", nullable = false)
	private AboutPage aboutPage;

	@Column(nullable = false)
	private int position;

	@Column(length = 16)
	private String icon;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, length = 1000)
	private String text;

	protected AboutValue() {
	}

	public AboutValue(AboutPage aboutPage, int position) {
		this.aboutPage = aboutPage;
		this.position = position;
	}

	public Long getId() {
		return id;
	}

	public int getPosition() {
		return position;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
