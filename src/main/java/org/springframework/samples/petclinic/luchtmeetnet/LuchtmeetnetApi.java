package org.springframework.samples.petclinic.luchtmeetnet;

import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * The open API of Luchtmeetnet, the air quality network of the Netherlands. Each method
 * is one request, and each argument is the query parameter of the same name. A null
 * leaves the parameter out. An answer stays a JSON object, so a GraphQL field reads the
 * attribute of the same name.
 * <p>
 * Luchtmeetnet allows 100 requests in 5 minutes.
 */
@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE)
interface LuchtmeetnetApi {

	@GetExchange("/open_api/stations")
	Map<String, Object> stations(@RequestParam(required = false) Integer page,
			@RequestParam(name = "order_by", required = false) String orderBy,
			@RequestParam(name = "organisation_id", required = false) Integer organisationId);

	@GetExchange("/open_api/stations/{number}")
	Map<String, Object> station(@PathVariable String number);

	@GetExchange("/open_api/stations/{number}/measurements")
	Map<String, Object> stationMeasurements(@PathVariable String number, @RequestParam(required = false) Integer page,
			@RequestParam(required = false) String order,
			@RequestParam(name = "order_direction", required = false) String orderDirection,
			@RequestParam(required = false) String formula);

	@GetExchange("/open_api/measurements")
	Map<String, Object> measurements(@RequestParam(name = "station_number", required = false) String stationNumber,
			@RequestParam(required = false) String formula, @RequestParam(required = false) Integer page,
			@RequestParam(name = "order_by", required = false) String orderBy,
			@RequestParam(name = "order_direction", required = false) String orderDirection,
			@RequestParam(required = false) String start, @RequestParam(required = false) String end);

	@GetExchange("/open_api/lki")
	Map<String, Object> lki(@RequestParam(name = "station_number", required = false) String stationNumber,
			@RequestParam(required = false) Integer page,
			@RequestParam(name = "order_by", required = false) String orderBy,
			@RequestParam(name = "order_direction", required = false) String orderDirection,
			@RequestParam(required = false) String start, @RequestParam(required = false) String end);

	@GetExchange("/open_api/concentrations")
	Map<String, Object> concentrations(@RequestParam String formula, @RequestParam double latitude,
			@RequestParam double longitude);

	@GetExchange("/open_api/components")
	Map<String, Object> components(@RequestParam(required = false) Integer page,
			@RequestParam(name = "order_by", required = false) String orderBy);

	@GetExchange("/open_api/components/{formula}")
	Map<String, Object> component(@PathVariable String formula);

	@GetExchange("/open_api/organisations")
	Map<String, Object> organisations(@RequestParam(required = false) Integer page);

	/**
	 * Registers the interface as an HTTP service in the group <code>luchtmeetnet</code>,
	 * whose base URL and timeouts come from
	 * <code>spring.http.serviceclient.luchtmeetnet.*</code>.
	 */
	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(group = "luchtmeetnet", types = LuchtmeetnetApi.class)
	class Registration {

	}

}
