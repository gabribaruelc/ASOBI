package br.com.asobi.twofactor.email;

import java.time.Duration;
import java.util.Locale;

import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

/** Monta o e-mail com o código (HTML do template email/code.html + versão em texto). */
@Component
public class CodeEmailComposer {

	private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

	private final ITemplateEngine templateEngine;

	public CodeEmailComposer(ITemplateEngine templateEngine) {
		this.templateEngine = templateEngine;
	}

	public EmailMessage compose(String email, String code, Duration validity) {
		String validFor = validity.toMinutes() + " minutos";
		Context context = new Context(PT_BR);
		context.setVariable("code", code);
		context.setVariable("validFor", validFor);
		String text = "Seu código de verificação da ASOBI é " + code + ".\n"
				+ "Ele vale por " + validFor + " e só pode ser usado uma vez.\n"
				+ "Se não foi você quem pediu, é só ignorar este e-mail. Nunca passe este código para ninguém.";
		return new EmailMessage(email, "Seu código de verificação — ASOBI",
				templateEngine.process("email/code", context), text);
	}
}
