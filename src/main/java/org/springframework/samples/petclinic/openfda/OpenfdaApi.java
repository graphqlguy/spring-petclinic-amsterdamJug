package org.springframework.samples.petclinic.openfda;

import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * The animal and veterinary endpoint of openFDA, which publishes the adverse event
 * reports of the FDA Center for Veterinary Medicine. The endpoint is one operation, and
 * each argument is the query parameter of the same name. A null leaves the parameter out.
 * An answer stays a JSON object, so a GraphQL field reads the field of the same name.
 * <p>
 * Without a key, openFDA allows 240 requests a minute and 1,000 a day for each IP
 * address.
 */
@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE)
interface OpenfdaApi {

	/**
	 * Searches the reports, or counts them by the values of one field when
	 * <code>count</code> is set.
	 */
	@GetExchange("/animalandveterinary/event.json")
	Map<String, Object> animalAndVeterinaryEvents(@RequestParam(required = false) String search,
			@RequestParam(required = false) String sort, @RequestParam(required = false) String count,
			@RequestParam(required = false) Integer limit, @RequestParam(required = false) Integer skip);

	/**
	 * Registers the interface as an HTTP service in the group <code>openfda</code>, whose
	 * base URL and timeouts come from <code>spring.http.serviceclient.openfda.*</code>.
	 */
	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(group = "openfda", types = OpenfdaApi.class)
	class Registration {

	}

}
