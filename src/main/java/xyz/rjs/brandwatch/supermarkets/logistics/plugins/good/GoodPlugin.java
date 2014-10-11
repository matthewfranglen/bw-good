package xyz.rjs.brandwatch.supermarkets.logistics.plugins.good;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import xyz.rjs.brandwatch.supermarkets.logistics.plugins.AbstractPlugin;
import xyz.rjs.brandwatch.supermarkets.model.events.ArrivalNotification;
import xyz.rjs.brandwatch.supermarkets.model.events.ClockTick;
import xyz.rjs.brandwatch.supermarkets.model.events.Customer;
import xyz.rjs.brandwatch.supermarkets.model.events.Order;
import xyz.rjs.brandwatch.supermarkets.model.events.PriceList;
import xyz.rjs.brandwatch.supermarkets.sim.Shop;
import xyz.rjs.brandwatch.supermarkets.sim.Warehouse;

import com.google.common.eventbus.Subscribe;

/**
 * The good plugin tracks delivery times and purchases to cover spikes in demand
 * to two standard deviations (approximately 95% coverage).
 * 
 * This plugin has two states.
 * 
 * The first time it is started and ready it will spend the entire balance on
 * stock. This is prudent because the price cannot get lower than it is at the
 * start (1/unit) and it buys time to calculate delivery times. The purchase is
 * made over several transactions to allow an accurate delivery time estimate to
 * be generated.
 * 
 * Once all of the money has been spent the plugin switches to the second state.
 * Here it uses the tracked delivery times and any sale data to calculate the
 * amount that must be held. The delivery time is estimated as the time that
 * covers 95% of deliveries. The unit sale rate is also estimated as the rate
 * that covers 95%. When combined this should lead to near total coverage, as it
 * would take constant high sales for an entire delivery period to drain the
 * stock.
 * 
 * This plugin always assumes that the current price is the average price for
 * the forseeable future. It will check that the price permits a profit, but
 * otherwise will purchase at any price.
 * 
 * The plugin will ensure that the shop always has enough stock to cover a
 * single tick of demand.
 * 
 * In general this is a very conservative approach to take.
 * 
 * @author matthew
 */
@Component
public class GoodPlugin extends AbstractPlugin {

	private static final Logger logger = LoggerFactory.getLogger(GoodPlugin.class);

	/**
	 * These are the trades that are issued by the GATHER_DATA state. The total
	 * of 78 is slightly under the lowest starting balance of 85.
	 */
	private static final int[] STARTING_TRADES = new int[] { 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 };
	/**
	 * This is the buy price at which it becomes unprofitable to buy any stock.
	 */
	private static final int PRICE_LIMIT = 10;

	@Autowired
	private Warehouse warehouse;

	@Autowired
	private Shop shop;

	/**
	 * The current state of the plugin.
	 */
	private STATE state;

	/**
	 * This is the list of outstanding orders.
	 */
	private final Collection<PendingOrder> orders;
	/**
	 * This is the current index in the STARTING_TRADES array.
	 */
	private int startingTradesIndex;
	/**
	 * This is the current per unit buy price.
	 */
	private int price;
	/**
	 * This is the last seen tick. This is used to calculate delivery times.
	 */
	private int tick;

	/**
	 * This tracks every completed delivery and calculates duration confidence.
	 */
	private final Stats deliveries;
	/**
	 * This tracks every proposed sale and calculates sale volume confidence.
	 */
	private final Stats purchases;

