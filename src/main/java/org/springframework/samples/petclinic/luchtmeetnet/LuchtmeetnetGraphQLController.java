package org.springframework.samples.petclinic.luchtmeetnet;

import java.util.Map;
import java.util.function.Supplier;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.samples.petclinic.upstream.InvalidArgumentException;
import org.springframework.samples.petclinic.upstream.UpstreamException;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * Answers the <code>luchtmeetnet*</code> queries of the GraphQL schema from the open API
 * of Luchtmeetnet. Every query is one request: the arguments go out under their own
 * names, and the answer goes back as Luchtmeetnet sent it. The types in
 * <code>graphql/luchtmeetnet.graphqls</code> name their fields after the attributes of
 * Luchtmeetnet, so each field reads the attribute of the same name.
 */
@Controller
class LuchtmeetnetGraphQLController {

	static final String LUCHTMEETNET = "The open API of Luchtmeetnet";

	private final LuchtmeetnetApi luchtmeetnet;

	LuchtmeetnetGraphQLController(LuchtmeetnetApi luchtmeetnet) {
		this.luchtmeetnet = luchtmeetnet;
	}

	@QueryMapping
	Map<String, Object> luchtmeetnetStations(@Argument Integer page, @Argument("order_by") String orderBy,
			@Argument("organisation_id") Integer organisationId) {
		return call(() -> this.luchtmeetnet.stations(page, orderBy, organisationId));
	}

	@QueryMapping
	Map<String, Object> luchtmeetnetStation(@Argument String number) {
		return call(() -> this.luchtmeetnet.station(number));
	}

	@QueryMapping
	Map<String, Object> luchtmeetnetStationMeasurements(@Argument String number, @Argument Integer page,
			@Argument String order, @Argument("order_direction") String orderDirection, @Argument String formula) {
		return call(() -> this.luchtmeetnet.stationMeasurements(number, page, order, orderDirection, formula));
	}

	@QueryMapping
	Map<String, Object> luchtmeetnetMeasurements(@Argument("station_number") String stationNumber,
			@Argument String formula, @Argument Integer page, @Argument("order_by") String orderBy,
			@Argument("order_direction") String orderDirection, @Argument String start, @Argument String end) {
		return call(() -> this.luchtmeetnet.measurements(stationNumber, formula, page, orderBy, orderDirection, start,
				end));
	}

	@QueryMapping
	Map<String, Object> luchtmeetnetLki(@Argument("station_number") String stationNumber, @Argument Integer page,
			@Argument("order_by") String orderBy, @Argument("order_direction") String orderDirection,
			@Argument String start, @Argument String end) {
		return call(() -> this.luchtmeetnet.lki(stationNumber, page, orderBy, orderDirection, start, end));
	}

	@QueryMapping
	Map<String, Object> luchtmeetnetConcentrations(@Argument String formula, @Argument double latitude,
			@Argument double longitude) {
		return call(() -> this.luchtmeetnet.concentrations(formula, latitude, longitude));
	}

	@QueryMapping
	Map<String, Object> luchtmeetnetComponents(@Argument Integer page, @Argument("order_by") String orderBy) {
		return call(() -> this.luchtmeetnet.components(page, orderBy));
	}

	@QueryMapping
	Map<String, Object> luchtmeetnetComponent(@Argument String formula) {
		return call(() -> this.luchtmeetnet.component(formula));
	}

	@QueryMapping
	Map<String, Object> luchtmeetnetOrganisations(@Argument Integer page) {
		return call(() -> this.luchtmeetnet.organisations(page));
	}

	/**
	 * Makes the call. <code>404 Not Found</code> becomes null. Luchtmeetnet answers
	 * <code>400 Bad Request</code> with the reason as JSON, for example a formula it does
	 * not know, and the client gets that reason so it can correct the query.
	 */
	private static Map<String, Object> call(Supplier<Map<String, Object>> call) {
		try {
			return call.get();
		}
		catch (HttpClientErrorException.NotFound ex) {
			return null;
		}
		catch (HttpClientErrorException.BadRequest ex) {
			throw new InvalidArgumentException(
					"%s rejected the arguments: %s".formatted(LUCHTMEETNET, ex.getResponseBodyAsString().strip()));
		}
		catch (RestClientException ex) {
			throw new UpstreamException(LUCHTMEETNET, ex);
		}
	}

}
