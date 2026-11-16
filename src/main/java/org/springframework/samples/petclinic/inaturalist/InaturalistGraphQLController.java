package org.springframework.samples.petclinic.inaturalist;

import java.util.Map;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.Arguments;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.samples.petclinic.upstream.RestUpstreams;
import org.springframework.stereotype.Controller;

/**
 * Answers the <code>inaturalist*</code> queries of the GraphQL schema from the API of
 * iNaturalist. Every query is one request: the arguments go out under their own names,
 * and the answer goes back as iNaturalist sent it. The types in
 * <code>graphql/inaturalist.graphqls</code> name their fields after the fields of
 * iNaturalist, so each field reads the field of the same name.
 */
@Controller
class InaturalistGraphQLController {

	static final String INATURALIST = "The API of iNaturalist";

	private final InaturalistApi inaturalist;

	InaturalistGraphQLController(InaturalistApi inaturalist) {
		this.inaturalist = inaturalist;
	}

	@QueryMapping
	Map<String, Object> inaturalistPlaceAutocomplete(@Argument String q, @Argument("order_by") String orderBy,
			@Argument("per_page") Integer perPage) {
		return RestUpstreams.call(INATURALIST, () -> this.inaturalist.placeAutocomplete(q, orderBy, perPage));
	}

	@QueryMapping
	Map<String, Object> inaturalistPlaceDetails(@Argument String id) {
		return RestUpstreams.call(INATURALIST, () -> this.inaturalist.placeDetails(id));
	}

	@QueryMapping
	Map<String, Object> inaturalistTaxonSearch(@Argument String q, @Argument("is_active") Boolean isActive,
			@Argument("parent_id") Integer parentId, @Argument String rank, @Argument("rank_level") Integer rankLevel,
			@Argument("per_page") Integer perPage, @Argument Integer page, @Argument String locale,
			@Argument("preferred_place_id") Integer preferredPlaceId, @Argument String order,
			@Argument("order_by") String orderBy) {
		return RestUpstreams.call(INATURALIST, () -> this.inaturalist.taxonSearch(q, isActive, parentId, rank,
				rankLevel, perPage, page, locale, preferredPlaceId, order, orderBy));
	}

	@QueryMapping
	Map<String, Object> inaturalistTaxonDetails(@Argument String id) {
		return RestUpstreams.call(INATURALIST, () -> this.inaturalist.taxonDetails(id));
	}

	@QueryMapping
	Map<String, Object> inaturalistTaxonAutocomplete(@Argument String q, @Argument("is_active") Boolean isActive,
			@Argument("taxon_id") String taxonId, @Argument String rank, @Argument("rank_level") Integer rankLevel,
			@Argument("per_page") Integer perPage, @Argument String locale,
			@Argument("preferred_place_id") Integer preferredPlaceId) {
		return RestUpstreams.call(INATURALIST, () -> this.inaturalist.taxonAutocomplete(q, isActive, taxonId, rank,
				rankLevel, perPage, locale, preferredPlaceId));
	}

	/**
	 * The arguments of the query are the parameters of the request, so the map of the
	 * arguments goes to iNaturalist as it is.
	 */
	@QueryMapping
	Map<String, Object> inaturalistObservationSpeciesCounts(@Arguments Map<String, Object> arguments) {
		return RestUpstreams.call(INATURALIST, () -> this.inaturalist.observationSpeciesCounts(arguments));
	}

}
