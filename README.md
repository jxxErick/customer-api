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
| `GET` | `/api/customers` | Lista clientes |
| `GET` | `/api/customers/{id}` | Busca cliente |
| `PUT` | `/api/customers/{id}/address` | Troca o endereço |
| `DELETE` | `/api/customers/{id}` | Remove cliente |

Erros seguem o padrão **RFC 7807** (`application/problem+json`):
`400` validação · `404` cliente inexistente · `409` e-mail duplicado · `422` CEP não encontrado · `503` ViaCEP fora do ar.

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
