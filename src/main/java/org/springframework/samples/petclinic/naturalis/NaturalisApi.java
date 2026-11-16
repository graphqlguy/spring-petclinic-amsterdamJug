package org.springframework.samples.petclinic.naturalis;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * The Netherlands Biodiversity API of Naturalis Biodiversity Center, the natural history
 * museum of the Netherlands. Each method is one request. A query goes out as a
 * <code>QuerySpec</code> in the JSON body, and an answer stays a JSON object, so a
 * GraphQL field reads the property of the same name.
 * <p>
 * The count and the exists operations answer a number or a boolean as
 * <code>text/plain</code>.
 */
@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE)
interface NaturalisApi {

	@PostExchange(url = "/v2/taxon/query", contentType = MediaType.APPLICATION_JSON_VALUE)
	Map<String, Object> taxonQuery(@RequestBody Map<String, Object> querySpec);

	@PostExchange(url = "/v2/taxon/count", contentType = MediaType.APPLICATION_JSON_VALUE,
			accept = MediaType.TEXT_PLAIN_VALUE)
	String taxonCount(@RequestBody Map<String, Object> querySpec);

	@GetExchange("/v2/taxon/find/{id}")
	Map<String, Object> taxonFind(@PathVariable String id);

	@PostExchange(url = "/v2/specimen/query", contentType = MediaType.APPLICATION_JSON_VALUE)
	Map<String, Object> specimenQuery(@RequestBody Map<String, Object> querySpec);

	@PostExchange(url = "/v2/specimen/count", contentType = MediaType.APPLICATION_JSON_VALUE,
			accept = MediaType.TEXT_PLAIN_VALUE)
	String specimenCount(@RequestBody Map<String, Object> querySpec);

	@GetExchange("/v2/specimen/find/{id}")
	Map<String, Object> specimenFind(@PathVariable String id);

	@GetExchange("/v2/specimen/findByUnitID/{unitID}")
	List<Map<String, Object>> specimenFindByUnitID(@PathVariable String unitID);

	@GetExchange(url = "/v2/specimen/exists/{unitID}", accept = MediaType.TEXT_PLAIN_VALUE)
	String specimenExists(@PathVariable String unitID);

	/**
	 * Registers the interface as an HTTP service in the group <code>naturalis</code>,
	 * whose base URL and timeouts come from
	 * <code>spring.http.serviceclient.naturalis.*</code>.
	 */
	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(group = "naturalis", types = NaturalisApi.class)
	class Registration {

	}

}
