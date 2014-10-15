package xyz.rjs.brandwatch.supermarkets.logistics.plugins.printers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import xyz.rjs.brandwatch.supermarkets.logistics.plugins.AbstractPlugin;
import xyz.rjs.brandwatch.supermarkets.logistics.plugins.good.DeliveryStats;
import xyz.rjs.brandwatch.supermarkets.logistics.plugins.good.GoodPlugin;
import xyz.rjs.brandwatch.supermarkets.logistics.plugins.good.PurchaseStats;
import xyz.rjs.brandwatch.supermarkets.model.events.ClockTick;
import xyz.rjs.brandwatch.supermarkets.sim.Shop;
import xyz.rjs.brandwatch.supermarkets.sim.Warehouse;

import com.google.common.eventbus.Subscribe;

@Component
public class StatsPrinter extends AbstractPlugin {

	@Autowired
	private Warehouse warehouse;

	@Autowired
	private Shop shop;

	/**
	 * This tracks every completed delivery and calculates duration confidence.
	 */
	@Autowired
	private DeliveryStats deliveries;

	/**
	 * This tracks every proposed sale and calculates sale volume confidence.
	 */
	@Autowired
	private PurchaseStats purchases;

	@Autowired
	private GoodPlugin plugin;

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private volatile int totalEventCount = 0;

	@Subscribe
	public void eventListener(Object o) {
		totalEventCount++;
	}

	@Subscribe
	public void tickListener(ClockTick tick) {
		if (tick.getTick() % 10 == 0) {
			printStats();
		}
	}

	private void printStats() {
		logger.info("Total Events: {}", totalEventCount);

		logger.info(String.format(
			"Overall Statistics:\n" +
			"Delivery Time (%.2f)\n" +
			"Purchase per Tick (%.2f)\n" +
			"Shop Stock (%d)\n" +
			"Warehouse Stock (%d)\n" +
			"Total Stock (%d)\n" +
			"Desired Stock (%d)",
			deliveries.confidence(),
			purchases.overallConfidence(),
			shop.getStock(),
			warehouse.getStock(),
			plugin.getTotalStock(),
			plugin.getRequiredStock()));

	}
}
