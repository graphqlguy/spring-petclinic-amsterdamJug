package org.springframework.samples.petclinic.eufeedadditives;

import java.util.Map;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.samples.petclinic.upstream.RestUpstreams;
import org.springframework.stereotype.Controller;

/**
 * Answers the <code>euFeedAdditives*</code> queries of the GraphQL schema from the EU
 * register of feed additives. Every query is one request: the arguments go out under
 * their own names, and the answer goes back as the Commission sent it. The types in
 * <code>graphql/eu-feed-additives.graphqls</code> name their fields after the fields of
 * the API, so each field reads the field of the same name.
 */
@Controller
class EuFeedAdditivesGraphQLController {

	static final String REGISTER = "The EU register of feed additives";

	private final EuFeedAdditivesApi register;

	EuFeedAdditivesGraphQLController(EuFeedAdditivesApi register) {
		this.register = register;
	}

	@QueryMapping
	Map<String, Object> euFeedAdditivesList(@Argument("additive_code") String additiveCode, @Argument String after) {
		return RestUpstreams.call(REGISTER, () -> this.register.list(additiveCode, after));
	}

	@QueryMapping
	Map<String, Object> euFeedAdditivesDetails(@Argument("additive_code") String additiveCode,
			@Argument("policy_item_code") String policyItemCode, @Argument("policy_item_id") Integer policyItemId,
			@Argument String after) {
		return RestUpstreams.call(REGISTER,
				() -> this.register.details(additiveCode, policyItemCode, policyItemId, after));
	}

	@QueryMapping
	Map<String, Object> euFeedAdditivesAuthorizations(@Argument("to_be_phased_out") String toBePhasedOut,
			@Argument("cat_funct_grp") String catFunctGrp, @Argument String after) {
		return RestUpstreams.call(REGISTER, () -> this.register.authorizations(toBePhasedOut, catFunctGrp, after));
	}

}
