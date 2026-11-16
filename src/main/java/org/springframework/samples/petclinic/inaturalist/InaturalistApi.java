package org.springframework.samples.petclinic.inaturalist;

import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * The API of iNaturalist. Each method is one request, and each argument is the query
 * parameter of the same name. A null leaves the parameter out. A parameter that takes
 * several values takes them separated by commas, so it is a string here. An answer stays
 * a JSON object, so a GraphQL field reads the field of the same name.
 * <p>
 * iNaturalist asks for at most 60 requests a minute and 10,000 a day.
 */
@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE)
interface InaturalistApi {

	@GetExchange("/v1/places/autocomplete")
	Map<String, Object> placeAutocomplete(@RequestParam String q,
			@RequestParam(name = "order_by", required = false) String orderBy,
			@RequestParam(name = "per_page", required = false) Integer perPage);

	@GetExchange("/v1/places/{id}")
	Map<String, Object> placeDetails(@PathVariable String id);

	@GetExchange("/v1/taxa")
	Map<String, Object> taxonSearch(@RequestParam(required = false) String q,
			@RequestParam(name = "is_active", required = false) Boolean isActive,
			@RequestParam(name = "parent_id", required = false) Integer parentId,
			@RequestParam(required = false) String rank,
			@RequestParam(name = "rank_level", required = false) Integer rankLevel,
			@RequestParam(name = "per_page", required = false) Integer perPage,
			@RequestParam(required = false) Integer page, @RequestParam(required = false) String locale,
			@RequestParam(name = "preferred_place_id", required = false) Integer preferredPlaceId,
			@RequestParam(required = false) String order,
			@RequestParam(name = "order_by", required = false) String orderBy);

	@GetExchange("/v1/taxa/{id}")
	Map<String, Object> taxonDetails(@PathVariable String id);

	@GetExchange("/v1/taxa/autocomplete")
	Map<String, Object> taxonAutocomplete(@RequestParam String q,
			@RequestParam(name = "is_active", required = false) Boolean isActive,
			@RequestParam(name = "taxon_id", required = false) String taxonId,
			@RequestParam(required = false) String rank,
			@RequestParam(name = "rank_level", required = false) Integer rankLevel,
			@RequestParam(name = "per_page", required = false) Integer perPage,
			@RequestParam(required = false) String locale,
			@RequestParam(name = "preferred_place_id", required = false) Integer preferredPlaceId);

	/**
	 * Counts the observations of each species. The operation has 103 parameters, and the
	 * map holds the ones the query gave, under their own names.
	 */
	@GetExchange("/v1/observations/species_counts")
	Map<String, Object> observationSpeciesCounts(@RequestParam Map<String, Object> parameters);

	/**
	 * Registers the interface as an HTTP service in the group <code>inaturalist</code>,
	 * whose base URL and timeouts come from
	 * <code>spring.http.serviceclient.inaturalist.*</code>.
	 */
	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(group = "inaturalist", types = InaturalistApi.class)
	class Registration {

	}

}
