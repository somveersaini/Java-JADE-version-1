import java.util.ArrayList;
import java.util.Iterator;

import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.util.Logger;

public class Supplier extends Agent {
	/*
	 * Private object of class logger to log events.
	 */
	private Logger myLogger = Logger.getMyLogger(getClass().getName());
	/*
	 * Private array list, holds info on goods, q-ty and price.
	 */
	private ArrayList<SupplierRecord> listOfGoods = new ArrayList<SupplierRecord>();
	/*
	 * Amount of money agent has.
	 */
	private int money;
	/*
	 * How much agent charges for delivering goods. Fixed and same for all types.
	 */
	private int postage;
	
	private class ListGoodsWithBroker extends Behaviour {

		public ListGoodsWithBroker(Agent a) {
			super(a);
		}
		
		/*
		 * Private integer representing stage of the registration.
		 */
		private int stage = 0;
		/*
		 * 
		 */
		private boolean allGoodsRegistered = false;
		/*
		 * Max index for goods list. 
		 */
		private int maxGoods;
		/*
		 * Current index for goods list.
		 */
		private int currentGood = 0;
		/*
		 * Holds temporary record object.
		 */
		private SupplierRecord goodUnderOffer;
		/*
		 * Holds temporary quantity info about some good.
		 */
		private int tempQty = 0;
		/*
		 * Holds temporary value for the transaction.
		 */
		private int tempMoney = 0;
		
		/**
		 * This is where the agent performs registration action.
		 */
		public void action() {
			//Get how many entries there are in the list.
			maxGoods = listOfGoods.size();
			//Depending on what stage it is, either send message or receive message.
			switch(stage) {
			case 0:
				if(currentGood >= maxGoods) {
					//We have gone through every good on our list.
					myLogger.log(Logger.SEVERE, "Agent " + getLocalName() + " - All of supplier's goods have been processed, moving to waiting phase");
					stage = 2;
				} else {
					//Stage 0 is the send messages stage.
					//Get current object.
					SupplierRecord sr = listOfGoods.get(currentGood);
					//Set up message.
					ACLMessage register = new ACLMessage(ACLMessage.REQUEST);
					//Add recipient - broker.
					register.addReceiver(new AID("Broker", AID.ISLOCALNAME));
					//Set content.
					register.setContent("ADD " + sr.getName());
					//Send message.
					send(register);
					//Now switch to waiting mode.
					stage = 1;
				}
				break;
			case 1:
				//Stage 1 is the wait for reply stage.
				//Get message.
				ACLMessage m = myAgent.receive();
				//Check if it's not empty.
				if(m != null) {
					//Check what type of message it is.
					if(m.getPerformative() == ACLMessage.AGREE) {
						//Say the agent has agreed to add my goods.
						myLogger.log(Logger.SEVERE, "Agent " + getLocalName() + " - received an AGREE from" + m.getSender());
					}
					if(m.getPerformative() == ACLMessage.INFORM) {
						//Received an inform message.
						myLogger.log(Logger.SEVERE, "Agent " + getLocalName() + " - received an INFORM from" + m.getSender());
						//Reset stage to send phase.
						stage = 0;
						//Increment current record.
						currentGood++;
					}
				}
				break;
			case 2:
				//Stage 3, broker waits for requests from clients. A request would look something like:
				//name quantity price_per_one.
				ACLMessage offer = myAgent.blockingReceive();

				//Check it's not empty.
				if(offer != null) {
					//Reply exists.
					String content = offer.getContent();
					//Get all 3 message parts.
					if(content != null) {
						String[] parts = content.split("\\s+");
						String pName = parts[0];
						int qty = Integer.parseInt(parts[1]);
						int pp1 = Integer.parseInt(parts[2]);
					
					
					myLogger.log(Logger.SEVERE, "Agent " + getLocalName() + " - received a PURCHASE request from" + offer.getSender() 
							+ "with product name: " + pName + " nof which " + qty + " units needed with max price per unit of: " + pp1);
					
					//Check merchant trades in this good.
					for(Iterator<SupplierRecord> sr = listOfGoods.iterator(); sr.hasNext();) {
						SupplierRecord rec = sr.next();
						if(rec.getName().equals(pName)) {
							//Found the good, now look for price and qty.
							if(rec.getQuantity() >= qty) {
								//There is enough of it. Check price.
								if(rec.getPrice() <= pp1) {
									//Make an offer!
									ACLMessage offerProduct = new ACLMessage(ACLMessage.PROPOSE);
									int totalPrice = (qty * pp1) + postage;
									offerProduct.setConversationId("purchase");
									offerProduct.setContent(Integer.toString(totalPrice));
									offerProduct.addReceiver(new AID(offer.getSender().getLocalName(), AID.ISLOCALNAME));
									send(offerProduct);
									//Update good object.
									goodUnderOffer = rec;
									tempQty = qty;
									tempMoney = totalPrice;
									//Go to reply stage.
									stage = 3;
								}
							}
						} else {
							//In case the product was not found or there is not enough of it or too expensive.
							ACLMessage refuseProduct = new ACLMessage(ACLMessage.REFUSE);
							refuseProduct.setConversationId("purchase");
							refuseProduct.addReceiver(new AID(offer.getSender().getLocalName(), AID.ISLOCALNAME));
							send(refuseProduct);
							myLogger.log(Logger.SEVERE, "Agent " + getLocalName() + 
									" - sending a REFUSE to client as request could not be fulfilled. ");
						}
					}/* END of iterations */
					}/* END of if content not null */
				} else {
					System.out.println("Onull");
				}
				break;
			case 3: 
				//Now wait for the reply.
				ACLMessage clientReply = myAgent.receive();
				if(clientReply != null) {
					//Check it's an agree message.
					if(clientReply.getPerformative() == ACLMessage.AGREE) {
						//We have a sale!
						updateGood(goodUnderOffer, tempQty);
					}
					stage = 2;
				}
				break;
			}
		}
		
