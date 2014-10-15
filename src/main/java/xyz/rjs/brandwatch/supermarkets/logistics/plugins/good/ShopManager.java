package xyz.rjs.brandwatch.supermarkets.logistics.plugins.good;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import xyz.rjs.brandwatch.supermarkets.logistics.plugins.AbstractPlugin;
import xyz.rjs.brandwatch.supermarkets.model.events.ClockTick;
import xyz.rjs.brandwatch.supermarkets.sim.Shop;
import xyz.rjs.brandwatch.supermarkets.sim.Warehouse;

import com.google.common.eventbus.Subscribe;


/**
 * Handles restocking the shop.
 * 
 * @author matthew
 */
@Component
public class ShopManager extends AbstractPlugin {

	@Autowired
	private Warehouse warehouse;

	@Autowired
	private Shop shop;

	/**
	 * This tracks every proposed sale and calculates sale volume confidence.
	 */
	@Autowired
	private PurchaseStats purchases;

	@Subscribe
	public void tickListener(ClockTick tick) {
		// The calculation of p here does *not* use the tick count because this
		// is a calculation that only considers ticks where there was a sale.
		int p = (int) Math.ceil(purchases.confidence());
		int shopStock = shop.getStock(), warehouseStock = warehouse.getStock();

		if (shopStock < p && warehouseStock > 0) {
			int volume = Math.min(p - shopStock, warehouseStock);
			shop.addStock(volume);
			warehouse.setStock(warehouseStock - volume);
		}
	}
}
