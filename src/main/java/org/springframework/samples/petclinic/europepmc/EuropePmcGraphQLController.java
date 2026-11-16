package org.springframework.samples.petclinic.europepmc;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * Answers the <code>europePmc*</code> queries of the GraphQL schema from the SOAP web
 * service of Europe PMC. Every query is one SOAP request: the field names its operation,
 * the arguments go out as the elements of the request under their own names, in the order
 * of the WSDL, and the answer goes back with the element names of the response.
 */
@Controller
class EuropePmcGraphQLController {

	private final EuropePmcSoapClient europePmc;

	EuropePmcGraphQLController(EuropePmcSoapClient europePmc) {
		this.europePmc = europePmc;
	}

	@QueryMapping
	Map<String, Object> europePmcSearchPublications(@Argument String queryString, @Argument String resultType,
			@Argument String cursorMark, @Argument Integer pageSize, @Argument String sort, @Argument Boolean synonym) {
		Map<String, Object> arguments = new LinkedHashMap<>();
		arguments.put("queryString", queryString);
		arguments.put("resultType", resultType);
		arguments.put("cursorMark", cursorMark);
		arguments.put("pageSize", pageSize);
		arguments.put("sort", sort);
		arguments.put("synonym", synonym);
		return this.europePmc.call("searchPublications", arguments);
	}

	@QueryMapping
	Map<String, Object> europePmcGetCitations(@Argument String id, @Argument String source, @Argument Integer offSet,
			@Argument Integer pageSize) {
		return this.europePmc.call("getCitations", publication(id, source, offSet, pageSize));
	}

	@QueryMapping
	Map<String, Object> europePmcGetReferences(@Argument String id, @Argument String source, @Argument Integer offSet,
			@Argument Integer pageSize) {
		return this.europePmc.call("getReferences", publication(id, source, offSet, pageSize));
	}

	private static Map<String, Object> publication(String id, String source, Integer offSet, Integer pageSize) {
		Map<String, Object> arguments = new LinkedHashMap<>();
		arguments.put("id", id);
		arguments.put("source", source);
		arguments.put("offSet", offSet);
		arguments.put("pageSize", pageSize);
		return arguments;
	}

}
