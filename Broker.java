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

public class Broker extends Agent{
	/*
	 * Private object of class logger to log events.
	 */
	private Logger myLogger = Logger.getMyLogger(getClass().getName());
	/*
	 * Private array list of objects with merchants.
	 */
	private ArrayList<Merchant> registeredSuppliers = new ArrayList<Merchant>();
	
	private class WaitSupplierRegisterBehaviour extends CyclicBehaviour {
		
		public WaitSupplierRegisterBehaviour(Agent a) {
			super(a);
		}

		/**
		 * This function essentially keeps repeating itself performing everything in it.
		 * My agent accepts messages of the form: ACTION argument;ACTION argument;...
		 */
		public void action() {
			//Retrieve messages if any.
			ACLMessage m = myAgent.receive();
			//Check if message is not NULL.
			if(m != null) {
				//Okay there is some message, now check type.
				if(m.getPerformative() == ACLMessage.REQUEST) {
					//We have a request. Split it at ; in case it is a multiple request.
					String[] requests = m.getContent().split(";");
					//Trim trailing and front spaces.
					for(int i = 0; i < requests.length; i ++) {
						requests[i] = requests[i].trim();
						//For each individual request split it in to ACTION and argument.
						String[] parts = requests[i].split("\\s+");
						//Now check there are exactly 2 entries.
						if(parts.length == 2) {
							//Exactly 2 entries.
							String action = parts[0];
							String argument = parts[1];
							if(action.equals("ADD")) {
								//Supplier wishes to register.
								addSupplierRecord(argument, m.getSender().getLocalName());
								sendAgree(m.getSender().getLocalName());
							} else if (action.equals("DELETE")) {
								//Supplier wishes to delete.
								deleteSupplierRecord(argument, m.getSender().getLocalName());
								sendAgree(m.getSender().getLocalName());
							} else if (action.equals("LIST")) {
								//Consumer wishes to get info.
								searchAndSendToClient(argument, m.getSender().getLocalName());
								sendAgree(m.getSender().getLocalName());
							} else {
								//No idea who you are or what you want, sorry. Error message.
								sendUnknownCommandError(m.getSender(), action);
							}
						}
					}/* END of for loop of message arguments */
				}
			}/* END message null check */
		}/* END of action() */
		
		/**
		 * Function takes in two parameters: argument an name. Name represents the
		 * name of the agent that has requested information and argument represents
		 * the good that the client wished to find.
		 * @param argument: Good to be searched for.
		 * @param name: String name of the requesting client.
		 */
		private void searchAndSendToClient(String argument, String name) {
			if(registeredSuppliers.isEmpty()) {
				//List is empty.
				sendEndSearchMessage(name);
			} else {
				//Start iterating through the list.
				for(Iterator<Merchant> m = registeredSuppliers.iterator(); m.hasNext();) {
					//Current merchant object.
					Merchant supplier = m.next();
					if(supplier.tradesIn(argument)) {
						//Found a supplier that trades in what's required.
						sendSupplierNameToClient(supplier.getName(), name);
					}
				}
				//Iterated through the entire loop. Send searched all message.
				sendEndSearchMessage(name);
			}
		}
		
		/**
		 * Function that goes through every merchant in the list until the merchant
		 * that has the name passed in parameter is found. After which update the
		 * merchant and if every good is now not traded in, completely de-list the
		 * merchant.
		 * @param argument: String representing what good to remove.
		 * @param name: String representing name of the merchant in question.
		 */
		private void deleteSupplierRecord(String argument, String name) {
			//First things first - find the merchant.
			if(registeredSuppliers.size() != 0) {
				//List contains some merchant.
				for(Iterator<Merchant> m = registeredSuppliers.iterator(); m.hasNext();) {
					//Go through every merchant in the list. Check for matches in name.
					Merchant supplier = m.next();
					if(supplier.getName().equals(name)) {
						if(argument.equals("all")) {
							//Simply de-list the agent.
							registeredSuppliers.remove(supplier);
						} else {
							//Remove specified goods.
							supplier.delistItem(argument);
						}
						informDone(name);
					}
					if(supplier.trading() == false) {
						registeredSuppliers.remove(supplier);
					}
				}
			} else {
				//List has no merchants.
				sendNotInList(name);
			}
		}
		
