package br.com.asobi.admin.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import br.com.asobi.admin.security.CurrentAdmin;
import jakarta.servlet.http.HttpServletRequest;

/** Dados comuns a todas as telas do painel (e-mail logado, item ativo do menu). */
@ControllerAdvice(annotations = Controller.class)
public class AdminModelAdvice {

	@ModelAttribute("currentAdminEmail")
	public String currentAdminEmail(Authentication authentication) {
		return CurrentAdmin.emailOf(authentication);
	}

	@ModelAttribute("currentPath")
	public String currentPath(HttpServletRequest request) {
		return request.getRequestURI();
	}
}
