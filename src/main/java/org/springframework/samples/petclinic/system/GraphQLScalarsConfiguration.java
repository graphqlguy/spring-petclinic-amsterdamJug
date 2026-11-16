package org.springframework.samples.petclinic.system;

import graphql.scalars.ExtendedScalars;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

@Configuration(proxyBeanMethods = false)
class GraphQLScalarsConfiguration {

	@Bean
	RuntimeWiringConfigurer scalars() {
		// GBIF counts more than 3 billion occurrences, past the 2,147,483,647 of Int
		return (wiring) -> wiring.scalar(ExtendedScalars.Date).scalar(ExtendedScalars.GraphQLLong);
	}

}
