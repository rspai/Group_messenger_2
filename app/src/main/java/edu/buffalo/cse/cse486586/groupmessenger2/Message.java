package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

class Message implements Serializable {
    String msgStr;
    String type;
    int senderPort;
    int proposedSeq;
    int proposingPort;
    int deliverable;
    int maxAgreedSeq;
    int failedAVD;

    public int getMsgType() {
        if(type.equals("INITIAL")) {
            return 1;
        }
        else if(type.equals("RESPONSE")) {
            return 2;
        }
        else if(type.equals("FINAL")) {
            return 3;
        }
        else if(type.equals("ACK")) {
            return 4;
        }
        return 0;
    }

    public void setMsgType(String s) {
        this.type = s;
    }

    public int getDeliverableStatus() {
        return deliverable;
    }

    public int getSenderPort() {
        return senderPort;
    }

    public int getFailedAVD() {
        return failedAVD;
    }

}
