package br.com.asobi.shipping.quote;

/**
 * @param name       transportadora/agregador usado nas cotações
 * @param configured tem credenciais para cotar? (sem isso o painel não deixa ligar o frete)
 */
public record ProviderStatusResponse(String name, boolean configured) {
}
