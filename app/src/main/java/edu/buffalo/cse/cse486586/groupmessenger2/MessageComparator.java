package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Comparator;

class MessageComparator implements Comparator<Message> {
    @Override
    public int compare(Message lhs, Message rhs) {
        if (lhs.maxAgreedSeq < rhs.maxAgreedSeq) {
            return -1;
        }
        if (lhs.maxAgreedSeq > rhs.maxAgreedSeq) {
            return 1;
        }
        return 0;
    }
}
