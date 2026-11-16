package org.springframework.samples.petclinic.upstream;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The time the application works with: the system clock, or a fixed moment when
 * <code>petclinic.now</code> is set. The <code>mock</code> profile sets it to the moment
 * the answers were recorded, so a question about tomorrow or right now asks the recorded
 * answers on any later day.
 */
@Component
public class UpstreamClock {

	/**
	 * The clinic is in Amsterdam.
	 */
	public static final ZoneId AMSTERDAM = ZoneId.of("Europe/Amsterdam");

	private final Clock clock;

	public UpstreamClock(@Value("${petclinic.now:}") String now) {
		this.clock = now.isBlank() ? Clock.system(AMSTERDAM) : Clock.fixed(Instant.parse(now), AMSTERDAM);
	}

	/**
	 * The clock, in the time zone of Amsterdam.
	 */
	public Clock clock() {
		return this.clock;
	}

	/**
	 * Today in Amsterdam.
	 */
	public LocalDate today() {
		return LocalDate.now(this.clock);
	}

}
