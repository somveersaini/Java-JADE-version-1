/**
 * 
 * This class holds information that a client needs in order to
 * complete it's task i.e. find and buy some goods.
 *
 */
public class ClientRecord {
	
	private String goodName;
	private int xNeeded;
	private int maxPerOne;
	private String[] knownSuppliers;
	private int currentIndex;
	
	/**
	 * Public constructor.
	 * @param gName: String representing name of good in question.
	 * @param howManyNeeded: Integer representing the quantity required.
	 * @param canPayPerOne: Integer representing how much the client is willing to pay per one.
	 */
	public ClientRecord(String gName, int howManyNeeded, int canPayPerOne) {
		goodName = gName;
		xNeeded = howManyNeeded;
		maxPerOne = canPayPerOne;
		currentIndex = 0;
		knownSuppliers = new String[100];
	}
	/**
	 * Getter for name of good.
	 * @return: String name of this good.
	 */
	public String getName() {
		return goodName;
	}
	/**
	 * Getter for quantity required.
	 * @return: Integer representing quantity of good required.
	 */
	public int getQuantity() {
		return xNeeded;
	}
	/**
	 * Getter for price customer willing to pay per one item.
	 * @return: Integer representing how much customer is willing to pay per unit.
	 */
	public int getPricePerOne() {
		return maxPerOne;
	}
	/**
	 * Setter function to change xNeeded amount.
	 * @param qty: Integer representing how much is needed now.
	 */
	public void setQuantity(int qty) {
		xNeeded = qty;
	}
	/**
	 * Function returns the number of entries in the knownSuppliers list.
	 * @return: Integer representing the number of known suppliers.
	 */
	public int getKnownSuppliers() {
		return currentIndex;
	}
	/**
	 * Function to add String to the array of known suppliers. String is
	 * expected to be the name of supplier for this particular good.
	 * @param name: String representing the name of supplier for this
	 * good.
	 */
	public void addSupplier(String name) {
		knownSuppliers[currentIndex] = name;
		currentIndex++;
	}
	/**
	 * Function returns name of the supplier stored at a given index.
	 * @param index: Pointing to index of the suppliers array.
	 * @return String containing supplier name.
	 */
	public String getSupplierNameAt(int index) {
		//Index out of bounds?
		if(index < currentIndex) {
			//YES.
			return knownSuppliers[index];
		} else {
			//NO.
			return "";
		}
	}
	/**
	 * Function checks if the supplied string s is already in the array.
	 * @param s: Name of the supplier.
	 * @return TRUE if match found.
	 * 			FALSE otherwise.
	 */
	public boolean hasEntry(String s) {
		if(knownSuppliers.length <= 0) {
			return false;
		} else {
			for(int i = 0; i < currentIndex - 1; i++) {
				if(knownSuppliers[i].equals(s)) {
					return true;
				}
			}
			//Nothing was fond (no matches).
			return false;
		}
	}
}
