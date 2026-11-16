package org.springframework.samples.petclinic.amsterdam;

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
 * Answers the <code>amsterdamSignals*</code> queries of the GraphQL schema from the
 * public API of the City of Amsterdam for the reports of its residents. Every query is
 * one request: the arguments go out under their own names, and the answer goes back as
 * the city sent it. The types in <code>graphql/amsterdam-signals.graphqls</code> name
 * their fields after the fields of the API, so each field reads the field of the same
 * name.
 */
@Controller
class AmsterdamSignalsGraphQLController {

	static final String SIGNALS = "The signals API of the City of Amsterdam";

	private final AmsterdamSignalsApi signals;

	AmsterdamSignalsGraphQLController(AmsterdamSignalsApi signals) {
		this.signals = signals;
	}

	@QueryMapping
	Map<String, Object> amsterdamSignalsGeography(@Argument String bbox,
			@Argument("maincategory_slug") List<String> maincategorySlug,
			@Argument("category_slug") List<String> categorySlug, @Argument String ordering) {
		return call(() -> this.signals.signalsGeography(bbox, maincategorySlug, categorySlug, ordering));
	}

	@QueryMapping
	Map<String, Object> amsterdamSignalsCategories(@Argument Integer page, @Argument("page_size") Integer pageSize) {
		return call(() -> this.signals.categories(page, pageSize));
	}

	@QueryMapping
	Map<String, Object> amsterdamSignalsCategory(@Argument String slug) {
		return call(() -> this.signals.category(slug));
	}

	@QueryMapping
	Map<String, Object> amsterdamSignalsSubCategories(@Argument("maincategory_slug") String maincategorySlug,
			@Argument Integer page, @Argument("page_size") Integer pageSize) {
		return call(() -> this.signals.subCategories(maincategorySlug, page, pageSize));
	}

	@QueryMapping
	Map<String, Object> amsterdamSignalsSubCategory(@Argument("maincategory_slug") String maincategorySlug,
			@Argument String slug) {
		return call(() -> this.signals.subCategory(maincategorySlug, slug));
	}

	@QueryMapping
	Map<String, Object> amsterdamSignalsAreas(@Argument("type_code") List<String> typeCode, @Argument List<String> code,
			@Argument Integer page, @Argument("page_size") Integer pageSize, @Argument String ordering) {
		return call(() -> this.signals.areas(typeCode, code, page, pageSize, ordering));
	}

	/**
	 * Makes the call. <code>404 Not Found</code> becomes null. The API answers
	 * <code>400 Bad Request</code> with the reason as JSON, in Dutch, for example a
	 * category it does not have, and the client gets that reason so it can correct the
	 * query.
	 */
	private static Map<String, Object> call(Supplier<Map<String, Object>> call) {
		try {
			return call.get();
		}
		catch (HttpClientErrorException.NotFound ex) {
			return null;
		}
		catch (HttpClientErrorException.BadRequest ex) {
			throw new InvalidArgumentException(
					"%s rejected the arguments: %s".formatted(SIGNALS, ex.getResponseBodyAsString().strip()));
		}
		catch (RestClientException ex) {
			throw new UpstreamException(SIGNALS, ex);
		}
	}

}
