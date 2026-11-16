package org.springframework.samples.petclinic.upstream;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpstreamClockTests {

	@Test
	void mockClockIsPinned() {
		UpstreamClock clock = new UpstreamClock("2026-11-15T17:00:00Z");
		assertThat(clock.clock().instant()).isEqualTo(Instant.parse("2026-11-15T17:00:00Z"));
	}

	@Test
	void blankMeansTheSystemClock() {
		Instant before = Instant.now();
		Instant now = new UpstreamClock("").clock().instant();
		assertThat(now).isBetween(before, Instant.now());
	}

	@Test
	void todayIsTheDayInAmsterdam() {
		// 22:30 UTC on 15 November is still 15 November in Amsterdam, which is on UTC+1
		// in winter
		assertThat(new UpstreamClock("2026-11-15T22:30:00Z").today()).hasToString("2026-11-15");
		// 22:30 UTC on 15 June is already 16 June in Amsterdam, which is on UTC+2 in
		// summer
		assertThat(new UpstreamClock("2026-06-15T22:30:00Z").today()).hasToString("2026-06-16");
	}

}