	public GoodPlugin() {
		orders = new ArrayList<PendingOrder>();
		state = STATE.START;
		startingTradesIndex = 0;
		deliveries = new Stats();
		purchases = new Stats();
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

			deliveries.add(tick - o.getTick());
			orders.remove(o);
		}
	}

	@Subscribe
	public void customerListener(Customer customer) {
		purchases.add(customer.getStuffNeeded());
	}

	@Subscribe
	public void priceListener(PriceList priceList) {
		price = priceList.getCurrentPrice();
		state.priceListener(this, priceList);
	}

	@Subscribe
	public void orderListener(Order order) {
		orders.add(new PendingOrder(order.getVolume(), tick));
	}

	@Subscribe
	public void tickListener(ClockTick tick) {
		this.tick = tick.getTick(); // saved so that arrivals can be tracked.
		state.tickListener(this, tick);
		stockShop();

		logger.info(String.format(
				"\nTICK: Delivery Time (%.2f) Purchase per Tick (%.2f) Shop Stock (%d) Warehouse Stock (%d) Total Stock (%d) Required Stock (%d)",
				deliveries.confidence(), purchases.confidence(this.tick), shop.getStock(), warehouse.getStock(), getTotalStock(), getRequiredStock()));
	}

	/**
	 * This ensures that the shop has at least enough stock for a single tick of
	 * sales.
	 */
	private void stockShop() {
		// The calculation of p here does *not* use the tick count because this
		// is a calculation that only considers ticks where there was a sale.
		int p = (int) Math.ceil(purchases.confidence());

		if (shop.getStock() < p && warehouse.getStock() > 0) {
			int volume = Math.min(p - shop.getStock(), warehouse.getStock());
			shop.addStock(volume);
			warehouse.setStock(warehouse.getStock() - volume);
		}
	}

	/**
	 * Creates and tracks the order.
	 * 
	 * @param volume
	 */
	private void placeOrder(int volume) {
		Order order = new Order();
		order.setWarehouse(warehouse);
		order.setVolume(volume);
		eventBus.post(order);
	}

	/**
	 * Get the current warehouse stock and all outstanding deliveries.
	 * 
	 * @return
	 */
	private int getTotalStock() {
		return shop.getStock() + warehouse.getStock() + orders.stream().mapToInt(order -> order.getVolume()).sum();
	}

	/**
	 * Get the required stock to survive for the length of an average delivery.
	 * 
	 * @return
	 */
	private int getRequiredStock() {
		return (int) Math.ceil((deliveries.confidence() * purchases.confidence(tick)));
	}

	/**
	 * Transition the plugin through the different GoodPlugin.STATE states.
	 * 
	 * @param state
	 */
	private void setState(STATE state) {
		logger.info(String.format("STATE TRANSITION: %s to %s", this.state, state));

		this.state = state;
	}

	/**
	 * Provides the behaviours associated with the different states the
	 * GoodPlugin can be in.
	 * 
	 * @author matthew
	 */
	private enum STATE {
		/**
		 * The START state waits for the price to become available. Once the
		 * price is available trading can begin.
		 */
		START() {

			@Override
			void priceListener(GoodPlugin plugin, PriceList priceList) {
				plugin.setState(GATHER_DATA);
			}
		},
		/**
		 * The GATHER_DATA state issues a set of initial trades to seed the
		 */
		GATHER_DATA() {

			@Override
			void tickListener(GoodPlugin plugin, ClockTick tick) {
				if (plugin.startingTradesIndex < STARTING_TRADES.length && plugin.price < PRICE_LIMIT) {
					plugin.placeOrder(STARTING_TRADES[plugin.startingTradesIndex]);
					plugin.startingTradesIndex++;
				}
				if (!(plugin.deliveries.isEmpty() || plugin.purchases.isEmpty())) {
					plugin.setState(plugin.price >= PRICE_LIMIT ? OVERPRICED : TRADE);
				}
			}
		},
		TRADE() {

			@Override
			void tickListener(GoodPlugin plugin, ClockTick tick) {
				int requiredStock = plugin.getRequiredStock() - plugin.getTotalStock();

				if (requiredStock > 0) {
					plugin.placeOrder(requiredStock);
				}
			}

			@Override
			void priceListener(GoodPlugin plugin, PriceList priceList) {
				if (priceList.getCurrentPrice() >= PRICE_LIMIT) {
					plugin.setState(OVERPRICED);
				}
			}
		},
		OVERPRICED() {

			@Override
			void priceListener(GoodPlugin plugin, PriceList priceList) {
				if (priceList.getCurrentPrice() < PRICE_LIMIT) {
					plugin.setState(TRADE);
				}
			}

		};

		void tickListener(GoodPlugin plugin, ClockTick tick) {
		}

		void priceListener(GoodPlugin plugin, PriceList priceList) {
		}
	}
}
