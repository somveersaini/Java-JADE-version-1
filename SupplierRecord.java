/**
 * This class acts as a record for suppliers.
 *
 */
public class SupplierRecord {
	private String goodName;
	private int qty;
	private int price;
	
	/**
	 * Public constructor.
	 * @param n: String name of this good.
	 * @param q: integer quantity.
	 * @param p: integer price per unit.
	 */
	public SupplierRecord(String n, int q, int p) {
		goodName = n;
		qty = q;
		price = p;
	}
	
	/**
	 * Getter for name of this good.
	 * @return: String.
	 */
	public String getName() {
		return goodName;
	}
	/**
	 * Getter for quantity.
	 * @return: integer.
	 */
	public int getQuantity() {
		return qty;
	}
	/**
	 * Getter for price.
	 * @return: integer.
	 */
	public int getPrice() {
		return price;
	}
	/**
	 * Setter for quantity.
	 * @param q: integer representing new quantity.
	 */
	public void setQty(int q) {
		qty = q;
	}
	/**
	 * Setter for price.
	 * @param p: integer representing new price.
	 */
	public void setPrice(int p) {
		price = p;
	}
}
