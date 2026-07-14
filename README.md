# URL Shortener

Encurtadores de URL parecem simples à primeira vista, mas escondem boa profundidade de engenharia: geração de identificadores sem colisão, alta frequência de leitura no redirecionamento, necessidade de cache para escalar, e controle de acesso por usuário. Este projeto foi construído do zero como exercício prático desses conceitos, documentando as decisões técnicas ao longo do caminho.

## Funcionalidades

- Registro e login de usuários com autenticação JWT
- Criação de links curtos vinculados ao usuário autenticado
- Redirecionamento público (sem necessidade de login)
- Contagem de cliques por link, processada de forma assíncrona
- Cache de leitura no redirecionamento (padrão cache-aside com Redis)
- Tratamento centralizado de erros com respostas HTTP consistentes

## Tecnologias

- Java 21 + Spring Boot 4.1
- Spring Security + JWT (jjwt) para autenticação stateless
- Spring Data JPA + PostgreSQL para persistência
- Spring Data Redis para cache
- Docker + Docker Compose para orquestração local
- GitHub Actions para CI/CD
- JUnit 5 + Mockito + H2 para testes

## Arquitetura

```
Cliente
  │
  ▼
Auth Filter (valida JWT)
  │
  ▼
Controller (REST endpoints)
  │
  ▼
Service (regras de negócio)
  │
  ├──► Redis (cache de leitura no redirect)
  │
  └──► PostgreSQL (fonte de verdade)
```

O fluxo de escrita (`POST /links`) sempre grava direto no Postgres. O fluxo de leitura (`GET /{code}`) consulta o Redis primeiro; se não encontrar, busca no Postgres e populariza o cache para as próximas leituras.

## Decisões técnicas

### Por que cache-aside com Redis

O endpoint de redirecionamento é, por natureza, o mais acessado da aplicação — cada clique num link bate nele. Sem cache, todo clique gera uma consulta ao Postgres, o que não escala bem se um link viralizar. Com o padrão cache-aside, a primeira leitura de um código busca no banco e guarda o resultado no Redis; leituras seguintes do mesmo código vêm direto da memória, com TTL de 10 minutos.

**Trade-off aceito:** como o TTL é de 10 minutos, é possível que um link editado ou deletado ainda apareça no cache por até esse tempo. Para este caso de uso — um link raramente muda depois de criado — essa janela de inconsistência é aceitável em troca do ganho de performance.

### Por que a contagem de cliques é assíncrona

Se a contagem de clique acontecesse dentro do mesmo método cacheado, o cache "congelaria" o valor na primeira leitura e o contador nunca mais atualizaria. Por isso a leitura da URL (cacheada) e a contagem de clique (assíncrona, via `@Async`) foram separadas em métodos distintos — a resposta do redirecionamento não espera a escrita do clique ser concluída.

### Por que CodeGenerator é uma interface

A geração do código curto foi extraída para uma interface (`CodeGenerator`), implementada por `RandomCodeGenerator`. Isso segue o princípio de inversão de dependência (DIP): o `ShortUrlService` depende de uma abstração, não de uma implementação concreta. Se no futuro fizer sentido trocar por um gerador sequencial em base62, a troca acontece sem tocar no service.

### Por que ShortUrlService não foi dividido em leitura/escrita

Cheguei a considerar separar o service em interfaces menores (`UrlReader`/`UrlWriter`), seguindo o princípio de segregação de interface (ISP). Decidi não seguir esse caminho: com apenas três operações coesas girando em torno da mesma entidade, essa separação adicionaria complexidade sem benefício real. SOLID não significa maximizar o número de interfaces — significa isolar as dependências que de fato variam, como fizemos com o `CodeGenerator`.

### Por que JWT em vez de sessão

A API é stateless por design (`SessionCreationPolicy.STATELESS`): nenhuma sessão é mantida no servidor. Cada requisição se autentica sozinha através do token enviado no header `Authorization`. Essa escolha facilita escalar horizontalmente — qualquer instância da aplicação pode validar qualquer token, sem precisar compartilhar estado de sessão entre instâncias.

## Como rodar

**Pré-requisito:** Docker e Docker Compose instalados.

```bash
git clone https://github.com/DudaFernand/url-shortener.git
cd url-shortener
```

Crie o arquivo `.env` a partir do template:

```bash
cp .env.example .env
# Edite o .env e defina sua JWT_SECRET
```

Suba tudo (aplicação, Postgres e Redis):

```bash
docker compose up -d --build
```

A API fica disponível em `http://localhost:8080`.

## Endpoints da API

| Método | Rota             | Auth               | Descrição                                    |
|--------|------------------|---------------------|----------------------------------------------|
| POST   | /auth/register   | Não                 | Cria um novo usuário e retorna um token JWT  |
| POST   | /auth/login      | Não                 | Autentica um usuário e retorna um token JWT  |
| POST   | /links           | Sim (Bearer token)  | Cria um link curto vinculado ao usuário       |
| GET    | /{code}          | Não                 | Redireciona para a URL original               |
| GET    | /links/{code}    | Sim (Bearer token)  | Estatísticas do link (cliques, data, etc.)    |

### Exemplo de criação de link

```
POST /links
Authorization: Bearer <token>
Content-Type: application/json

{
  "originalUrl": "https://www.google.com"
}
```

Resposta:

```json
{
  "code": "aB3xY9",
  "originalUrl": "https://www.google.com",
  "shortUrl": "http://localhost:8080/aB3xY9",
  "createdAt": "2026-07-13T10:00:00"
}
```

## Testes

O projeto tem testes unitários e de integração cobrindo service, repository, controller e a estratégia de geração de código.

```bash
./mvnw clean test
```

- **Testes unitários:** `ShortUrlServiceTest`, `RandomCodeGeneratorTest` — lógica de negócio isolada com Mockito
- **Testes de integração:** `ShortUrlRepositoryTest` (`@DataJpaTest`), `ShortUrlControllerTest` (`@WebMvcTest`) — validam a integração com o banco e a camada web

## CI/CD

O pipeline (GitHub Actions) roda a cada push ou pull request para `main`:

1. Sobe Postgres e Redis como serviços auxiliares
2. Configura Java 21
3. Roda `mvn clean verify` (compilação + testes)
4. Builda a imagem Docker da aplicação, validando que o Dockerfile está saudável

## Possíveis melhorias futuras

1. Rate limiting para evitar abuso do endpoint de criação de links
2. Paginação e filtros na listagem de links por usuário
3. Métricas de observabilidade (Prometheus/Grafana)
4. Expiração configurável de links com job de limpeza automática
