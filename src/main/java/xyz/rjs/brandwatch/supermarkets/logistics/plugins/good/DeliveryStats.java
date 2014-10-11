package xyz.rjs.brandwatch.supermarkets.logistics.plugins.good;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import xyz.rjs.brandwatch.supermarkets.logistics.plugins.AbstractPlugin;
import xyz.rjs.brandwatch.supermarkets.model.events.ArrivalNotification;
import xyz.rjs.brandwatch.supermarkets.model.events.Order;
import xyz.rjs.brandwatch.supermarkets.sim.Warehouse;

import com.google.common.eventbus.Subscribe;


/**
 * @author matthew
 *
 */
@Component
public class DeliveryStats extends AbstractPlugin {

	@Autowired
	private Warehouse warehouse;

	@Autowired
	private TickTracker tick;

	/**
	 * This is the list of outstanding orders.
	 */
	private final Collection<PendingOrder> orders;

	/**
	 * This tracks every completed delivery and calculates duration confidence.
	 */
	private final Stats deliveries;

	public DeliveryStats() {
		orders = new ArrayList<PendingOrder>();
		deliveries = new Stats();
	}

	@Subscribe
	public void arrivalListener(ArrivalNotification arrival) {
		// Only arrivals at the Warehouse need to be tracked.
		// Warehouse to Shop is instant (see WarehouseManagementSystem).
		if (arrival.getPlace() != warehouse) {
			return;
		}

		final int amount = arrival.getAmount();
		Optional<PendingOrder> order = orders.stream().filter(o -> o.getVolume() == amount).findFirst();

		if (order.isPresent()) {
			PendingOrder o = order.get();

			deliveries.add(tick.getTick() - o.getTick());
			orders.remove(o);
		}
	}

	@Subscribe
	public void orderListener(Order order) {
		orders.add(new PendingOrder(order.getVolume(), tick.getTick()));
	}

	public boolean isEmpty() {
		return deliveries.isEmpty();
	}

	/**
	 * Indicates the amount of stock yet to be delivered.
	 * @return
	 */
	public int pendingStock() {
		return orders.stream().mapToInt(order -> order.getVolume()).sum();
	}

	/**
	 * Indicates the confidence levels across all deliveries.
	 * This is the number that will cover 95% of deliveries.
	 * @return
	 */
	public double confidence() {
		return deliveries.confidence();
	}
}
