package org.springframework.samples.petclinic.cbs;

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
 * The open data of Statistics Netherlands (CBS), an OData version 3 service. Each method
 * is one request, and the arguments <code>filter</code>, <code>top</code> and
 * <code>skip</code> go to CBS as the OData query options of the same name. A null leaves
 * the option out.
 * <p>
 * CBS serves a table at two addresses. <code>ODataApi</code> refuses <code>$skip</code>
 * and answers at most 10,000 rows, so the methods call <code>ODataFeed</code>, which
 * accepts both and answers JSON for the <code>Accept</code> header below.
 */
@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE)
interface CbsApi {

	@GetExchange("/ODataCatalog/Tables")
	Rows tables(@RequestParam(name = "$filter", required = false) String filter,
			@RequestParam(name = "$top", required = false) Integer top,
			@RequestParam(name = "$skip", required = false) Integer skip);

	@GetExchange("/ODataFeed/odata/{table}/TableInfos")
	Rows tableInfos(@PathVariable String table);

	@GetExchange("/ODataFeed/odata/{table}/DataProperties")
	Rows dataProperties(@PathVariable String table);

	@GetExchange("/ODataFeed/odata/{table}/CategoryGroups")
	Rows categoryGroups(@PathVariable String table);

	@GetExchange("/ODataFeed/odata/{table}/Periods")
	Rows periods(@PathVariable String table, @RequestParam(name = "$filter", required = false) String filter,
			@RequestParam(name = "$top", required = false) Integer top,
			@RequestParam(name = "$skip", required = false) Integer skip);

	/**
	 * Returns the categories of one dimension of a table. CBS names the list after the
	 * dimension, for example <code>ExpenditureCategories</code>.
	 */
	@GetExchange("/ODataFeed/odata/{table}/{dimension}")
	Rows categories(@PathVariable String table, @PathVariable String dimension,
			@RequestParam(name = "$filter", required = false) String filter,
			@RequestParam(name = "$top", required = false) Integer top,
			@RequestParam(name = "$skip", required = false) Integer skip);

	@GetExchange("/ODataFeed/odata/{table}/TypedDataSet")
	Rows typedDataSet(@PathVariable String table, @RequestParam(name = "$filter", required = false) String filter,
			@RequestParam(name = "$top", required = false) Integer top,
			@RequestParam(name = "$skip", required = false) Integer skip);

	/**
	 * An OData answer: the rows are in <code>value</code>, each one a JSON object. The
	 * rows stay maps, so a GraphQL field reads the property of the same name, and every
	 * name and value a client sees is the one CBS sent.
	 *
	 * @param value the rows, as CBS sent them
	 */
	record Rows(List<Map<String, Object>> value) {

	}

	/**
	 * Registers the interface as an HTTP service in the group <code>cbs</code>, whose
	 * base URL and timeouts come from <code>spring.http.serviceclient.cbs.*</code>.
	 */
	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(group = "cbs", types = CbsApi.class)
	class Registration {

	}

}
