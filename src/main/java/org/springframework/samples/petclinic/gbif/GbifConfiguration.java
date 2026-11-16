package org.springframework.samples.petclinic.gbif;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.samples.petclinic.upstream.RestOperationFetcher;
import org.springframework.samples.petclinic.upstream.UpstreamClients;
import org.springframework.web.client.RestClient;

/**
 * Exposes the read operations of GBIF, the Global Biodiversity Information Facility, as
 * root fields of the schema, one field for each operation. A generator wrote the fields,
 * their types and <code>gbif/operations.json</code> from GBIF's OpenAPI documents. One
 * {@link RestOperationFetcher} for each field calls GBIF.
 */
@Configuration(proxyBeanMethods = false)
class GbifConfiguration {

	/**
	 * The name of the service in errors, in words a client can read.
	 */
	static final String UPSTREAM = "GBIF";

	@Bean
	RuntimeWiringConfigurer gbifOperations(UpstreamClients clients,
			@Value("${petclinic.upstream.gbif.url}") String url) {
		RestClient client = clients.builder().build();
		return (wiring) -> clients.register(wiring, "gbif/operations.json", UPSTREAM, url, client);
	}

}
