package org.springframework.samples.petclinic.openfda;

import java.util.Map;
import java.util.function.Supplier;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.samples.petclinic.upstream.InvalidArgumentException;
import org.springframework.samples.petclinic.upstream.UpstreamException;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * Answers the <code>openfda*</code> queries of the GraphQL schema from openFDA. Every
 * query is one request: the arguments go out under their own names, and the answer goes
 * back as openFDA sent it. The types in <code>graphql/openfda.graphqls</code> name their
 * fields after the fields of openFDA, so each field reads the field of the same name.
 */
@Controller
class OpenfdaGraphQLController {

	static final String OPENFDA = "openFDA";

	private final OpenfdaApi openfda;

	OpenfdaGraphQLController(OpenfdaApi openfda) {
		this.openfda = openfda;
	}

	@QueryMapping
	Map<String, Object> openfdaAnimalAndVeterinaryEvents(@Argument String search, @Argument String sort,
			@Argument Integer limit, @Argument Integer skip) {
		return call(() -> this.openfda.animalAndVeterinaryEvents(search, sort, null, limit, skip));
	}

	@QueryMapping
	Map<String, Object> openfdaAnimalAndVeterinaryEventCounts(@Argument String search, @Argument String count,
			@Argument Integer limit) {
		return call(() -> this.openfda.animalAndVeterinaryEvents(search, null, count, limit, null));
	}

	/**
	 * Makes the call. openFDA answers <code>404 Not Found</code> when no report matches,
	 * which becomes null. It answers <code>400 Bad Request</code> with the reason as
	 * JSON, for example a limit above 1000, and the client gets that reason so it can
	 * correct the query.
	 */
	private static Map<String, Object> call(Supplier<Map<String, Object>> call) {
		try {
			return call.get();
		}
		catch (HttpClientErrorException.NotFound ex) {
			return null;
		}
		catch (HttpClientErrorException.BadRequest ex) {
			throw new InvalidArgumentException("%s rejected the arguments: %s".formatted(OPENFDA,
					ex.getResponseBodyAsString().replaceAll("\\s+", " ").strip()));
		}
		catch (RestClientException ex) {
			throw new UpstreamException(OPENFDA, ex);
		}
	}

}
