import java.util.ArrayList;
import java.util.Iterator;

import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.util.Logger;

public class Client extends Agent{
	/*
	 * Private logger object.
	 */
	private Logger myLogger = Logger.getMyLogger(getClass().getName());
	/*
	 * Money the client is willing to spend.
	 */
	private int money;
	/*
	 * Holds the records for each good that this client wants.
	 */
	private ArrayList<ClientRecord> listOfGoods = new ArrayList<ClientRecord>();
	/*
	 * message template.
	 */
	private MessageTemplate mt;
	
	private class ClientFindSupplierAndOrder extends Behaviour {

		public ClientFindSupplierAndOrder(Agent a) {
			super(a);
		}
		/*
		 * Holds information as to how many entries we can have in total.
		 */
		private int maxGoods;
		/*
		 * Holds pointer to the good currently being processed.
		 */
		private int currentGood = 0;
		/*
		 * Current state of the program execution.
		 */
		private int stage = 0;
		/*
		 * Holds flag condition for a repeated search.
		 */
		private boolean supplierFound = false;
		/*
		 * Holds index to good that is to be negotiated.
		 */
		private int currentGoodForNegotiation = 0;
		/*
		 * Holds the number of replies.
		 */
		private int repliesCount = 0;
		/*
		 * Holds number of messages sent.
		 */
		private int messagesSent = 0;
		
