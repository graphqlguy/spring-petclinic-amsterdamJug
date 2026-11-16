package org.springframework.samples.petclinic.eufeedadditives;

import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * The feed additives API of the European Commission's health department (DG SANTE). Each
 * method is one request, and each argument is the query parameter of the same name. A
 * null leaves the parameter out. Every request asks for JSON and for version 2.0 of the
 * API, which the Commission requires. An answer stays a JSON object, so a GraphQL field
 * reads the field of the same name.
 * <p>
 * Each operation accepts the parameters below and answers
 * <code>500 Internal server error</code> for any other.
 */
@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE)
interface EuFeedAdditivesApi {

	@GetExchange("/feed-additives/feed-additives-list?format=json&api-version=v2.0")
	Map<String, Object> list(@RequestParam(name = "additive_code", required = false) String additiveCode,
			@RequestParam(name = "$after", required = false) String after);

	@GetExchange("/feed-additives/feed-additives-details?format=json&api-version=v2.0")
	Map<String, Object> details(@RequestParam(name = "additive_code", required = false) String additiveCode,
			@RequestParam(name = "policy_item_code", required = false) String policyItemCode,
			@RequestParam(name = "policy_item_id", required = false) Integer policyItemId,
			@RequestParam(name = "$after", required = false) String after);

	@GetExchange("/feed-additives/feed-additives-authorizations?format=json&api-version=v2.0")
	Map<String, Object> authorizations(@RequestParam(name = "to_be_phased_out", required = false) String toBePhasedOut,
			@RequestParam(name = "cat_funct_grp", required = false) String catFunctGrp,
			@RequestParam(name = "$after", required = false) String after);

	/**
	 * Registers the interface as an HTTP service in the group
	 * <code>eu-feed-additives</code>, whose base URL and timeouts come from
	 * <code>spring.http.serviceclient.eu-feed-additives.*</code>.
	 */
	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(group = "eu-feed-additives", types = EuFeedAdditivesApi.class)
	class Registration {

	}

}
