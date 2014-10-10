package xyz.rjs.brandwatch.supermarkets.logistics.plugins.good;

/**
 * Represents an order that is yet to be completed.
 * 
 * @author matthew
 */
class PendingOrder {
	private final int volume;
	private final int tick;

	/**
	 * @param volume
	 * @param tick
	 */
	public PendingOrder(int volume, int tick) {
		this.volume = volume;
		this.tick = tick;
	}

	
	/**
	 * @return the volume
	 */
	public int getVolume() {
		return volume;
	}

	
	/**
	 * @return the tick
	 */
	public int getTick() {
		return tick;
	}
}