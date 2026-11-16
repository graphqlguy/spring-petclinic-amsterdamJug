package org.springframework.samples.petclinic.cbs;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.samples.petclinic.cbs.CbsApi.Rows;
import org.springframework.samples.petclinic.upstream.UpstreamException;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * Answers the <code>cbs*</code> queries of the GraphQL schema from the open data of
 * Statistics Netherlands. Every query is one request to CBS: the arguments go out as they
 * came in, and the rows go back as CBS sent them. The types in
 * <code>graphql/cbs.graphqls</code> name their fields after the properties of CBS, so
 * each field reads the property of the same name.
 */
@Controller
class CbsGraphQLController {

	static final String CBS = "The open data of Statistics Netherlands (CBS)";

	private final CbsApi cbs;

	CbsGraphQLController(CbsApi cbs) {
		this.cbs = cbs;
	}

	@QueryMapping
	List<Map<String, Object>> cbsTables(@Argument String filter, @Argument Integer top, @Argument Integer skip) {
		return rows(() -> this.cbs.tables(filter, top, skip));
	}

	@QueryMapping
	List<Map<String, Object>> cbsTableInfos(@Argument String table) {
		return rows(() -> this.cbs.tableInfos(table));
	}

	@QueryMapping
	List<Map<String, Object>> cbsDataProperties(@Argument String table) {
		return rows(() -> this.cbs.dataProperties(table));
	}

	@QueryMapping
	List<Map<String, Object>> cbsCategoryGroups(@Argument String table) {
		return rows(() -> this.cbs.categoryGroups(table));
	}

	@QueryMapping
	List<Map<String, Object>> cbsPeriods(@Argument String table, @Argument String filter, @Argument Integer top,
			@Argument Integer skip) {
		return rows(() -> this.cbs.periods(table, filter, top, skip));
	}

	@QueryMapping
	List<Map<String, Object>> cbs86144ENGExpenditureCategories(@Argument String filter, @Argument Integer top,
			@Argument Integer skip) {
		return rows(() -> this.cbs.categories("86144ENG", "ExpenditureCategories", filter, top, skip));
	}

	@QueryMapping
	List<Map<String, Object>> cbs86144ENGTypedDataSet(@Argument String filter, @Argument Integer top,
			@Argument Integer skip) {
		return rows(() -> this.cbs.typedDataSet("86144ENG", filter, top, skip));
	}

	@QueryMapping
	List<Map<String, Object>> cbs86141ENGExpenditureCategories(@Argument String filter, @Argument Integer top,
			@Argument Integer skip) {
		return rows(() -> this.cbs.categories("86141ENG", "ExpenditureCategories", filter, top, skip));
	}

	@QueryMapping
	List<Map<String, Object>> cbs86141ENGTypedDataSet(@Argument String filter, @Argument Integer top,
			@Argument Integer skip) {
		return rows(() -> this.cbs.typedDataSet("86141ENG", filter, top, skip));
	}

	@QueryMapping
	List<Map<String, Object>> cbs84952ENGFarmAnimals(@Argument String filter, @Argument Integer top,
			@Argument Integer skip) {
		return rows(() -> this.cbs.categories("84952ENG", "FarmAnimals", filter, top, skip));
	}

	@QueryMapping
	List<Map<String, Object>> cbs84952ENGTypedDataSet(@Argument String filter, @Argument Integer top,
			@Argument Integer skip) {
		return rows(() -> this.cbs.typedDataSet("84952ENG", filter, top, skip));
	}

	/**
	 * Makes the call and returns the rows of the answer. CBS answers
	 * <code>404 Not Found</code> for a table it does not have, which becomes null.
	 */
	private static List<Map<String, Object>> rows(Supplier<Rows> call) {
		try {
			return call.get().value();
		}
		catch (HttpClientErrorException.NotFound ex) {
			return null;
		}
		catch (RestClientException ex) {
			throw new UpstreamException(CBS, ex);
		}
	}

}
