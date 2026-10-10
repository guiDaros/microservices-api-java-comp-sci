# Documentação do entregavel de MicroServiços

OBS: Tive que recomeçar um repositorio do zero, pois o que comecei inicialmente, acabei nao me atentando e ele estava dentro de uma outra pasta git. Para deixar o repositorio de forma "limpa" optei por começar outro repositorio.

## Visão geral

O repositório contém dois microsserviços Spring Boot independentes (Java 21, Spring Boot 4.1.1, Spring Cloud 2025.1.3):

| Serviço | Porta | Banco | Responsabilidade |
|---|---|---|---|
| `product-api` | 8000 | `db_product` | Catálogo de produtos (consulta por id e listagem paginada) |
| `currency-api` | 8100 | `db_currency` | Taxas de câmbio entre pares de moedas |

Cada serviço segue as camadas Controller → Service → Repository (Spring Data JPA) → Entity. O schema e os dados iniciais são criados pelo Flyway (`src/main/resources/db/migration`).

> Atualmente os serviços **não se comunicam entre si**. O campo `convertedPrice` da `product-api` é sempre `0` e `targetCurrency` é apenas devolvido na resposta.

## Pré-requisitos

- JDK 21
- PostgreSQL acessível em `localhost:5432`
- Consul (opcional)

## Configuração do PostgreSQL

Crie os bancos antes de subir os serviços (o Flyway cria as tabelas e popula os dados, mas não cria os bancos):

```sql
CREATE DATABASE db_product;
CREATE DATABASE db_currency;
```

As credenciais podem ser informadas por variáveis de ambiente. Sem elas, são usados os valores padrão de desenvolvimento local:

| Variável | Serviço | Padrão |
|---|---|---|
| `PRODUCT_DB_URL` | product-api | `jdbc:postgresql://localhost/db_product` |
| `CURRENCY_DB_URL` | currency-api | `jdbc:postgresql://localhost/db_currency` |
| `DB_USERNAME` | ambos | `postgres` |
| `DB_PASSWORD` | ambos | `postgres` |

## Consul

Os serviços usam o Consul **apenas como fonte de configuração** (KV), via `spring.config.import=optional:consul:`. Por ser `optional`, os serviços sobem normalmente sem o Consul. Não há registro/descoberta de serviços.

O script `start_consul.bat` sobe um agente em modo dev e grava chaves em `config/product-api/` e `config/currency-api/`. Quando o Consul está ativo, esses valores têm precedência sobre o `application.properties` — por exemplo, `app.promotion.message` passa a ser `Promoção Semana do Cliente`.

## Perfis da product-api

A mensagem de promoção (`app.promotion.message`) pode ser sobrescrita por perfil:

| Perfil | Mensagem |
|---|---|
| (padrão) | `Christmas Promotion` |
| `es` | `Promoción de Navidad` |
| `it` | `Promozione Natalizia` |

Exemplo: `./mvnw spring-boot:run -Dspring-boot.run.profiles=es`

## Como executar

1. (Opcional) Rode `start_consul.bat`.
2. Crie os bancos `db_product` e `db_currency` no PostgreSQL.
3. Em terminais separados, suba o `product-api` (porta 8000) e o `currency-api` (porta 8100) com `./mvnw spring-boot:run` dentro de cada projeto.
4. Teste `http://localhost:8100/currencies?source=USD&target=BRL`.

Para rodar os testes: `./mvnw test` dentro de cada projeto. Os testes são unitários (Mockito e MockMvc standalone) e não dependem de PostgreSQL nem de Consul.

## Endpoints

Erros são retornados como texto simples no corpo da resposta.

### currency-api

#### `GET /currencies?source={codigo}&target={codigo}`

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `source` | query | sim | Código ISO 4217 da moeda de origem |
| `target` | query | sim | Código ISO 4217 da moeda de destino |

Os códigos são normalizados (espaços removidos e convertidos para maiúsculas), então `usd` equivale a `USD`.

| Status | Situação |
|---|---|
| 200 | Par encontrado |
| 400 | Parâmetro obrigatório ausente (`Parâmetro obrigatório ausente: source`) |
| 404 | Par não cadastrado (`Cotação não encontrada`) |
| 500 | Erro inesperado (`Ops!!! Algo deu errado.`) |

Resposta 200:

```json
{
  "sourceCurrency": "USD",
  "targetCurrency": "BRL",
  "conversionRate": 5.15,
  "environment": "Currency API running in Port: 8100"
}
```

Pares cadastrados (migration `V2`): `USD` → `BRL`, `EUR`, `GBP`, `ARS`, `CLP`, `COP`, `MXN`, `PEN`, `UYU`, `PYG`. Apenas a direção cadastrada é suportada — pares inversos (ex.: `BRL` → `USD`) ou entre outras moedas (ex.: `EUR` → `BRL`) retornam 404.

### product-api

#### `GET /products/{id}?targetCurrency={codigo}`

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `id` | path (número) | sim | Id do produto |
| `targetCurrency` | query | sim | Moeda de destino (atualmente apenas ecoada na resposta) |

| Status | Situação |
|---|---|
| 200 | Produto encontrado |
| 400 | `targetCurrency` ausente ou `id` não numérico |
| 404 | Produto inexistente (`Produto não encontrado`) |
| 500 | Erro inesperado |

Resposta 200:

```json
{
  "id": 1,
  "brand": "Apple",
  "model": "iPhone 15",
  "description": "Smartphone Apple com tela Super Retina XDR, chip A16 Bionic e câmera principal de 48 MP.",
  "currency": "USD",
  "price": 699.0,
  "image": "https://example.com/images/iphone-15.jpg",
  "environment": "Product API running in port 8000",
  "promotionMessage": "Christmas Promotion",
  "targetCurrency": "BRL",
  "convertedPrice": 0.0
}
```

#### `GET /products?targetCurrency={codigo}&page={n}&size={n}&sort={campo,dir}`

| Parâmetro | Obrigatório | Padrão | Descrição |
|---|---|---|---|
| `targetCurrency` | sim | — | Moeda de destino (atualmente apenas ecoada) |
| `page` | não | `0` | Página (começa em 0) |
| `size` | não | `10` | Itens por página |
| `sort` | não | `description` | Campo da entidade e direção, ex.: `price,desc` |

Retorna um objeto de página do Spring Data (`content`, `totalElements`, `totalPages`, etc.) com itens no mesmo formato do endpoint por id.

| Status | Situação |
|---|---|
| 200 | Página retornada (pode ter `content` vazio) |
| 400 | `targetCurrency` ausente ou campo de `sort` inexistente |
| 500 | Erro inesperado |

## Testes manuais realizados

- Tive pequenos impecilios com o consul, mas consegui fazer ele funcionar. Foram realizados testes manuais chamando o endpoint via HTTP (resultados abaixo).

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

## Limitações conhecidas

- `convertedPrice` é sempre `0`: não há integração entre `product-api` e `currency-api`.
- Não há endpoint de cadastro de produtos.
- `conversion_rate` é `DECIMAL(10,2)`: taxas são armazenadas com 2 casas decimais.
- Pares de moedas inversos não são calculados automaticamente.
