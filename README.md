# customer-api

API REST de cadastro de clientes com preenchimento automático de endereço via [ViaCEP](https://viacep.com.br).

Projeto de estudo focado em **Clean Architecture**, **SOLID**, **design patterns** e **testes unitários**.

## Stack

- Java 21 · Spring Boot 3.3
- Spring Web (`RestClient`) · Spring Data JPA · Bean Validation
- H2 (em memória)
- JUnit 5 · Mockito · AssertJ · MockMvc · MockRestServiceServer

## Arquitetura

```
domain/          regras de negócio puras (sem Spring)
  model/         Customer, Email, Cpf, ZipCode, Address, PostalAddress, IdempotencyKey
  port/          CustomerRepository, AddressProvider, IdempotencyStore (interfaces)
  exception/     exceções de domínio
application/     casos de uso (sem Spring) + commands
infrastructure/  adapters: JPA, ViaCEP, cache, limpeza agendada, wiring dos beans
web/             controller REST, DTOs, leitor de CSV e tratamento de erros
```

O domínio e os casos de uso não dependem de framework; o Spring só aparece nas bordas
(`infrastructure` e `web`), que implementam as portas do domínio.

### Padrões utilizados

| Padrão | Onde |
| --- | --- |
| Ports & Adapters (Hexagonal) | `CustomerRepository` / `AddressProvider` ← `JpaCustomerRepository`, `ViaCepAddressProvider` |
| Adapter | `ViaCepAddressProvider` traduz a API externa para o modelo do domínio |
| Decorator | `CachingAddressProvider` adiciona cache com TTL a qualquer `AddressProvider` |
| Factory Method | `Customer.create(...)` / `Customer.restore(...)` |
| Value Object | `Email`, `Cpf`, `ZipCode`, `Address`, `IdempotencyKey` (imutáveis e autovalidados) |
| Command | `CreateCustomerCommand`, `ChangeAddressCommand` |
| Mapper | `CustomerEntityMapper`, `CustomerResponse.from(...)` |
| Optimistic Locking | `Customer.ensureVersion(...)` + `@Version` no `CustomerEntity` |
| Idempotency Key | `IdempotentCreateCustomerUseCase` + porta `IdempotencyStore` |
| Fail fast | `ImportCustomersUseCase` para de chamar o ViaCEP depois da primeira falha |

### SOLID na prática

- **S** — um caso de uso por classe (`CreateCustomerUseCase`, `DeleteCustomerUseCase`…).
- **O** — novos provedores de CEP ou cache entram por composição, sem mudar os casos de uso.
- **L** — qualquer `AddressProvider` (ViaCEP, cache, mock) é intercambiável.
- **I** — portas pequenas e específicas.
- **D** — casos de uso dependem de interfaces do domínio; a injeção é feita em `UseCaseConfiguration`.

## Endpoints

| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/api/customers` | Cria cliente (endereço resolvido pelo CEP, aceita `Idempotency-Key`) |
| `POST` | `/api/customers/lookup` | Busca cliente pelo CPF (enviado no corpo) |
| `POST` | `/api/customers/import` | Importa clientes de um CSV (`text/csv` ou upload `file`) |
| `GET` | `/api/customers?name=&page=0&size=20` | Lista clientes paginada, com filtro por nome |
| `GET` | `/api/customers/stats` | Clientes por estado e cidade |
| `GET` | `/api/customers/{id}` | Busca cliente (devolve `ETag`) |
| `PUT` | `/api/customers/{id}/address` | Troca o endereço (aceita `If-Match`) |
| `PATCH` | `/api/customers/{id}/name` | Renomeia o cliente (aceita `If-Match`) |
| `GET` | `/api/customers/{id}/history?page=0&size=20` | Histórico de alterações (mais recente primeiro) |
| `DELETE` | `/api/customers/{id}` | Remove cliente |

A listagem é ordenada por nome; `name` busca por trecho sem diferenciar maiúsculas, `page` começa
em 0 e `size` vai de 1 a 100. A resposta traz `items`, `page`, `size`, `totalItems` e `totalPages`.
A paginação fica atrás da porta `CustomerRepository` (`PageResult` do domínio), então o caso de uso
não conhece o `Pageable` do Spring.

Erros seguem o padrão **RFC 7807** (`application/problem+json`):
`400` validação, CPF inválido, CSV ilegível ou cabeçalho malformado · `404` cliente inexistente ·
`409` e-mail ou CPF duplicado · `412` versão desatualizada · `413` CSV com mais de 1000 clientes ·
`422` CEP não encontrado ou `Idempotency-Key` reaproveitada · `503` ViaCEP fora do ar.

### Concorrência otimista (ETag / If-Match)

Cada cliente tem uma versão, que a API devolve no cabeçalho `ETag` (`"0"`, `"1"`...) no `POST`,
no `GET /{id}` e nas alterações. Quem quiser editar sem sobrescrever a mudança de outra pessoa manda
essa versão no `If-Match`:

```bash
curl -i -X PATCH http://localhost:8080/api/customers/6f1c.../name \
  -H "Content-Type: application/json" -H 'If-Match: "0"' -d '{"name":"Maria Souza"}'
```

Se alguém alterou o cliente nesse meio-tempo, a resposta é `412 Precondition Failed`. A checagem
acontece em duas camadas: o caso de uso compara o `If-Match` com a versão carregada (antes de
consultar o ViaCEP) e o `@Version` do JPA barra a corrida entre a leitura e a gravação, que o
adapter traduz para a mesma exceção de domínio. Sem `If-Match` (ou com `*`), a alteração passa
direto, como antes.

### CPF

O CPF é opcional no cadastro (`"cpf": "529.982.247-25"`, com ou sem pontuação). O value object
`Cpf` guarda só os 11 dígitos, confere os **dois dígitos verificadores** (módulo 11) e recusa
sequências repetidas como `111.111.111-11`. Ele é único entre os clientes (`409` se repetir) e
nunca sai inteiro da API: a resposta traz `"cpf": "***.982.247-**"` e até o `toString()` mascara,
para o documento não vazar em log.

A busca por CPF é um `POST /api/customers/lookup` com `{"cpf": "..."}` no corpo, e não um `GET`
com o CPF na URL, porque URLs acabam em logs de acesso, proxies e histórico do navegador.

### Idempotency-Key

Rede instável faz cliente repetir `POST`, e repetir `POST` cria cadastro duplicado. Quem manda o
cabeçalho `Idempotency-Key` (8 a 64 caracteres, normalmente um UUID) ganha a garantia de que o
cliente é criado uma vez só:

```bash
curl -i -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" -H "Idempotency-Key: 3f6e1c9a-7d41-4c55-9a0e-1b2c3d4e5f60" \
  -d '{"name":"Maria Silva","email":"maria@example.com","zipCode":"01001-000","number":"100"}'
```

- primeira vez: cria o cliente e guarda a chave com um **SHA-256 do corpo** e o id criado;
- mesma chave e mesmo corpo: devolve o mesmo cliente com `201` e `Idempotent-Replayed: true`,
  sem chamar o ViaCEP nem gravar nada;
- mesma chave com outro corpo: `422`, porque provavelmente é um bug de quem chama.

As chaves valem 24 horas (`idempotency.ttl`) e um job `@Scheduled` (`IdempotencyKeyCleanup`)
apaga as vencidas a cada hora. Sem o cabeçalho, o `POST` funciona como antes.

### Importação em lote (CSV)

`POST /api/customers/import` recebe o CSV direto no corpo (`Content-Type: text/csv`, UTF-8) ou
como upload `multipart/form-data` no campo `file`:

```bash
curl -X POST http://localhost:8080/api/customers/import -H "Content-Type: text/csv" --data-binary @clientes.csv
```

```csv
nome;e-mail;cep;número;complemento;cpf
"Silva, Maria";maria@example.com;01001-000;100;"Bloco ""B""";529.982.247-25
João Pereira;joao@example.com;20040020;5;;
```

- o leitor (`CsvParser`) segue a **RFC 4180**: campos entre aspas podem ter vírgula, quebra de
  linha e aspas duplicadas; aceita BOM, CRLF e detecta `;` (padrão do Excel em pt-BR) pelo cabeçalho;
- o cabeçalho pode vir em inglês (`name,email,zipCode,number,complement,cpf`) ou português, em
  qualquer ordem e com ou sem acento; colunas desconhecidas são ignoradas;
- cada linha passa pelas **mesmas regras do cadastro unitário** (inclusive e-mail/CPF repetidos
  dentro do próprio arquivo), e uma linha ruim não impede as boas;
- se o ViaCEP cair no meio, as linhas seguintes falham na hora em vez de esperar o timeout uma a uma.

A resposta é um relatório por linha (o número é a linha física do arquivo, para achar no Excel):

```json
{
  "total": 2, "created": 1, "failed": 1,
  "rows": [
    { "line": 2, "status": "CREATED", "email": "maria@example.com", "customerId": "6f1c...", "error": null },
    { "line": 3, "status": "FAILED", "email": "joao@example.com", "customerId": null, "error": "E-mail já cadastrado: joao@example.com" }
  ]
}
```

### Estatísticas

`GET /api/customers/stats` agrupa os clientes por estado e cidade com uma única consulta
`GROUP BY` (projeção do Spring Data); a montagem, a ordenação (maior primeiro) e as porcentagens
ficam em `CustomerStats`, no domínio.

```json
{
  "totalCustomers": 9,
  "states": [
    { "state": "SP", "customers": 5, "percentage": 55.6,
      "cities": [ { "city": "São Paulo", "customers": 4 }, { "city": "Campinas", "customers": 1 } ] }
  ]
}
```

### Histórico de alterações

Toda mudança num cliente vira uma entrada de auditoria (`CustomerChange`): `CREATED`, `RENAMED`,
`ADDRESS_CHANGED` e `DELETED`, cada uma com os campos que mudaram (`from` → `to`). O endereço
entra como uma linha só (`Praça da Sé, 100 - Apto 12, Sé, São Paulo/SP, 01001-000`) e o CPF nunca
aparece no histórico.

- Os casos de uso gravam pela porta `CustomerHistory`; o adaptador JPA guarda os campos numa
  `@ElementCollection` ordenada.
- Renomear para o mesmo nome ou "mudar" para o mesmo endereço não gera entrada.
- A tabela não tem chave estrangeira para `customers` de propósito: o histórico **sobrevive à
  exclusão**, então `GET /api/customers/{id}/history` continua respondendo depois do `DELETE`.
  Um id que nunca existiu devolve 404.

```json
{
  "items": [
    { "type": "RENAMED", "occurredAt": "2026-11-23T14:02:11Z",
      "changes": [ { "field": "name", "from": "Maria Silva", "to": "Maria Souza" } ] },
    { "type": "CREATED", "occurredAt": "2026-11-23T13:58:40Z",
      "changes": [ { "field": "name", "from": null, "to": "Maria Silva" }, "..." ] }
  ],
  "page": 0, "size": 20, "totalItems": 2, "totalPages": 1
}
```

### Exemplo

```bash
curl -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{"name":"Maria Silva","email":"maria@example.com","zipCode":"01001-000","number":"100"}'
```

```json
{
  "id": "6f1c...",
  "name": "Maria Silva",
  "email": "maria@example.com",
  "cpf": null,
  "address": {
    "zipCode": "01001-000",
    "street": "Praça da Sé",
    "number": "100",
    "complement": null,
    "neighborhood": "Sé",
    "city": "São Paulo",
    "state": "SP"
  },
  "createdAt": "2026-11-16T12:00:00Z"
}
```

## Como rodar

```bash
mvn spring-boot:run
```

```bash
mvn test
```

Configuração do ViaCEP (timeouts e TTL do cache) em `src/main/resources/application.yml`.
