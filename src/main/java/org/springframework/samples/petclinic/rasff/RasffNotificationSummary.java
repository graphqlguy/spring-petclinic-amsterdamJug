package org.springframework.samples.petclinic.rasff;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A notification as a RASFF search lists it, as the <code>RasffNotificationSummary</code>
 * type of the GraphQL schema. The Jackson annotations map RASFF's names to the names of
 * the schema.
 *
 * @param id the id, which the notification query takes
 * @param reference the public reference, for example 2026.8625
 * @param subject what the notification is about
 * @param validationDate the day the European Commission validated the notification
 * @param notifyingCountry the member of the network that sent it
 * @param productCategory the category of the product
 * @param productType food, feed or food contact material
 * @param classification the kind of notification, for example an alert
 * @param riskDecision how serious the risk is
 * @param originCountries the countries the product came from
 */
record RasffNotificationSummary(@JsonProperty("notifId") long id, String reference, String subject,
		@JsonProperty("ecValidationDate") @JsonFormat(pattern = RasffNotification.TIMESTAMP) LocalDate validationDate,
		RasffCountryRef notifyingCountry, RasffTerm productCategory, RasffTerm productType,
		@JsonProperty("notificationClassification") RasffTerm classification, RasffTerm riskDecision,
		List<RasffCountryRef> originCountries) {

	RasffNotificationSummary {
		// subjects arrive with trailing spaces
		subject = (subject != null) ? subject.strip() : null;
		originCountries = (originCountries != null) ? originCountries : List.of();
	}

	/**
	 * One page of notifications, as the <code>RasffNotificationPage</code> type.
	 */
	record Page(List<RasffNotificationSummary> notifications, int page, int totalPages, int totalNotifications) {

	}

}
