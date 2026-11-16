package org.springframework.samples.petclinic.rasff;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A country that a notification can name as the origin of a product or as a country the
 * product was distributed to.
 *
 * @param id the id, which the filters of a search take
 * @param name the English short name
 * @param code the ISO 3166 alpha-2 code
 */
record RasffCountry(long id, @JsonProperty("englishShortName") String name, @JsonProperty("alpha2Code") String code) {

}
