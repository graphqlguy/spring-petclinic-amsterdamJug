package org.springframework.samples.petclinic.rasff;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A member of the RASFF network, which can send notifications: an EU country, a country
 * of the European Economic Area, Switzerland, or the European Commission.
 *
 * @param id the id, which the filters of a search take
 * @param name the English name
 * @param code the ISO 3166 alpha-2 code, or EU for the European Commission
 */
record RasffMember(long id, @JsonProperty("description") String name, @JsonProperty("alpha2Code") String code) {

}
