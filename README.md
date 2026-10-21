# Documentação do entregavel de MicroServiços

Microservices: `gateway-api`, `auth-api`, `product-api` e `currency-api`, com Consul para configuração centralizada e descoberta de serviços.

OBS: Tive que recomeçar um repositorio do zero, pois o que comecei inicialmente, acabei nao me atentando e ele estava dentro de uma outra pasta git. Para deixar o repositorio de forma "limpa" optei por começar outro repositorio.

## Arquitetura

```
Cliente ──> gateway-api :8080 ──(lb:// via Consul)──> auth-api     :8200  (cadastro, login, JWT)
              │ valida JWT nas rotas /ws/**          ├─> product-api  :8000
              │ propaga X-User-Id/Type/Email         └─> currency-api :8100
              └──────────────── Consul :8500 (config + service discovery)
```

- **Autenticação** (quem é o usuário) é centralizada no `gateway-api`: o `AuthFilter` (GlobalFilter) valida o JWT
  de toda rota `/ws/**`, remove os cabeçalhos `X-User-*` enviados pelo cliente e os recria a partir dos claims do token.
- **Autorização** (o que o usuário pode fazer) fica em cada microservice (`AuthorizationService.requireAdmin`).
  Tipo `0` = Admin, tipo `1` = Common.
- O roteamento usa `lb://<serviço>`: o Gateway descobre as instâncias no Consul e o Spring Cloud LoadBalancer
  distribui as requisições (round-robin).

### Rotas

| Rota (via gateway :8080)          | Acesso                     | Serviço      |
|-----------------------------------|----------------------------|--------------|
| `POST /auth/signup`               | Pública                    | auth-api     |
| `POST /auth/signin`               | Pública (retorna o JWT)    | auth-api     |
| `GET  /products`, `/products/{id}`| Pública                    | product-api  |
| `GET  /currencies`                | Pública                    | currency-api |
| `GET  /ws/auth/me`                | Qualquer usuário logado    | auth-api     |
| `GET  /ws/auth/users`             | Somente Admin              | auth-api     |
| `POST /ws/products`               | Somente Admin              | product-api  |
| `PUT  /ws/products/{id}`          | Somente Admin              | product-api  |
| `DELETE /ws/products/{id}`        | Somente Admin              | product-api  |
| `POST /ws/currencies`             | Somente Admin              | currency-api |

Sem token / token inválido ou expirado → `401` (barrado no gateway). Token válido sem permissão → `403` (barrado no serviço).

## Como executar

1. Rode `start_consul.bat` (Windows) ou `./start_consul.sh` (Linux/macOS).
2. Crie os bancos `db_product`, `db_currency` e `db_user` no PostgreSQL.
3. Em terminais separados, suba com `./mvnw spring-boot:run` dentro de cada projeto, nesta ordem:
   `auth-api` (8200), `currency-api` (8100), `product-api` (8000) e `gateway-api` (8080).
4. Confira em `http://localhost:8500` se os quatro serviços estão registrados e *passing*.
5. A partir daqui, todas as requisições passam pelo gateway: `http://localhost:8080/...`

Na primeira execução o `auth-api` cria o administrador `admin@admin.dev` / `teste` (credenciais apenas didáticas).

## Testes de autenticação e autorização (via gateway)

```bash
# 1. Login do administrador -> copie o "token"
curl -X POST localhost:8080/auth/signin -H 'Content-Type: application/json' \
     -d '{"email":"admin@admin.dev","password":"teste"}'

# 2. Cadastrar produto SEM token -> 401 Unauthorized
curl -i -X POST localhost:8080/ws/products -H 'Content-Type: application/json' \
     -d '{"description":"POCO X8 Pro Global","brand":"POCO","model":"POCO X8 Pro","currency":"USD","price":395.5}'

# 3. Cadastrar produto com token do ADMIN -> 201 Created
curl -i -X POST localhost:8080/ws/products -H 'Content-Type: application/json' -H "Authorization: Bearer $ADMIN_TOKEN" \
     -d '{"description":"POCO X8 Pro Global","brand":"POCO","model":"POCO X8 Pro","currency":"USD","price":395.5}'

# 4. Cadastrar usuário comum, fazer login e tentar cadastrar produto -> 403 Forbidden
curl -X POST localhost:8080/auth/signup -H 'Content-Type: application/json' \
     -d '{"name":"Novo Usuário NÃO ADMIN","email":"user@atitus.edu.br","password":"Teste123"}'
curl -X POST localhost:8080/auth/signin -H 'Content-Type: application/json' \
     -d '{"email":"user@atitus.edu.br","password":"Teste123"}'
curl -i -X POST localhost:8080/ws/products -H 'Content-Type: application/json' -H "Authorization: Bearer $USER_TOKEN" \
     -d '{"description":"POCO X8 Pro Global","brand":"POCO","model":"POCO X8 Pro","currency":"USD","price":395.5}'
```

Resultados obtidos:

| Cenário                                                        | Resultado            |
|----------------------------------------------------------------|----------------------|
| Login admin / senha errada                                     | 200 + JWT / 401      |
| `POST /ws/products` sem token / token inválido                 | 401                  |
| `POST /ws/products` sem token, forjando `X-User-Type: 0`       | 401                  |
| `POST /ws/products` com token admin                            | 201                  |
| `POST /ws/products` admin com dados inválidos                  | 400 (`Marca inválida`)|
| Cadastro de usuário comum / e-mail repetido                    | 201 / 409            |
| `POST /ws/products` com token comum (mesmo forjando `X-User-Type: 0`) | 403           |
| `GET /ws/auth/me` comum / `GET /ws/auth/users` comum / admin   | 200 / 403 / 200      |
| `POST /ws/currencies` comum / admin                            | 403 / 201            |
| `PUT` e `DELETE /ws/products/{id}` admin / `DELETE` comum      | 200, 204 / 403       |

### Observações de segurança (ambiente didático)

- A chave JWT fica em `app.jwt.secret` (application.properties) e é sobrescrita pelo Consul em
  `config/application/app.jwt.secret`, sendo compartilhada entre `auth-api` (assina) e `gateway-api` (valida).
- Os serviços confiam nos cabeçalhos `X-User-*`; em produção o acesso direto às portas dos microservices
  deve ser bloqueado (rede interna), deixando apenas o gateway exposto.
- O cadastro público sempre cria usuários `Common`; não é possível se auto-promover a Admin.
- As senhas são armazenadas com BCrypt e nunca retornadas nas respostas.

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