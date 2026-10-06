package br.com.asobi.account;

import java.util.UUID;

/**
 * Cliente logado na loja (Supabase Auth).
 *
 * @param userId        id do usuário no Supabase
 * @param email         e-mail em minúsculas
 * @param emailVerified se o provedor confirmou o e-mail (com Google, sempre)
 */
public record CustomerIdentity(UUID userId, String email, boolean emailVerified) {
}
