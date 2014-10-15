package xyz.rjs.brandwatch.supermarkets.logistics.plugins.good;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import xyz.rjs.brandwatch.supermarkets.model.events.Customer;

/**
 * @author matthew
 *
 */
public class PurchaseStatsTest {

	@InjectMocks
	private PurchaseStats stats;

	@Mock
	private TickTracker tracker;

	@Before
	public void setUp() {
		stats = new PurchaseStats();
		tracker = mock(TickTracker.class);

		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testPurchaseStats() {
		assertTrue("PurchaseStats starts empty", stats.isEmpty());

		Customer customer = new Customer();
		customer.setName("Robert Paulson");
		customer.setStuffNeeded(9);
		stats.customerListener(customer);

		assertTrue("PurchaseStats no longer empty", !stats.isEmpty());
		assertEquals("PurchaseStats confidence correct", 9, stats.confidence(), 0.1);

		when(tracker.getTick()).thenReturn(10);
		assertEquals("PurchaseStats confidence correct", 6.3, stats.overallConfidence(), 0.1);
	}
}
