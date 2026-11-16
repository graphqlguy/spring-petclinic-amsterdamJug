package org.springframework.samples.petclinic.rasff;

/**
 * A term of one of the lists that classify RASFF notifications, such as the product
 * category "pet food" or the risk decision "serious".
 *
 * @param id the id, which the filters of a search take
 * @param description the term in English
 */
record RasffTerm(long id, String description) {

}
