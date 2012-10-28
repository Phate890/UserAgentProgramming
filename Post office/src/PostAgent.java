/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Bart
 */
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;

public class PostAgent extends Agent {

    //set constants to clarify functions
    final int MASTER = 0;
    final int SLAVE = 1;

    @Override
    protected void setup() {

        Object[] args = getArguments();
        int function = 0, nrOfLetters = 0, time = 0;
        //Function holds the value for the agent to specify it as master or slave
        if (args[0] != null) {
            function = Integer.parseInt((String) args[0]);
        }
        //number of letters currently held by an agent
        if (args[1] != null) {
            nrOfLetters = Integer.parseInt((String) args[1]);
        }
        //available worktime an agent has
        if (args[2] != null) {
            time = Integer.parseInt((String) args[2]);
        }
        
        //Search the system for all active agents
        AMSAgentDescription[] agents = null;
        try {
            SearchConstraints c = new SearchConstraints();
            c.setMaxResults(new Long(-1));
            agents = AMSService.search(this, new AMSAgentDescription(), c);
        } catch (Exception e) {
            System.out.println("Problem in finding agents: " + e);
        }
        //substract 3 default agents to find the number of active agents we are using
        int nrOfAgents = agents.length - 3;
        
        //Add behaviour to our agents
        addBehaviour(new myBehaviour(this, nrOfLetters, time, function, nrOfAgents));


    }

    class myBehaviour extends SimpleBehaviour {

        int nrOfLetters;
        int action;
        int time;
        int nrOfAgents;
        boolean isAvailable = false;

        public myBehaviour(Agent a, int nrOfLetters, int time, int action, int nrOfAgents) {
            super(a);
            this.nrOfLetters = nrOfLetters;
            this.time = time;
            this.action = action;
            this.nrOfAgents = nrOfAgents;
        }

        @Override
        public void action() {
            switch (action) {
                //The slave agent listens to tasks all the time, upon receiving a message
                //it either responds to inform the master of its current workload and 
                //time, or adds the workload to his current workload
                case SLAVE:
                    ACLMessage msgrecSlave;
                    msgrecSlave = receive();
                    if (msgrecSlave != null && msgrecSlave.getPerformative() != ACLMessage.AGREE) {
                        ACLMessage msgInf = new ACLMessage(ACLMessage.INFORM);
                        AID sender = msgrecSlave.getSender();
                        msgInf.addReceiver(sender);
                        msgInf.setContent(nrOfLetters + "," + time);
                        send(msgInf);
                    } else if (msgrecSlave != null && msgrecSlave.getPerformative() == ACLMessage.AGREE) {
                        int letter = Integer.parseInt(msgrecSlave.getContent());
                        nrOfLetters += letter;
                    }
                    break;
                case MASTER:
                    doTask(6);
                    doTask(4);
                    doTask(4);

                    isAvailable = true;
                    break;
            }

        }

        /**
         * This method finds the best available agent to perform his task on the number 
         * of letters specified
         * @param letters - the number of letters that have to be handled by an agent
         */
        public void doTask(int letters) {
            int topRating = 0;
            AID topAgent = null;
            
            //Send every agent a request to find out who can do work and who has
            //enough time for the job
            for (int i = 2; i <= nrOfAgents; i++) {
                ACLMessage msgrecMaster;
                ACLMessage msgReq = new ACLMessage(ACLMessage.REQUEST);
                msgReq.setContent("Who is available for work?");
                msgReq.addReceiver(new AID("agent" + i, AID.ISLOCALNAME));
                send(msgReq);
                //System.out.println(msgReq);
                
                //Wait for a message back, receive does not seem to give reliable
                //results, so therefore blockingReceive is used. This means all agents
                //have to be able to send a message back at all time
                msgrecMaster = blockingReceive();
                if (msgrecMaster != null) {
                    //The agents send back their current workload and time
                    String[] split = msgrecMaster.getContent().split(","); //letters, time
                    int curLett = Integer.parseInt(split[0]);
                    int curTime = Integer.parseInt(split[1]);
                    //A rating is calculated based on workload and time, for this
                    //program a simple 1 on 1 rate is being used for simplicity
                    int rating = curTime - curLett;
                    //When the rating is sufficient and better than the rating found before
                    //the agent and rating are updated to find the best agent for the job
                    if (rating >= letters && rating > topRating) {
                        topAgent = msgrecMaster.getSender();
                        topRating = rating;
                    }
                }
            }
            //The best agent has now been found, the agent will be notified and
            //the task can be perfomed
            ACLMessage msgReq = new ACLMessage(ACLMessage.AGREE);
            msgReq.setContent(letters + "");
            msgReq.addReceiver(topAgent);
            send(msgReq);
            //When an agent has been found, print its name for testing purpose
            if (topAgent != null) {
                System.out.println(topAgent.getLocalName());
            }
        }

        @Override
        public boolean done() {
            return isAvailable;
        }
    }
}