		/**
		 * Function essentially lists a supplier as now trading in a good specified by
		 * the parameter. String name is used to identify the supplier as each individual
		 * supplier has a unique name.
		 * @param argument: String with good to add.
		 * @param name: String with name of the Merchant.
		 */
		private void addSupplierRecord(String argument, String name) {
			Merchant merchant;
			//Firstly go through the list of suppliers and check if supplier with such name exists.
			if(registeredSuppliers.size() != 0) {
				//List of registered merchants is not empty.
				for(Iterator<Merchant> m = registeredSuppliers.iterator(); m.hasNext();) {
					//Go through every merchant in the list. Check for matches in name.
					Merchant supplier = m.next();
					if(supplier.getName().equals(name)) {
						//Found matching name. Update.
						supplier.updateMerchant(argument);
						//Send confirmation.
						informDone(name);
					}
				}
				//Went through every entry, no matching merchant, create new.
				merchant = new Merchant(name);
				//Set the required good traded.
				merchant.updateMerchant(argument);
				//Add merchant to list.
				registeredSuppliers.add(merchant);
				//Send confirmation.
				informDone(name);
			} else {
				//List of registered merchants is empty. Create new one.
				merchant = new Merchant(name);
				//Update merchant with a specified good.
				merchant.updateMerchant(argument);
				//Add merchant to list.
				registeredSuppliers.add(merchant);
				//Send confirmation.
				informDone(name);
				/*
				 * I know this is a bit redundant since else has the same code as if,
				 * and this can be avoided, but right now I think this is a valid
				 * implementation. Will change it if I have time.
				 */
			}
		}
		
		/**
		 * Function sends a message to the recipient indicating a supplier for the required
		 * good was FOUND followed by the name of supplier (FOUND supplierName).
		 * @param supplierName: String representing name of the supplier.
		 * @param recipientName: String representing name of the recipient.
		 */
		private void sendSupplierNameToClient(String supplierName, String recipientName) {
			//Set up message.
			ACLMessage message = new ACLMessage(ACLMessage.INFORM);
			//Add a recipient.
			message.addReceiver(new AID(recipientName, AID.ISLOCALNAME));
			//Set up content.
			message.setContent("FOUND " + supplierName);
			//Send message.
			send(message);
		}
		
		/**
		 * Function sends INFORM message that tells the search is over.
		 * @param name: String containing the name of the recipient.
		 */
		private void sendEndSearchMessage(String name) {
			//Set up message.
			ACLMessage message = new ACLMessage(ACLMessage.INFORM);
			//Add a recipient.
			message.addReceiver(new AID(name, AID.ISLOCALNAME));
			//Set up content.
			message.setContent("SEARCH_FINISHED");
			//Send message.
			send(message);
		}
		
		/**
		 * Function that sends an inform message to some agent that is specified by
		 * the agent name informing that some operation was completed successfully.
		 * @param name: String representing name of the recipient.
		 */
		private void informDone(String name) {
			//Set up message.
			ACLMessage message = new ACLMessage(ACLMessage.INFORM);
			//Add a recipient.
			message.addReceiver(new AID(name, AID.ISLOCALNAME));
			//Set up content.
			message.setContent("done");
			//Send message.
			send(message);
		}
		
		/**
		 * Function to send an error message to the agent with a specified name.
		 * @param name: Specify which agent the message is for.
		 */
		private void sendNotInList(String name) {
			//Set up message.
			ACLMessage message = new ACLMessage(ACLMessage.INFORM);
			//Add a recipient.
			message.addReceiver(new AID(name, AID.ISLOCALNAME));
			//Set up content.
			message.setContent("ERROR you are not in the merchants list");
			//Send message.
			send(message);
		}
		
		/**
		 * Function that sends an AGREE message to specified address.
		 * @param aid: address of the recipient.
		 */
		private void sendAgree(String name) {
			//Set up message.
			ACLMessage message = new ACLMessage(ACLMessage.AGREE);
			//Add a recipient.
			message.addReceiver(new AID(name, AID.ISLOCALNAME));
			//Send message.
			send(message);
		}
		
		/**
		 * Function takes in an address of recipient and string with invalid command,
		 * compiles all this in a message and sends it.
		 * @param aid: address of the recipient.
		 * @param action: String with invalid command.
		 */
		private void sendUnknownCommandError(AID aid, String action) {
			//Set up message.
			ACLMessage error = new ACLMessage(ACLMessage.REFUSE);
			//Add a recipient.
			error.addReceiver(aid);
			//Set content to unknown action.
			error.setContent("UNKNOWN COMMAND " + action);
			//Send message.
			send(error);
		}
		
	}
	
	protected void setup() {
		DFAgentDescription dfd1 = new DFAgentDescription();
		ServiceDescription sd1 = new ServiceDescription();
		
		//Set up the agent and register it.
		sd1.setType("Broker");
		sd1.setName(getName());
		sd1.setOwnership("MIKE");
		dfd1.setName(getAID());
		dfd1.addServices(sd1);
		try {
			DFService.register(this, dfd1);
			WaitSupplierRegisterBehaviour supplierRegistrationBehaviour = new WaitSupplierRegisterBehaviour(this);
			addBehaviour(supplierRegistrationBehaviour);
		} catch (FIPAException e) {
			//In case registration will not be possible.
			myLogger.log(Logger.SEVERE, "Agent " + getLocalName() + " - Cannot register with DF", e);
			doDelete();
		}
	} /* END of Setup() */
}
