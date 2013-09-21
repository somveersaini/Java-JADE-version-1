
public class Merchant {
	private String name;
	private boolean nuts;
	private boolean bolts;
	private boolean widgets;
	private boolean doobries;
	private boolean washers;
	
	/**
	 * Public constructor.
	 * @param mName: String containing merchant name.
	 */
	public Merchant(String mName) {
		name = mName;
		nuts = false;
		bolts = false;
		widgets = false;
		doobries = false;
		washers = false;
	}
	
	/**
	 * Public getter for merchant name.
	 * @return: String - merchant name.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Public update method for some good that merchant trades in.
	 * @param what: String representing the good to add.
	 */
	public void updateMerchant(String what) {
		if(what.equals("nuts")) {
			nuts = true;
		} else if(what.equals("bolts")) {
			bolts = true;
		} else if(what.equals("widgets")) {
			widgets = true;
		} else if(what.equals("doobries")) {
			doobries = true;
		} else if(what.equals("washers")) {
			washers = true;
		}
	}
	
	/**
	 * Public remove method for some good that merchant trades in.
	 * @param what: String representing the good to de-list.
	 */
	public void delistItem(String what) {
		if(what.equals("nuts")) {
			nuts = false;
		} else if(what.equals("bolts")) {
			bolts = false;
		} else if(what.equals("widgets")) {
			widgets = false;
		} else if(what.equals("doobries")) {
			doobries = false;
		} else if(what.equals("washers")) {
			washers = false;
		}
	}
	
	/**
	 * Public function to check if the merchant actually has any good
	 * listed for sale.
	 * @return TRUE if any of boolean goods variables are true.
	 * 			FALSE otherwise.
	 */
	public boolean trading() {
		if(bolts || nuts || widgets || doobries || washers) {
			return true;
		}
		return false;
	}
	
	/**
	 * Function returns true if the supplied string matches one of the
	 * goods and that good is being traded in.
	 * @param what: String representing good to check.
	 * @return  TRUE if specified good is traded in.
	 * 			FALSE otherwise.
	 */
	public boolean tradesIn(String what) {
		if(what.equals("nuts")) {
			if(nuts)
				return true;
		} else if(what.equals("bolts")) {
			if(bolts)
				return true;
		} else if(what.equals("widgets")) {
			if(widgets)
				return true;
		} else if(what.equals("doobries")) {
			if(doobries)
				return true;
		} else if(what.equals("washers")) {
			if(washers)
				return true;
		}
		//None of the above were true, so return false.
		return false;
	}
}
