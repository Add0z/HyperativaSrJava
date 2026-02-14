# PROMPT FINAL – GERAÇÃO DE API REST

Gere uma **API REST completa** utilizando **Java 25** e **Spring Boot 4+**, seguindo **rigorosamente Arquitetura Hexagonal (Ports and Adapters)**, com foco em **segurança, escalabilidade, observabilidade e maturidade operacional**.

O sistema deve permitir **cadastro e consulta de números de cartão de crédito** de forma segura, conforme o desafio descrito, sem jamais expor dados sensíveis.

---

## 1️⃣ Requisitos Técnicos Obrigatórios

* Java 25
* Spring Boot 4+
* Arquitetura Hexagonal (domínio totalmente isolado de frameworks)
* Testes com JUnit 5 e Mockito
* MySQL, Redis, Prometheus e Grafana executando via Docker
* docker-compose.yml contendo toda a stack
* Logs estruturados em JSON
* Métricas expostas para Prometheus
* Documentação OpenAPI 3.x obrigatória
* API exposta exclusivamente sobre HTTPS
* HTTP/2 habilitado (HTTP/1.1 apenas como fallback)
* Liquibase para versionamento de schema
* Cache Redis com TTL e invalidação
* Autenticação JWT via Bearer Token
* Pool HikariCP otimizado
* Rate limiting distribuído
* Circuit breaker distribuído
* Configuração via variáveis de ambiente
* Configuração complementar via application.yml

---

## 2️⃣ Arquitetura

### Arquitetura Hexagonal

* Controllers atuam apenas como **adaptadores de entrada**
* Casos de uso explícitos (Application Services)
* Portas de entrada e saída bem definidas
* Domínio sem dependência de Spring, JPA, Redis ou HTTP
* Persistência implementada como adaptador de saída
* Cache, criptografia e mensageria como adaptadores de infraestrutura

---

## 3️⃣ Segurança e Criptografia

* O número do cartão **nunca** pode ser armazenado ou logado em texto puro
* Utilizar **AES-256-GCM** para criptografia do cartão em repouso
* Utilizar **HMAC-SHA-256** para gerar hash determinístico do cartão
* O hash deve ser usado para:

  * Consultas
  * Índices no banco
  * Cache Redis
* O número do cartão **jamais** deve ser retornado em respostas
* Chaves criptográficas fornecidas exclusivamente via variáveis de ambiente
* Serviço de criptografia deve ser definido como **porta de domínio**

---

## 4️⃣ Funcionalidades Obrigatórias

### Autenticação

* Autenticação via JWT Bearer Token
* JWT configurado no OpenAPI

### Cadastro

* Endpoint para inserção de cartão individual
* Endpoint para upload de arquivo TXT seguindo layout fornecido
* Inserção deve ser **idempotente**, baseada no hash do cartão
* Caso o cartão já exista, retornar o mesmo UUID
* **Validações obrigatórias antes da persistência:**

  * Validação de formato (apenas dígitos)
  * Validação de tamanho compatível com PAN
  * **Validação de integridade pelo algoritmo de Luhn**

### Consulta

* Endpoint para consulta de existência de cartão
* Entrada: número de cartão completo
* A busca deve ocorrer **exclusivamente via hash determinístico**
* Retorno:

  * UUID interno se existir
  * Indicação clara de inexistência se não existir
* Nenhum dado sensível deve ser retornado

---

## 5️⃣ Cache Redis

* Redis deve ser utilizado exclusivamente como **cache distribuído**
* Nenhum dado sensível deve ser armazenado no Redis
* Banco relacional é a única fonte da verdade

### Estratégia

* Chave do cache:

  * `card:exists:{card_hash}`
* Valor:

  * UUID interno

### TTL

* Cartão existente: ~24 horas
* Cartão inexistente: 5 a 10 minutos
* TTL configurável via propriedades

### Leitura e Escrita