		/**
		 * Function updates the amount of specified good and checks if there is at least
		 * one left. Otherwise de-list this good.
		 * @param record: SupplierObject to update.
		 * @param amount: Integer representing amount to take away.
		 */
		private void updateGood(SupplierRecord record,int amount) {
			//Update amount.
			record.setQty( (record.getQuantity() - amount) );
			//Update merchant money.
			money = tempMoney;
			//Check if still less or equal to 0.
			if(record.getQuantity() <= 0) {
				//De-list with the broker.
				ACLMessage delist = new ACLMessage(ACLMessage.REQUEST);
				delist.addReceiver(new AID("Broker", AID.ISLOCALNAME));
				delist.setContent("DELIST " + record.getName());
				send(delist);
			}
		}
		
		public boolean done() {
			return false;
		}
	} /* END of behavior class */
	
	/**
	 * Function to process argument supplied to the agent and transform them
	 * in to objects and vales.
	 * @param s: String of the current argument.
	 */
	private void processGoodParameters(String s) {
		//Break string in to parts.
		String[] parts = s.split("\\s+");
		//Quantity of goods.
		int quantity = Integer.parseInt(parts[0]);
		//Name of good.
		String good = parts[1];
		//Price per unit of good.
		int price = Integer.parseInt(parts[2]);
		//Create the new object and add to list.
		SupplierRecord rec = new SupplierRecord(good, quantity, price);
		listOfGoods.add(rec);
	}
	
	protected void setup() {
		//This section allows agent to be parameterized.
		Object[] args = getArguments();
		//Set up empty string.
		String s;
		if(args != null) {
			//Arguments supplied are not null.
			for(int i = 0; i < args.length; i++) {
				//Get argument.
				s = (String) args[i];
				if(i == 0) {
					//Money is first parameter according to specification.
					money = Integer.parseInt(s);
				} else if(i == 1) {
					//Shipping is second parameter according to spec.
					postage = Integer.parseInt(s);
				} else {
					//These are strings with "QTY GOOD COST"
					processGoodParameters(s);
				}
			}
		}
		
		// Register agent and set up.
		DFAgentDescription dfd1 = new DFAgentDescription();
		ServiceDescription sd1 = new ServiceDescription();
		sd1.setType("Supplier");
		sd1.setName(getName());
		sd1.setOwnership("MIKE");
		dfd1.setName(getAID());
		dfd1.addServices(sd1);
		try {
			DFService.register(this, dfd1);
			ListGoodsWithBroker listGoods = new ListGoodsWithBroker(this);
			addBehaviour(listGoods);
		} catch (FIPAException e) {
			//If something goes wrong and an agent can not be set up.
			myLogger.log(Logger.SEVERE, "Agent " + getLocalName() + " - Cannot register with DF", e);
			doDelete();
		}
	}/* END of setup() function */
}