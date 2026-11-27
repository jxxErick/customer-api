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
  model/         Customer, Email, Cpf, ZipCode, Address, PostalAddress, IdempotencyKey, Tag, TrashedCustomer
  port/          CustomerRepository, AddressProvider, IdempotencyStore, CustomerTrash (interfaces)
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
| `GET` | `/api/customers?name=&tag=&state=&city=&page=0&size=20` | Lista clientes paginada, com filtro por nome, tags, UF e cidade |
| `GET` | `/api/customers/export?name=` | Exporta clientes em CSV (mesmo formato da importação) |
| `GET` | `/api/customers/stats` | Clientes por estado e cidade |
| `GET` | `/api/customers/tags` | Tags em uso e quantos clientes têm cada uma |
| `GET` | `/api/customers/duplicates?minScore=0.85` | Prováveis clientes duplicados, com nota e motivo |
| `GET` | `/api/customers/{id}` | Busca cliente (devolve `ETag`) |
| `PUT` | `/api/customers/{id}/address` | Troca o endereço (aceita `If-Match`) |
| `PATCH` | `/api/customers/{id}/name` | Renomeia o cliente (aceita `If-Match`) |
| `PUT` | `/api/customers/{id}/tags` | Troca as tags do cliente (aceita `If-Match`) |
| `GET` | `/api/customers/{id}/history?page=0&size=20` | Histórico de alterações (mais recente primeiro) |
| `POST` | `/api/customers/{id}/merge` | Junta um duplicado neste cliente (aceita `If-Match`) |
| `DELETE` | `/api/customers/{id}` | Remove cliente (vai para a lixeira) |
| `GET` | `/api/customers/trash?page=0&size=20` | Lixeira: clientes excluídos, com data do expurgo |
| `POST` | `/api/customers/trash/{id}/restore` | Restaura o cliente com o mesmo id |
| `DELETE` | `/api/customers/trash/{id}` | Apaga da lixeira para sempre |
| `POST` | `/api/segments` | Salva um segmento (filtro com nome) |
| `GET` | `/api/segments` | Segmentos com a contagem atual de clientes |
| `GET` | `/api/segments/{id}/customers?page=0&size=20` | Clientes do segmento |
| `DELETE` | `/api/segments/{id}` | Apaga o segmento (os clientes ficam) |

A listagem é ordenada por nome; `name` busca por trecho sem diferenciar maiúsculas, `page` começa
em 0 e `size` vai de 1 a 100. A resposta traz `items`, `page`, `size`, `totalItems` e `totalPages`.
A paginação fica atrás da porta `CustomerRepository` (`PageResult` do domínio), então o caso de uso
não conhece o `Pageable` do Spring.

Erros seguem o padrão **RFC 7807** (`application/problem+json`):
`400` validação, CPF inválido, CSV ilegível ou cabeçalho malformado · `404` cliente ou segmento inexistente (ou fora da lixeira) ·
`409` e-mail ou CPF duplicado (também ao restaurar), merge de CPFs diferentes ou nome de segmento repetido · `412` versão desatualizada · `413` CSV com mais de 1000 clientes ·
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

### Exportação (CSV)

`GET /api/customers/export?name=` baixa os clientes (todos ou só os que batem com o filtro) como
`customers-AAAA-MM-DD.csv`, separado por `;` e com BOM UTF-8 para o Excel em pt-BR abrir com
acento certo.

- As colunas são as mesmas da importação (mais rua, bairro, cidade, UF e data de criação, que a
  importação ignora), então **um export pode ser importado de volta** em outra base.
- A resposta é um `StreamingResponseBody`: o `ExportCustomersUseCase` percorre o banco de 500 em
  500 e escreve cada linha direto na resposta, sem montar a tabela inteira na memória.
- Proteção contra **CSV injection**: células que começam com `=`, `+`, `-`, `@`, tab ou CR ganham
  um `'` na frente (o Excel não executa como fórmula); a importação remove esse `'` de volta.
