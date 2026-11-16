# Spring Petclinic for agents: the Amsterdam JUG demo

The demo project of a talk at the **Amsterdam JUG**, presented by **Željko Kozina**. It turns [Spring Petclinic](https://github.com/spring-projects/spring-petclinic) into a GraphQL API with Spring for GraphQL, and then puts that API in front of AI agents with GATool: as MCP tools, as the tools of an assistant inside the application, and as a schema that an agent explores by itself.

## Branches

The demo is built up on the branches below, and each branch adds to the one before it. `main` is the original Spring Petclinic. `git switch <branch>` moves to a step, and `git diff <branch>` compares your code with it.

| Branch | What it adds |
| --- | --- |
| [`checkpoint-01-graphql-api`](../../tree/checkpoint-01-graphql-api) | A GraphQL API on Spring Petclinic with Spring for GraphQL: queries for owners and vets with paging, filtering and sorting, the pets of an owner and the visits of a pet and of a vet read in batches to avoid the N+1 problem, mutations with input types for the five writes of the clinic, a `Date` scalar, GraphiQL to try them, and a React frontend that uses the API |
| [`checkpoint-02-curated-operations`](../../tree/checkpoint-02-curated-operations) | GATool, which serves the frontend's operation files as MCP tools at `/mcp` |
| [`checkpoint-03-in-app-assistant`](../../tree/checkpoint-03-in-app-assistant) | An assistant in the frontend's chat panel: Spring AI's `ChatClient` on a model in Ollama, with the same operation files as tools inside the application |
| [`checkpoint-04-dynamic-tools`](../../tree/checkpoint-04-dynamic-tools) | GATool's three dynamic tools (`searchSchema`, `introspectType` and `executeGraphql`) in place of the operation files, over MCP and for the assistant, with an embedding model in Ollama to search the schema |
| [`checkpoint-05-large-schema`](../../tree/checkpoint-05-large-schema) | A large schema: public data from Amsterdam, the Netherlands, the EU and others, read over REST, OData and SOAP, and a mock mode that answers from recorded responses |

## What you need

| Tool | Version | Check with |
| --- | --- | --- |
| JDK | 21 or newer | `java -version` |
| Node.js | 22 or 24, for the frontend and the MCP Inspector | `node -v` |
| [Ollama](https://ollama.com/download), from `checkpoint-03-in-app-assistant` on | tested with 0.40.13 | `ollama list` |

Pull the two models the application uses. The assistant needs the chat model from `checkpoint-03-in-app-assistant` on, and the schema search needs the embedding model from `checkpoint-04-dynamic-tools` on:

```bash
# the chat model of the assistant, 5.2 GB
ollama pull qwen3:8b

# an embedding model tuned for GraphQL schemas, 639 MB, under the name the project uses
ollama pull hf.co/xthor/Qwen3-Embedding-0.6B-GraphQL:Q8_0
ollama cp hf.co/xthor/Qwen3-Embedding-0.6B-GraphQL:Q8_0 qwen3-graphql-embedder
```

`spring.ai.ollama.chat.model` in `src/main/resources/application.properties` names the chat model, so another model that calls tools works as well.

## Running the backend

```bash
./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd` in place of `./mvnw`.

- The original Petclinic web pages: http://localhost:8080
- GraphiQL: http://localhost:8080/graphiql
- The MCP endpoint, from `checkpoint-02-curated-operations` on: http://localhost:8080/mcp

From `checkpoint-04-dynamic-tools` on, the application embeds the schema with the embedding model at its first start and caches the result, so later starts are faster.

## Running the frontend

Install its packages the first time, then start it:

```bash
cd frontend
npm install    # the first time only
npm run dev
```

Open http://localhost:5173. If you see `sh: vite: command not found`, run `npm install` first.

The frontend reads the backend's schema at startup and shows the pages that the API can serve. From `checkpoint-03-in-app-assistant` on, the chat panel opens the assistant.

## Connecting an MCP client

From `checkpoint-02-curated-operations` on, an MCP client can connect. The MCP Inspector is a browser client for MCP servers:

```bash
npx -y @modelcontextprotocol/inspector@2.9.0
```

Connect it to `http://localhost:8080/mcp` with the Streamable HTTP transport. The endpoint answers without a sign-in, which suits a demo on a laptop.

## The public services

On this branch the schema grows from the clinic's 6 root fields to 292. The other 286 read ten public services, and each service has a schema file of its own in `src/main/resources/graphql`:

| File | What it reads | Who runs it | Read over | Root fields |
| --- | --- | --- | --- | --- |
| `gbif.graphqls` | The names and classification of species, their occurrences, and the datasets and collections behind them | GBIF, the Global Biodiversity Information Facility | REST | 226 |
| `rasff.graphqls` | Notifications about risks found in food, feed and pet food | RASFF, the EU's Rapid Alert System for Food and Feed | REST | 12 |
| `cbs.graphqls` | Dutch statistics, such as pet prices and farm animals | CBS, Statistics Netherlands | OData | 11 |
| `luchtmeetnet.graphqls` | The air quality of the Netherlands | Luchtmeetnet | REST | 9 |
| `naturalis.graphqls` | Species and museum specimens | Naturalis Biodiversity Center | REST | 8 |
| `amsterdam-signals.graphqls` | The reports of Amsterdam residents about public space, such as loose dogs | The City of Amsterdam | REST | 6 |
| `inaturalist.graphqls` | The animals seen in a place | iNaturalist | REST | 6 |
| `eu-feed-additives.graphqls` | The EU register of feed additives | The European Commission | REST | 3 |
| `europepmc.graphqls` | Veterinary research papers | Europe PMC | SOAP | 3 |
| `openfda.graphqls` | Adverse reactions to veterinary medicines | openFDA | REST | 2 |

GBIF's 226 fields are generated from its OpenAPI documents, and one fetcher serves them all from the operation table in `src/main/resources/gbif/operations.json`. The other services are written by hand, each in a package of its own. Pages count from 0 in the fields that take `page` and `size`, as they do for the clinic's owners.

At its first start on this branch, GATool embeds 2,606 schema coordinates, which took 30 seconds in one run on a Mac. One `searchSchema` call returns up to 5,000 tokens of results.

## Without wifi

Start the backend with the `mock` profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=mock
```

A [WireMock](https://wiremock.org) server inside the application then answers in place of the ten services, on port 9091, from the 303 answers recorded in `src/main/resources/mock-upstreams`. The profile also sets the application's clock to 15 November 2026 at 17:00 UTC, the time of the newest recordings, so the assistant takes that day as today. The tests start the application with this profile, so they run without the internet.

## Questions to ask

Ask them in the frontend's chat panel, or in an MCP client connected to `/mcp`. The results were read from the live services through `executeGraphql` on 15 November 2026, and a live answer can differ today.

- **What is the share of household spending that goes to pets in the Netherlands?** About 1.2% in 2026, which is 0.87% for pets and pet products plus 0.33% for veterinary services (CBS table 86144ENG, the HICP). The CPI table 86141ENG gives 0.74% and 0.27%, about 1.0%, so the answer depends on the table the model picks.
- **Did veterinary care get more expensive in the Netherlands over the past year?** Up 5.5% in October 2026 against October 2025. October is the latest month CBS has.
- **Did veterinary care or pet products get more expensive in the Netherlands in 2026?** Veterinary care. In October 2026 it was up 5.5% on a year before, and pets and pet products were up 1.0%. In every month of 2026 veterinary care was up between 5.5% and 6.6%, and pets and pet products changed between -0.8% and 3.7%.
- **Where in Amsterdam did residents report loose or aggressive dogs?** 16 reports on the city's public map, each with its coordinates and the time it was made. The newest is from 15 November 2026. The city adds and closes reports during the day, so the number changes.
- **Is taurine authorised in the food of cats and dogs in the EU, and until when?** Yes, for Canidae, Felidae, Mustelidae and carnivorous fish, until 19 August 2036 (Regulation (EU) 2026/1859). The earlier authorisation of 2015 is being phased out and ends on 19 August 2028.
- **How many research papers are there about parvovirus vaccines for dogs, and which is the newest?** 2,734 papers for the search "canine parvovirus vaccine". The newest is "Canine parvovirus in Africa: Explaining persistent disease despite vaccination." in Vet Anim Sci, 5 September 2026. The count depends on the words the model searches for.

The questions about CBS take complex inputs, filters and comparisons.

## Changing the port

If port 8080 is taken, start the backend on another port:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8090
```

Then tell the frontend where the backend is:

```bash
PETCLINIC_BACKEND=http://localhost:8090 npm run dev
```

In Windows PowerShell, that is `$env:PETCLINIC_BACKEND="http://localhost:8090"; npm run dev`.

## Sample data

The ten owners Spring Petclinic comes with, and Dev Oxx of Overtoom 100 in Amsterdam, with the dog Woofy and the cat Rosa: 11 owners, 15 pets and 10 visits, each visit with its vet.

## Versions

| Component | Version |
|-----------|---------|
| Java | 21 or newer |
| Spring Boot | 4.1.0 |
| GATool | 0.1.0, from `checkpoint-02-curated-operations` on |
| Maven | 3.9.16, through the Maven wrapper `./mvnw` |
| Node.js | 22 or 24 |
| Frontend | React 19.2.4, Vite 8.0.1, graphql-js 16.13.1 |
| Database | H2, in memory |

## Credits

Built on [Spring Petclinic](https://github.com/spring-projects/spring-petclinic) by the Spring team. See [LICENSE.txt](LICENSE.txt).