* Estratégia read-through
* Atualização imediata após escrita no banco
* Eventual consistency é aceitável

---

## 6️⃣ Persistência e Banco de Dados

* Utilizar MySQL
* Schema criado **exclusivamente via Liquibase**
* Nenhum DDL manual permitido
* Migrations incrementais e versionadas
* Constraint UNIQUE no hash do cartão

### Pool de Conexões

* Utilizar HikariCP
* Pool enxuto e conservador
* Aplicação considerada majoritariamente CPU-bound
* Métricas do pool expostas via Prometheus

---

## 7️⃣ Rate Limiting, Circuit Breaker e Retry

### Rate Limiting

* Implementar rate limit antes de qualquer lógica de negócio
* Controle distribuído via Redis
* Métricas de bloqueio expostas

### Circuit Breaker

* Implementar circuit breaker distribuído
* Atuar antes da lógica de negócio
* Proteger contra falhas em banco, cache e serviços críticos

### Retry

* Utilizar **Spring Retry** como mecanismo oficial de retry
* Retry deve ser aplicado **apenas para falhas técnicas transitórias**
* Retry deve ocorrer **após rate limit e circuit breaker**
* Retry nunca deve ser aplicado para:

  * erros de validação
  * erros de negócio
  * violações de unicidade
  * falhas de autenticação/autorização

#### Configuração de Retry

* Máximo de 2 tentativas
* Backoff exponencial curto
* Retry restrito a exceções técnicas específicas (ex: timeouts, deadlocks)
* Retry deve ser transparente para o domínio

---

## 8️⃣ Protocolo e Transporte

* API exposta apenas via HTTPS
* TLS obrigatório em todos os ambientes
* HTTP/2 habilitado no servidor
* HTTP/1.1 apenas como fallback automático

---

## 9️⃣ Observabilidade

### Logs

* Logs estruturados em JSON
* Nunca logar:

  * número do cartão
  * hash do cartão
  * payload completo
* Sempre logar:

  * correlationId / requestId
  * UUID interno (quando existir)
  * tempo de execução

### Métricas

* Cache hit/miss
* Latência por endpoint
* Conexões Hikari (ativas, ociosas, pendentes)
* Rate limiting
* Circuit breaker

---

## 🔟 Testes

* Testes de domínio (sem Spring)
* Testes de casos de uso
* Testes de adaptadores com mocks
* Testes de integração usando Testcontainers (MySQL + Redis)
* Controllers testados apenas como contrato

---

## 1️⃣1️⃣ Contrato de Erros

* Padrão único de erro (ex: RFC 7807 – Problem Details)
* Códigos HTTP padronizados:

  * 400 – entrada inválida
  * 401 – não autenticado
  * 403 – não autorizado
  * 404 – cartão inexistente
  * 409 – cartão já cadastrado
  * 429 – rate limit
  * 503 – circuit breaker aberto
* Nenhum stack trace exposto

---

## 1️⃣2️⃣ Versionamento de API

* Versionar via URL (`/api/v1`)
* OpenAPI separado por versão
* Alterações devem preservar compatibilidade

---

## 1️⃣3️⃣ Health Checks

* Liveness probe
* Readiness probe
* Verificar conectividade com MySQL e Redis

---

## 1️⃣4️⃣ Documentação OpenAPI

* Utilizar springdoc-openapi compatível com Spring Boot 4+
* Documentar todos os endpoints
* Configurar JWT Bearer Token
* Centralizar configuração OpenAPI
* Expor documentação via:

  * `/swagger-ui.html`
  * `/v3/api-docs`

---

## 1️⃣5️⃣ Entregáveis

* Código completo
* docker-compose funcional
* Documento OpenAPI completo
* README explicando:

  * setup
  * arquitetura
  * decisões técnicas

---

## 🎯 Objetivo Final

O resultado deve ser uma API **robusta, segura, escalável e pronta para produção**, com nível arquitetural equivalente a sistemas financeiros ou fintechs modernas.
