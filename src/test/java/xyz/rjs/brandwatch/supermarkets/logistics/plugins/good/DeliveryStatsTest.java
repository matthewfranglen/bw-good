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

import xyz.rjs.brandwatch.supermarkets.model.events.ArrivalNotification;
import xyz.rjs.brandwatch.supermarkets.model.events.Order;
import xyz.rjs.brandwatch.supermarkets.sim.DeliverablePlace;
import xyz.rjs.brandwatch.supermarkets.sim.Warehouse;


/**
 * @author matthew
 *
 */
public class DeliveryStatsTest {

	@InjectMocks
	private DeliveryStats stats;

	@Mock
	private Warehouse warehouse;

	@Mock
	private TickTracker tracker;

	@Before
	public void setUp() {
		stats = new DeliveryStats();
		warehouse = mock(Warehouse.class);
		tracker = mock(TickTracker.class);

		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testDeliveryStats() {
		assertTrue("DeliveryStats starts empty", stats.isEmpty());

		Order order = new Order();
		order.setVolume(10);
		order.setWarehouse(warehouse);
		when(tracker.getTick()).thenReturn(0);
		stats.orderListener(order);

		assertTrue("DeliveryStats remains empty", stats.isEmpty());
		assertEquals("DeliveryStats pending stock correct", 10, stats.pendingStock(), 0.1);

		when(tracker.getTick()).thenReturn(10);
		stats.arrivalListener(new ArrivalNotification(new DeliverablePlace(null) {}, 10));

		assertTrue("DeliveryStats remains empty", stats.isEmpty());
		assertEquals("DeliveryStats pending stock correct", 10, stats.pendingStock(), 0.1);

		when(tracker.getTick()).thenReturn(20);
		stats.arrivalListener(new ArrivalNotification(warehouse, 5));

		assertTrue("DeliveryStats remains empty", stats.isEmpty());
		assertEquals("DeliveryStats pending stock correct", 10, stats.pendingStock(), 0.1);

		when(tracker.getTick()).thenReturn(30);
		stats.arrivalListener(new ArrivalNotification(warehouse, 10));

		assertTrue("DeliveryStats no longer empty", !stats.isEmpty());
		assertEquals("DeliveryStats confidence correct", 30, stats.confidence(), 0.1);
	}
}
