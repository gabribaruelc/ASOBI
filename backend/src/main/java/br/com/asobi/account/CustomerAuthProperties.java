package br.com.asobi.account;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param supabaseUrl endereço do projeto no Supabase; vazio = login de cliente desligado
 * @param supabaseKey chave do Supabase enviada como "apikey" ao conferir o login
 */
@ConfigurationProperties("asobi.customer-auth")
public record CustomerAuthProperties(String supabaseUrl, String supabaseKey) {
}