- O CPF fica de fora de propósito: a API nunca entrega o CPF sem máscara.

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
`ADDRESS_CHANGED`, `TAGS_CHANGED`, `DELETED`, `RESTORED`, `MERGED` e `MERGED_INTO`, cada uma com os campos que mudaram (`from` → `to`). O endereço
entra como uma linha só (`Praça da Sé, 100 - Apto 12, Sé, São Paulo/SP, 01001-000`) e o CPF nunca
aparece no histórico (só mascarado, quando um merge herda o CPF).

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

### Duplicados e mesclagem

Cadastro em lote e formulário aberto geram clientes repetidos com nome escrito diferente.
`GET /api/customers/duplicates?minScore=0.85` compara todos os pares e devolve os prováveis
duplicados, do mais parecido para o menos:

- os nomes são normalizados (sem acento, minúsculos, só letras) e comparados com **Jaro-Winkler**,
  também com as palavras ordenadas, então `Silva Maria` bate com `Maria Silva` e `Maira` com `Maria`;
- o mesmo usuário de e-mail (`Maria.Silva+promo@gmail.com` e `mariasilva@hotmail.com`, ignorando
  pontos e o `+tag`) soma 0,10 e o mesmo CEP soma 0,05, com teto de 1;
- cada par diz o porquê: `SAME_NAME`, `SIMILAR_NAME`, `SAME_EMAIL_USER`, `SAME_ZIP_CODE`;
- a comparação é O(n²) de propósito (pega erro de digitação em qualquer letra, até na primeira) e
  devolve no máximo 200 pares; `minScore` vai de 0,5 a 1.

```json
[
  { "first": { "id": "6f1c...", "name": "Maria Silva", "...": "..." },
    "second": { "id": "a93e...", "name": "Maira Silva", "...": "..." },
    "score": 0.976, "reasons": ["SIMILAR_NAME", "SAME_ZIP_CODE"] }
]
```

`POST /api/customers/{id}/merge` com `{"duplicateId": "..."}` junta o duplicado no cliente `{id}`:

- o cliente `{id}` mantém nome, e-mail e endereço e **herda o CPF** do duplicado se não tiver um;
- CPFs diferentes significam pessoas diferentes: `409` e nada muda;
- o duplicado é apagado **antes** de o alvo gravar o CPF herdado, para a constraint única não reclamar;
- aceita `If-Match` com a versão do alvo e devolve o alvo com o `ETag` novo;
- o histórico ganha `MERGED` no alvo (`mergedFrom` e o CPF herdado, mascarado) e `MERGED_INTO` no
  duplicado, que continua consultável depois de apagado.

### Tags

Etiquetas livres para segmentar clientes (`vip`, `zona-sul`, `atacado`), até 10 por cliente:

```bash
curl -X PUT http://localhost:8080/api/customers/6f1c.../tags \
  -H "Content-Type: application/json" -H 'If-Match: "2"' -d '{"tags":["VIP","Zona Sul"]}'
```

- o value object `Tag` normaliza o que foi digitado: minúsculas, sem acento, espaços e `_` viram
  hífen (`Área VIP` → `area-vip`); sobra só `[a-z0-9]` separado por hífen, com até 30 caracteres;
- o `PUT` manda o conjunto inteiro (lista vazia limpa tudo), repetições se juntam e a resposta vem
  em ordem alfabética; aceita `If-Match` como as outras alterações;
- o histórico ganha `TAGS_CHANGED` com antes e depois (`"vip"` → `"vip, zona-sul"`); mandar as
  mesmas tags não gera entrada;
- `GET /api/customers?tag=vip&tag=zona-sul` traz só quem tem **todas** as tags pedidas e combina
  com `name`; tag inválida no filtro é `400`. A busca virou uma `Specification` do Spring Data
  (`CustomerSpecifications`), um `isMember` por tag, então o total da paginação continua certo;
- `GET /api/customers/tags` lista as tags em uso, da mais usada para a menos (bom para autocomplete);
- a lixeira guarda as tags junto com o cliente; o merge mantém só as tags do alvo, e a exportação
  CSV não leva tags (o formato continua o mesmo da importação).

### Lixeira

Excluir sem querer não é mais definitivo. O `DELETE /api/customers/{id}` tira o cliente da tabela
`customers` e guarda uma cópia na lixeira (`customer_trash`) por **30 dias** (`trash.retention`):