		/**
		 * This function essentially keeps repeating itself performing everything in it.
		 * There are several stages to this process: stages 1 and 2 is essentially sending messages
		 * to broker and gathering information about suppliers.
		 * Stage 3 is the communication with the supplier.
		 */
		public void action() {
			maxGoods = listOfGoods.size();
			switch(stage) {
			case 0:
				//The very first state. For every object in goods list.
				if(currentGood >= maxGoods) {
					//We have been through every good on the list. Move to state 2.
					stage = 2;
					myLogger.log(Logger.SEVERE, "Agent " + getLocalName() + " - I have found suppliers for all goods, moving to waiting mode.");
				} else {
					//Reset supplier found.
					supplierFound = false;
					//There are still goods to look at. Get current good.
					//Set up current good record.
					ClientRecord rec = listOfGoods.get(currentGood);
					//Set up the message to broker.
					ACLMessage m = new ACLMessage(ACLMessage.REQUEST);
					//Add recipient - Broker.
					m.addReceiver(new AID("Broker", AID.ISLOCALNAME));
					//Add content.
					m.setContent("LIST " + rec.getName());
					//Send message.
					send(m);
					//Set stage to 1.
					stage = 1;
				}
				break;
			case 1:
				//This is a receiver stage, now keep checking the message box until SEARCH_FINISHED is detected.
				ACLMessage r = myAgent.receive();
				//Check message is not empty.
				if(r != null) {
					//Check what type of message this is.
					if(r.getPerformative() == ACLMessage.INFORM) {
						String s = r.getContent();
						if(s.indexOf("FOUND") != -1) {
							//Hey, there is a supplier for my good! Split original message.
							String[] message = s.split("\\s+");
							//Now extract supplier name.
							String name = message[1];
							//Save supplier name to object.
							ClientRecord cr = listOfGoods.get(currentGood);
							//Check if such record already exists.
							if(cr.hasEntry(name)) {
								//Do nothing.
							} else {
								//Add new supplier to list.
								cr.addSupplier(name);
							}
							//found a supplier.
							supplierFound = true;
							myLogger.log(Logger.SEVERE, "Agent " + getLocalName() + " - received a FOUND from" + r.getSender());
						}
						if(s.indexOf("SEARCH_FINISHED") != -1) {
							//Search has ended. Time to move to next good if any.
							stage = 0;
							//Search finished without results?
							if(supplierFound) {
								//Search finished with results.
								//And update current good.
								currentGood++;
								//Switch stage.
								stage = 0;
								//Log event.
								myLogger.log(Logger.SEVERE, "Agent " + getLocalName() + " - received a SEARCH_FINISHED from" + r.getSender());
							} else {
								//Search finished without results. Re-send message without updating the good.
								stage = 0;
							}
						}
					}
				}
				break;
			case 2:
				//At stage 2 I know there is at least one supplier for each good.
				if(currentGoodForNegotiation >= maxGoods) {
					//Stop as we have got all goods sorted.
					stage = 5;
				} else {
					//There are still goods to be purchased.
					ACLMessage msg = new ACLMessage(ACLMessage.CFP);
					//Set up good object.
					ClientRecord cr = listOfGoods.get(currentGoodForNegotiation);
					//Add every receiver to message.
				    for (int i = 0; i < cr.getKnownSuppliers(); ++i) {
				    	msg.addReceiver(new AID(cr.getSupplierNameAt(i), AID.ISLOCALNAME));
				    	messagesSent++;
				    }
				    //Set name of good in question.
				    msg.setContent(cr.getName() + " " + cr.getQuantity() + " " + cr.getPricePerOne());
				    //Set conversation topic.
				    msg.setConversationId("purchase");
				    msg.setReplyWith("msg"+System.currentTimeMillis()); // Unique value
				    //Finally send message.
				    myAgent.send(msg);
				    //Set up message template
				    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("purchase"),MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
				    //Finally go to next stage.
				    stage = 3;
				}
				break;
			case 3:
				//At this stage: get all proposals of refusals from the supplier agent.
				//Get reply.
				ACLMessage reply = myAgent.receive();
				
				//Check if reply does not contain anything.
				if(reply != null) {
				  //Get what was the message.
					//Initialize comparison variable.
					int cheapest = 0;
					//Initialize cheapest offer sender.
					String supplier = null;
					if(reply.getPerformative() == ACLMessage.PROPOSE) {
						myLogger.log(Logger.SEVERE, "Agent " + getLocalName() + " - received an OFFER from" + reply.getSender());
						//PROPOSE means agent received an offer. Total cost is carried as argument.
						repliesCount++;
						//Get the offer.
						int offer = Integer.parseInt(reply.getContent());
						//Check if this is the best offer.
						if(cheapest == 0 || cheapest > offer) {
							//make this offer the new cheapest and update seller details.
							cheapest = offer;
							supplier = reply.getSender().getLocalName();
						}
					} else if(reply.getPerformative() == ACLMessage.REFUSE) {
						myLogger.log(Logger.SEVERE, "Agent " + getLocalName() + " - received a REFUSE from" + reply.getSender());
						repliesCount++;
					}
					if(repliesCount == messagesSent) {
						//Received all replies. Time to pick the cheapest.
						if(supplier != null) {
							//make sure there is the cheapest offer.
							//Set up message.
							ACLMessage purchaseOffer = new ACLMessage(ACLMessage.AGREE);
							//Add recipient.
							purchaseOffer.addReceiver(new AID(supplier, AID.ISLOCALNAME));
							//Send offer.
							send(purchaseOffer);
							//Update money.
							money -= cheapest;
							//Update units of good required.
							ClientRecord cl = listOfGoods.get(currentGoodForNegotiation);
							cl.setQuantity(0);
						}
						//Swich back to stage 2
						stage = 2;
						//Update good in question
						currentGoodForNegotiation++;
						//Reset replies and message counts.
						repliesCount = 0;
						messagesSent = 0;
					}
				} else {
					block(); 
				}
				break;
			case 5:
				myLogger.log(Logger.SEVERE, "Agent " + getLocalName() + " - agent is done, *DELETING*");
				myAgent.doDelete();
				break;
			}/* END of switch */
		}

		@Override
		public boolean done() {
			return false;
		}
	} /* END of private class */
	
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
		//Create the new object and add to list. name of good, how many needed, max per unit.
		ClientRecord rec = new ClientRecord(good, quantity, price);
		listOfGoods.add(rec);
	}
	
	protected void setup() {
		Object[] args = getArguments();
		String s;

		if(args != null) {
			for(int i = 0; i < args.length; i++) {
				//Get argument.
				s = (String) args[i];
				if(i == 0) {
					money = Integer.parseInt(s);
				} else {
					processGoodParameters(s);
				}
			}
		}

		//Set up the agent and register it.
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Client");
		sd.setName(getName());
		sd.setOwnership("MIKE");
		dfd.setName(getAID());
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
			ClientFindSupplierAndOrder findAndOrder = new ClientFindSupplierAndOrder(this);
			addBehaviour(findAndOrder);
		} catch (FIPAException e) {
			myLogger.log(Logger.SEVERE, "Agent " + getLocalName() + " - Cannot register with DF", e);
			doDelete();
		}
	} /* END of setup() function */
} /* END of class */
