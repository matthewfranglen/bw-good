package xyz.rjs.brandwatch.supermarkets.logistics.plugins.good;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


/**
 * @author matthew
 *
 */
public class StatsTest {

	@Test
	public void testStats() {
		Stats stats = new Stats();

		stats.add(5);
		stats.add(10);
		stats.add(15);
		assertEquals("Average calculation", stats.average(3), 10.0, 0.1);

		// standard deviation (m = average, N = element, l = length):
		// sqrt( ( sum(square(N - m)) / l )
		double standardDeviation = Math.sqrt(((5.0 * 5.0) + (5.0 * 5.0)) / 3.0);
		assertEquals("Standard deviation calculation", standardDeviation, stats.standardDeviation(3), 0.1);
		assertEquals("Two standard deviation confidence calculation", 10 + (standardDeviation * 2), stats.confidence(3), 0.1);
	}
}
