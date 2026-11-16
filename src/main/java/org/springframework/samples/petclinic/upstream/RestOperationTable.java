package org.springframework.samples.petclinic.upstream;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import graphql.schema.idl.RuntimeWiring;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.core.io.Resource;
import org.springframework.web.client.RestClient;

/**
 * The read operations of one REST service, as a generator wrote them from the service's
 * OpenAPI document into a JSON file next to the schema.
 *
 * @param operations the operations, one for each root field
 * @param mapFields the fields that hold a map in the service's answers, by type. The
 * schema has a list of entries with a key and a value for each of them.
 */
public record RestOperationTable(List<RestOperation> operations, Map<String, List<String>> mapFields) {

	public RestOperationTable {
		mapFields = (mapFields != null) ? mapFields : Map.of();
	}

	/**
	 * Reads the operations of one service from the JSON file that the generator wrote.
	 */
	public static RestOperationTable read(Resource resource, JsonMapper mapper) {
		try (InputStream json = resource.getInputStream()) {
			return mapper.readValue(json, RestOperationTable.class);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Cannot read the operations in " + resource, ex);
		}
	}

	/**
	 * Registers a {@link RestOperationFetcher} for each root field, and a fetcher that
	 * turns a map into entries for each map field.
	 * @param wiring the runtime wiring of the schema
	 * @param upstream the name of the service, in words a client can read
	 * @param baseUrl the URL the paths of the operations start from
	 * @param client the client that calls the service
	 * @param mapper the mapper that reads the JSON of the answers
	 */
	public void register(RuntimeWiring.Builder wiring, String upstream, String baseUrl, RestClient client,
			JsonMapper mapper) {
		wiring.type("Query", (type) -> {
			this.operations.forEach((operation) -> type.dataFetcher(operation.field(),
					new RestOperationFetcher(upstream, baseUrl, client, operation, mapper)));
			return type;
		});
		this.mapFields.forEach((typeName, fields) -> wiring.type(typeName, (type) -> {
			fields.forEach((field) -> type.dataFetcher(field,
					(environment) -> RestOperationFetcher.entries(((Map<?, ?>) environment.getSource()).get(field))));
			return type;
		}));
	}

}
