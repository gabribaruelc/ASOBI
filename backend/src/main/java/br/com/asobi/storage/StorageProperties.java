package br.com.asobi.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param supabaseUrl endereço do projeto no Supabase (https://&lt;id&gt;.supabase.co); vazio = pasta local
 * @param supabaseKey chave secreta do Supabase (service_role / sb_secret_...), nunca a "anon"
 * @param bucket      bucket público das fotos de produto
 * @param localDir    pasta usada quando o Supabase não está configurado (desenvolvimento)
 */
@ConfigurationProperties("asobi.storage")
public record StorageProperties(String supabaseUrl, String supabaseKey, String bucket, String localDir) {
}
