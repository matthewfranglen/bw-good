package xyz.rjs.brandwatch.supermarkets.logistics.plugins.good;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import xyz.rjs.brandwatch.supermarkets.logistics.plugins.AbstractPlugin;
import xyz.rjs.brandwatch.supermarkets.model.events.Customer;

import com.google.common.eventbus.Subscribe;


/**
 * @author matthew
 *
 */
@Component
public class PurchaseStats extends AbstractPlugin {

	@Autowired
	private TickTracker tick;

	/**
	 * This tracks every proposed sale and calculates sale volume confidence.
	 */
	private final Stats purchases;

	/**
	 * @param purchases
	 */
	public PurchaseStats() {
		purchases = new Stats();
	}

	@Subscribe
	public void customerListener(Customer customer) {
		purchases.add(customer.getStuffNeeded());
	}

	/**
	 * Indicates the confidence levels across all purchases that have been made.
	 * This is the number that will cover 95% of single purchases.
	 * @return
	 */
	public double confidence() {
		return purchases.confidence();
	}

	public boolean isEmpty() {
		return purchases.isEmpty();
	}

	/**
	 * Indicates the confidence levels for an average tick of the clock.
	 * This is the number that will cover 95% of purchases over a long time.
	 * @return
	 */
	public double overallConfidence() {
		return purchases.confidence(tick.getTick());
	}
}
