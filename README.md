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
  model/         Customer, Email, ZipCode, Address, PostalAddress
  port/          CustomerRepository, AddressProvider (interfaces)
  exception/     exceções de domínio
application/     casos de uso (sem Spring) + commands
infrastructure/  adapters: JPA, ViaCEP, cache, wiring dos beans
web/             controller REST, DTOs e tratamento de erros
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
| Value Object | `Email`, `ZipCode`, `Address` (imutáveis e autovalidados) |
| Command | `CreateCustomerCommand`, `ChangeAddressCommand` |
| Mapper | `CustomerEntityMapper`, `CustomerResponse.from(...)` |
| Optimistic Locking | `Customer.ensureVersion(...)` + `@Version` no `CustomerEntity` |

### SOLID na prática

- **S** — um caso de uso por classe (`CreateCustomerUseCase`, `DeleteCustomerUseCase`…).
- **O** — novos provedores de CEP ou cache entram por composição, sem mudar os casos de uso.
- **L** — qualquer `AddressProvider` (ViaCEP, cache, mock) é intercambiável.
- **I** — portas pequenas e específicas.
- **D** — casos de uso dependem de interfaces do domínio; a injeção é feita em `UseCaseConfiguration`.

## Endpoints

| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/api/customers` | Cria cliente (endereço resolvido pelo CEP) |
| `GET` | `/api/customers?name=&page=0&size=20` | Lista clientes paginada, com filtro por nome |
| `GET` | `/api/customers/stats` | Clientes por estado e cidade |
| `GET` | `/api/customers/{id}` | Busca cliente (devolve `ETag`) |
| `PUT` | `/api/customers/{id}/address` | Troca o endereço (aceita `If-Match`) |
| `PATCH` | `/api/customers/{id}/name` | Renomeia o cliente (aceita `If-Match`) |
| `DELETE` | `/api/customers/{id}` | Remove cliente |

A listagem é ordenada por nome; `name` busca por trecho sem diferenciar maiúsculas, `page` começa
em 0 e `size` vai de 1 a 100. A resposta traz `items`, `page`, `size`, `totalItems` e `totalPages`.
A paginação fica atrás da porta `CustomerRepository` (`PageResult` do domínio), então o caso de uso
não conhece o `Pageable` do Spring.

Erros seguem o padrão **RFC 7807** (`application/problem+json`):
`400` validação ou `If-Match` malformado · `404` cliente inexistente · `409` e-mail duplicado ·
`412` versão desatualizada · `422` CEP não encontrado · `503` ViaCEP fora do ar.

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
