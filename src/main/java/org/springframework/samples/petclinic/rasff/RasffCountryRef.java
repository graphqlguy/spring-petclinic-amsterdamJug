package org.springframework.samples.petclinic.rasff;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A country as a notification names it, without its id.
 *
 * @param name the English name
 * @param code the ISO 3166 alpha-2 code
 */
record RasffCountryRef(@JsonProperty("organizationName") String name, @JsonProperty("isoCode") String code) {

}
