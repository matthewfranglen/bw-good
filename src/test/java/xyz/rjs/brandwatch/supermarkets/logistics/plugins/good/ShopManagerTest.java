package xyz.rjs.brandwatch.supermarkets.logistics.plugins.good;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import xyz.rjs.brandwatch.supermarkets.model.events.ClockTick;
import xyz.rjs.brandwatch.supermarkets.sim.Shop;
import xyz.rjs.brandwatch.supermarkets.sim.Warehouse;

/**
 * @author matthew
 *
 */
public class ShopManagerTest {

	@InjectMocks
	private ShopManager manager;

	@Mock
	private Warehouse warehouse;

	@Mock
	private Shop shop;

	@Mock
	private PurchaseStats purchases;

	@Before
	public void setUp() {
		manager = new ShopManager();
		warehouse = mock(Warehouse.class);
		shop = mock(Shop.class);
		purchases = mock(PurchaseStats.class);

		MockitoAnnotations.initMocks(this);
	}

	/**
	 * Test when shop is fully stocked for required desire
	 */
	@Test
	public void testShopManagerStocked() {
		when(warehouse.getStock()).thenReturn(0);
		when(shop.getStock()).thenReturn(10);
		when(purchases.confidence()).thenReturn(5.0);

		manager.tickListener(new ClockTick(0));

		verify(shop, never()).addStock(anyInt());
		verify(warehouse, never()).setStock(anyInt());
	}

	/**
	 * Test when shop is not stocked and warehouse is stocked for required desire
	 */
	@Test
	public void testShopManagerUnstocked() {
		when(warehouse.getStock()).thenReturn(20);
		when(shop.getStock()).thenReturn(0);
		when(purchases.confidence()).thenReturn(5.0);

		manager.tickListener(new ClockTick(0));

		verify(shop, times(1)).addStock(5);
		verify(warehouse, times(1)).setStock(15);
	}

	/**
	 * Test when shop and warehouse are not stocked for required desire
	 */
	@Test
	public void testShopManagerFullyUnstocked() {
		when(warehouse.getStock()).thenReturn(0);
		when(shop.getStock()).thenReturn(0);
		when(purchases.confidence()).thenReturn(5.0);

		manager.tickListener(new ClockTick(0));

		verify(shop, never()).addStock(anyInt());
		verify(warehouse, never()).setStock(anyInt());
	}

	/**
	 * Test when shop and warehouse are partially stocked for required desire
	 */
	@Test
	public void testShopManagerPartiallyStocked() {
		when(warehouse.getStock()).thenReturn(1);
		when(shop.getStock()).thenReturn(2);
		when(purchases.confidence()).thenReturn(5.0);

		manager.tickListener(new ClockTick(0));

		verify(shop, times(1)).addStock(1);
		verify(warehouse, times(1)).setStock(0);
	}
}
