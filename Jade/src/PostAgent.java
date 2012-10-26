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

    final int MASTER = 0;
    final int NO_JOB = 1;

    @Override
    protected void setup() {

        Object[] args = getArguments();
        int action = 0, nrOfLetters = 0;
        if (args[0] != null) {
            action = Integer.parseInt((String) args[0]);
        }
        if (args[1] != null) {
            nrOfLetters = Integer.parseInt((String) args[1]);
        }

        AMSAgentDescription[] agents = null;
        try {
            SearchConstraints c = new SearchConstraints();
            c.setMaxResults(new Long(-1));
            agents = AMSService.search(this, new AMSAgentDescription(), c);
        } catch (Exception e) {
            System.out.println("Problem in finding agents: " + e);
        }

        addBehaviour(new myBehaviour(this, nrOfLetters, action));


    }

    class myBehaviour extends SimpleBehaviour {

        int nrOfLetters;
        int action;

        public myBehaviour(Agent a, int nrOfLetters, int action) {
            super(a);
            this.nrOfLetters = nrOfLetters;
            this.action = action;
        }
        int n = 0;
        boolean isAvailable = false;

        @Override
        public void action() {

            ACLMessage msgReq = new ACLMessage(ACLMessage.REQUEST);
            ACLMessage msgInf = new ACLMessage(ACLMessage.INFORM);
            ACLMessage msgrec;


            switch (action) {
                case NO_JOB:
                    msgrec = receive();
                    if (msgrec != null) {
                        AID sender = msgrec.getSender();
                        msgInf.addReceiver(sender);
                        msgInf.setContent("" + nrOfLetters);
                        send(msgInf);
                    }
                    break;
                case MASTER:
                    for (int i = 1; i < 3; i++) {
                        msgReq.setContent("" + nrOfLetters);
                        msgReq.addReceiver(new AID("agent" + i, AID.ISLOCALNAME));
                        send(msgReq);

                        msgrec = receive();
                        if (msgrec != null) {
                            /**
                             * receive info from agent and save
                             */
                        }
                    }

                    /**
                     * Calculate the best option and inform the agent
                     */
                    isAvailable = true;
                    break;
            }

        }

        public void sendAllAgents() {
        }

        @Override
        public boolean done() {
            return isAvailable;
        }
    }
}
