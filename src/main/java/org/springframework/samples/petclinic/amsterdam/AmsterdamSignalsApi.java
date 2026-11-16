package org.springframework.samples.petclinic.amsterdam;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * The public part of the API of Signalen, the system the City of Amsterdam runs for the
 * reports of its residents about public space. Each method is one request, and each
 * argument is the query parameter of the same name. A null leaves the parameter out, and
 * a list sends the parameter once for each value, which is how the API takes several
 * categories. An answer stays a JSON object, so a GraphQL field reads the field of the
 * same name.
 */
@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE)
interface AmsterdamSignalsApi {

	@GetExchange("/signals/v1/public/signals/geography/")
	Map<String, Object> signalsGeography(@RequestParam(required = false) String bbox,
			@RequestParam(name = "maincategory_slug", required = false) List<String> maincategorySlug,
			@RequestParam(name = "category_slug", required = false) List<String> categorySlug,
			@RequestParam(required = false) String ordering);

	@GetExchange("/signals/v1/public/terms/categories/")
	Map<String, Object> categories(@RequestParam(required = false) Integer page,
			@RequestParam(name = "page_size", required = false) Integer pageSize);

	@GetExchange("/signals/v1/public/terms/categories/{slug}/")
	Map<String, Object> category(@PathVariable String slug);

	@GetExchange("/signals/v1/public/terms/categories/{maincategorySlug}/sub_categories/")
	Map<String, Object> subCategories(@PathVariable String maincategorySlug,
			@RequestParam(required = false) Integer page,
			@RequestParam(name = "page_size", required = false) Integer pageSize);

	@GetExchange("/signals/v1/public/terms/categories/{maincategorySlug}/sub_categories/{slug}/")
	Map<String, Object> subCategory(@PathVariable String maincategorySlug, @PathVariable String slug);

	@GetExchange("/signals/v1/public/areas/")
	Map<String, Object> areas(@RequestParam(name = "type_code", required = false) List<String> typeCode,
			@RequestParam(required = false) List<String> code, @RequestParam(required = false) Integer page,
			@RequestParam(name = "page_size", required = false) Integer pageSize,
			@RequestParam(required = false) String ordering);

	/**
	 * Registers the interface as an HTTP service in the group
	 * <code>amsterdam-signals</code>, whose base URL and timeouts come from
	 * <code>spring.http.serviceclient.amsterdam-signals.*</code>.
	 */
	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(group = "amsterdam-signals", types = AmsterdamSignalsApi.class)
	class Registration {

	}

}
