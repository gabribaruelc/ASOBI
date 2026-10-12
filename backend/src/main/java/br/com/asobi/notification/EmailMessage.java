package br.com.asobi.notification;

import java.util.List;

/** Um e-mail pronto para envio (o remetente vem da configuração). */
public record EmailMessage(List<String> to, String subject, String html) {
}