- `GET /api/customers/trash` lista os excluídos (o último primeiro) com `deletedAt` e `purgeAt`;
- `POST /api/customers/trash/{id}/restore` devolve o cliente com **o mesmo id**, os mesmos dados e
  a data de cadastro original; a versão recomeça (`ETag: "0"`) e o histórico ganha `RESTORED`;
- se alguém cadastrou o mesmo e-mail ou CPF enquanto ele estava na lixeira, a restauração dá `409`
  e o cliente continua lá, em vez de virar um duplicado;
- `DELETE /api/customers/trash/{id}` apaga para sempre na hora; o histórico continua consultável;
- um job `@Scheduled` (`TrashPurgeJob`, a cada hora) expurga o que passou do `purgeAt`. A data do
  expurgo é gravada na exclusão, então mudar a retenção vale só para as próximas;
- a lixeira é uma tabela à parte, e não um `deleted = true` na `customers`, para que listagem,
  busca, exportação e as constraints únicas de e-mail e CPF continuem enxergando só clientes ativos;
- o duplicado apagado por um merge **não** vai para a lixeira: ele já foi absorvido pelo outro cliente.

```json
{
  "items": [
    { "customer": { "id": "6f1c...", "name": "Maria Silva", "cpf": "***.982.247-**", "...": "..." },
      "deletedAt": "2026-11-26T14:02:11Z", "purgeAt": "2026-12-26T14:02:11Z" }
  ],
  "page": 0, "size": 20, "totalItems": 1, "totalPages": 1
}
```

### Segmentos

Um segmento é um **filtro salvo com nome** ("VIPs de SP", "Atacado no Rio"). Ele guarda os
critérios, não os clientes, então está sempre atualizado com a base:

```bash
curl -X POST http://localhost:8080/api/segments -H "Content-Type: application/json" \
  -d '{"name":"VIPs SP","tags":["VIP"],"state":"sp"}'
```

- critérios: `nameContains`, `tags` (todas), `state` (UF) e `city` (igual, sem diferenciar
  maiúsculas), combinados com **E**. Os mesmos filtros valem na listagem:
  `GET /api/customers?state=SP&city=Campinas&tag=vip`;
- os critérios passam pelas mesmas regras do resto da API: tags normalizadas (`VIP` vira `vip`) e UF
  com duas letras (`sp` vira `SP`, `São Paulo` é `400`). Segmento sem nenhum critério também é `400`
  (para "todos" já existe a listagem);
- nome único sem diferenciar maiúsculas: `VIPs SP` e `vips sp` não convivem (`409`);
- `GET /api/segments` lista por nome com a contagem **ao vivo** de cada um (`customers`), e
  `GET /api/segments/{id}/customers` pagina os clientes do segmento com as mesmas regras da listagem;
- por dentro, `CustomerFilter` é um value object do domínio, compartilhado entre a listagem e os
  segmentos, e vira uma `Specification` só no adaptador JPA; a porta ganhou `findMatching` e
  `countMatching`;
- apagar o segmento (`DELETE /api/segments/{id}`) não mexe em cliente nenhum.

```json
{ "id": "3b0e...", "name": "VIPs SP",
  "criteria": { "nameContains": null, "tags": ["vip"], "state": "SP", "city": null },
  "customers": 12, "createdAt": "2026-11-27T20:10:00Z" }
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

## Front-end

Há uma interface web em Angular que usa esta API e a [expense-split-api](https://github.com/jxxErick/expense-split-api):
**[customer-expense-web](https://github.com/jxxErick/customer-expense-web)** (lista com busca, cadastro,
edição com `If-Match`, histórico, duplicados e mesclagem).

A API libera `http://localhost:4200` no CORS e expõe o `ETag` para o navegador; outra origem pode
ser configurada em `CORS_ALLOWED_ORIGINS` (lista separada por vírgula).

## Como rodar

```bash
mvn spring-boot:run
```

```bash
mvn test
```

Configuração do ViaCEP (timeouts e TTL do cache) em `src/main/resources/application.yml`.
