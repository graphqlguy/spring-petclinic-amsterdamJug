package org.springframework.samples.petclinic.naturalis;

import java.util.List;
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
 * Answers the <code>naturalis*</code> queries of the GraphQL schema from the Netherlands
 * Biodiversity API. Every query is one request to Naturalis. The <code>querySpec</code>
 * argument goes out as the JSON body with the names and values the client gave, and the
 * answer goes back as Naturalis sent it. The types in
 * <code>graphql/naturalis.graphqls</code> name their fields after the properties of
 * Naturalis, so each field reads the property of the same name.
 */
@Controller
class NaturalisGraphQLController {

	static final String NATURALIS = "The Netherlands Biodiversity API of Naturalis";

	private final NaturalisApi naturalis;

	NaturalisGraphQLController(NaturalisApi naturalis) {
		this.naturalis = naturalis;
	}

	@QueryMapping
	Map<String, Object> naturalisTaxonQuery(@Argument Map<String, Object> querySpec) {
		return call(() -> this.naturalis.taxonQuery(orEmpty(querySpec)));
	}

	@QueryMapping
	Integer naturalisTaxonCount(@Argument Map<String, Object> querySpec) {
		return Integer.valueOf(call(() -> this.naturalis.taxonCount(orEmpty(querySpec))).strip());
	}

	@QueryMapping
	Map<String, Object> naturalisTaxonFind(@Argument String id) {
		return call(() -> this.naturalis.taxonFind(id));
	}

	@QueryMapping
	Map<String, Object> naturalisSpecimenQuery(@Argument Map<String, Object> querySpec) {
		return call(() -> this.naturalis.specimenQuery(orEmpty(querySpec)));
	}

	@QueryMapping
	Integer naturalisSpecimenCount(@Argument Map<String, Object> querySpec) {
		return Integer.valueOf(call(() -> this.naturalis.specimenCount(orEmpty(querySpec))).strip());
	}

	@QueryMapping
	Map<String, Object> naturalisSpecimenFind(@Argument String id) {
		return call(() -> this.naturalis.specimenFind(id));
	}

	@QueryMapping
	List<Map<String, Object>> naturalisSpecimenFindByUnitID(@Argument String unitID) {
		return call(() -> this.naturalis.specimenFindByUnitID(unitID));
	}

	@QueryMapping
	Boolean naturalisSpecimenExists(@Argument String unitID) {
		return Boolean.valueOf(call(() -> this.naturalis.specimenExists(unitID)).strip());
	}

	/**
	 * Naturalis reads a query from the body of the request, and an empty object asks for
	 * everything.
	 */
	private static Map<String, Object> orEmpty(Map<String, Object> querySpec) {
		return (querySpec != null) ? querySpec : Map.of();
	}

	/**
	 * Makes the call. <code>404 Not Found</code> becomes null. Naturalis answers
	 * <code>400 Bad Request</code> with the reason as text, for example a field that does
	 * not exist, and the client gets that reason so it can correct the query.
	 */
	private static <T> T call(Supplier<T> call) {
		try {
			return call.get();
		}
		catch (HttpClientErrorException.NotFound ex) {
			return null;
		}
		catch (HttpClientErrorException.BadRequest ex) {
			throw new InvalidArgumentException(
					"%s rejected the query: %s".formatted(NATURALIS, ex.getResponseBodyAsString().strip()));
		}
		catch (RestClientException ex) {
			throw new UpstreamException(NATURALIS, ex);
		}
	}

}
