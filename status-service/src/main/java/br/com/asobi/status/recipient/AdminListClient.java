package br.com.asobi.status.recipient;

import java.util.List;

/** Quem é admin agora (fonte: admin-auth-service). */
public interface AdminListClient {

	boolean isConfigured();

	/** @throws RuntimeException se o serviço não responder */
	List<String> listAdminEmails();
}
