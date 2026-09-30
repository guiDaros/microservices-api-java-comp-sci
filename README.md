# Documentação do entregavel de MicroServiços

OBS: Tive que recomeçar um repositorio do zero, pois o que comecei inicialmente, acabei nao me atentando e ele estava dentro de uma outra pasta git. Para deixar o repositorio de forma "limpa" optei por começar outro repositorio.

## Como executar

1. Rode `start_consul.bat`.
2. Crie os bancos `db_product` e `db_currency` no PostgreSQL.
3. Em terminais separados, suba o `product-api` (porta 8000) e o `currency-api` (porta 8100) com `./mvnw spring-boot:run` dentro de cada projeto.
4. Teste `http://localhost:8100/currencies?source=USD&target=BRL`.

## Consul e testes

- Tive pequenos impecilios com o consul, mas consegui fazer ele funcionar. Foram realizados testes manuais chamando o endpoint via HTTP (resultados abaixo). Também há testes unitários do CurrencyServiceJpa (Mockito).
- Então fiz alguns testes para validar, batendo na porta da API. Seguem os testes:

#### Conversao das moedas: 
1. USD para BRL
```bash
http://localhost:8100/currencies?source=USD&target=BRL
```
Resultado obtido:
```json
{
  "sourceCurrency": "USD",
  "targetCurrency": "BRL",
  "conversionRate": 5.15,
  "environment": "Currency API running in Port: 8100"
}
```
---
2. EUR para BRL
```bash
http://localhost:8100/currencies?source=EUR&target=BRL
```
Resultado obtido:
```json
Cotação não encontrada
```
--- 
3. USD para EUR
```bash
http://localhost:8100/currencies?source=USD&target=EUR
```
Resultado obtido:
```json
{
  "sourceCurrency": "USD",
  "targetCurrency": "EUR",
  "conversionRate": 0.87,
  "environment": "Currency API running in Port: 8100"
}
```