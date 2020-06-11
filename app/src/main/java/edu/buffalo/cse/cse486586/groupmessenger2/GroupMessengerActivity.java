package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final int SERVER_PORT = 10000;
    static final String[] REMOTE_PORT = {"11108", "11112", "11116", "11120", "11124"};

    int msgSeqNum = 0;
    int proposedSequence = 1;
    int failedRemortPort = 0;
    int currentAVD = 0;
    int d;
    Comparator<Message> comparator = new MessageComparator();
    public PriorityQueue<Message> queue = new PriorityQueue(25,comparator);
    HashMap<Integer, Message> avdHashMap = new HashMap<Integer, Message>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = (EditText) findViewById(R.id.editText1);
                String msg = editText.getText().toString() + "\n";
                editText.setText("");
                Message msg1 = new Message();
                msg1.msgStr = msg;
                msg1.type = "INITIAL";
                msg1.senderPort = Integer.parseInt(myPort);
                if(avdHashMap.isEmpty() || (!avdHashMap.containsKey(Integer.parseInt(myPort)))) {
                    avdHashMap.put(Integer.parseInt(myPort), msg1);
                } else {
                    avdHashMap.remove(Integer.parseInt(myPort));
                    avdHashMap.put(Integer.parseInt(myPort), msg1);
                }
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg1);
            }
        });
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        int updateProposedSeq(int seq, int msgType, int maxSeqN) {
            int newSeq = 0;
            switch (msgType) {
                case(1): {
                    newSeq = seq + ((int )(Math.random() * 100 + 1));
                    break;
                } case(2): {
                    newSeq = (seq > maxSeqN) ? seq : maxSeqN;
                    break;
                } case(3): {
                    int x = maxSeqN + 1;
                    newSeq = (seq > x) ? seq : x;
                    break;
                } case(4): {
                    newSeq = seq;
                    break;
                }
            }
            return newSeq;
        }

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            try {
                Socket socket = null;
                ObjectInputStream dataIntoS = null;
                ObjectOutputStream dataOutS = null;
                while(true) {
                    socket = serverSocket.accept();
                    dataIntoS = new ObjectInputStream(socket.getInputStream());
                    dataOutS = new ObjectOutputStream(socket.getOutputStream());
                    Message msgFromClient = null;
                    try {
                        msgFromClient = (Message) dataIntoS.readObject();
                    } catch (ClassNotFoundException e) {
                        Log.e(TAG,"ServerTask: ClassNotFoundException");
                        e.printStackTrace();
                    }

                    String[] msgObjArr = new String[8];
                    msgObjArr[0] = msgFromClient.msgStr;
                    msgObjArr[1] = msgFromClient.type;
                    msgObjArr[2] = String.valueOf(msgFromClient.senderPort);
                    msgObjArr[3] = String.valueOf(msgFromClient.proposedSeq);
                    msgObjArr[4] = String.valueOf(msgFromClient.proposingPort);
                    msgObjArr[5] = String.valueOf(msgFromClient.deliverable);
                    msgObjArr[6] = String.valueOf(msgFromClient.maxAgreedSeq);
                    msgObjArr[7] = String.valueOf(msgFromClient.failedAVD);

                    Log.i(TAG, "This is " + msgFromClient.type + " message in the transmission cycle");

                    if(msgFromClient.getMsgType() == 1) {
                        msgObjArr[1] = "RESPONSE";
                        msgObjArr[3] = String.valueOf(proposedSequence);
                        proposedSequence = proposedSequence + ((int )(Math.random() * 100 + 1));
                        msgObjArr[5] = String.valueOf(0);
                        msgFromClient.type = msgObjArr[1];
                        msgFromClient.proposedSeq = Integer.parseInt(msgObjArr[3]);
                        msgFromClient.deliverable = Integer.parseInt(msgObjArr[5]);
                        if(queue.add(msgFromClient)) {
                            Log.i(TAG, "1 - Adding to queue done");
                        } else {
                            Log.i(TAG, "1 - Add to queue failed");
                        }
                        dataOutS.writeObject(msgFromClient);
                        dataOutS.flush();
                    }
                    else if(msgFromClient.getMsgType() == 3) {
                        //proposedSequence = updateProposedSeq(proposedSequence, msgFromClient.getMsgType(), msgFromClient.maxAgreedSeq);
                        proposedSequence = Math.max(proposedSequence, msgFromClient.maxAgreedSeq + 1);

                        //reference taken from: https://www.boraji.com/java-how-to-iterate-over-a-priorityqueue
                        Iterator<Message> queueIterator = queue.iterator();
                        while(queueIterator.hasNext()) {
                            Message currMsgInQ = queueIterator.next();
                            int c = (msgObjArr[0]).compareTo(currMsgInQ.msgStr);
                            if(c == 0) {
                                if(queue.remove(currMsgInQ)) {
                                    Log.i(TAG, "3 - Removing from queue done");
                                } else {
                                    Log.i(TAG, "3 - Remove from queue failed");
                                }
                            } else {
                                Log.i(TAG, "Msg not in queue");
                            }
                        }

                        msgObjArr[5] = String.valueOf(1);
                        msgFromClient.deliverable = Integer.parseInt(msgObjArr[5]);
                        if(queue.add(msgFromClient)) {
                            Log.i(TAG, "3 - Adding msg to queue");
                        } else {
                            Log.i(TAG, "3 - Add to queue failed");
                        }

                        Log.i(TAG, "Sending out ACK from ServerTask");
                        Message ack = new Message();
                        ack.type="ACK";
                        dataOutS.writeObject(ack);
                        dataOutS.flush();

                        Message qHead = null;
                        while((qHead = queue.peek()) != null) {
                            d = qHead.getDeliverableStatus();
                            if(d == 1) {
                                Message qMsg = queue.poll();
                                publishProgress(qMsg.msgStr);
                            } else {
                                Log.i(TAG, "Msg not in deliverable status yet");
                                break;
                            }
                        }
                    }

                    if(msgFromClient.failedAVD == 0) {
                        Log.i(TAG, "No AVD failure so far");
                    } else {
                        Log.i(TAG, "AVD failed, delete msg from queue");
                        Iterator<Message> queueIterator = queue.iterator();
                        while(queueIterator.hasNext()) {
                            Message currMsgInQ = queueIterator.next();
                            if(currMsgInQ.senderPort == Integer.parseInt(msgObjArr[7])) {
                                if(currMsgInQ.deliverable == 0) {
                                    if (queue.remove(currMsgInQ)) {
                                        Log.i(TAG, "Removed msg from queue on AVD failure");
                                    } else {
                                        Log.i(TAG, "Removing from queue failed, AVD failure case");
                                    }
                                } else {
                                    Log.i(TAG, "Msg at current location in queue not yet deliverable");
                                }
                            } else {
                                Log.i(TAG, "Not same as msg in queue head");
                            }
                        }
                    }
                    dataIntoS.close();
                    dataOutS.close();
                    socket.close();
                }

            } catch (IOException e) {
                Log.e(TAG, "ClientTask: IOException");
                e.printStackTrace();
            } catch (Exception e) {
                Log.e(TAG, "Msg receive failed");
                e.printStackTrace();
            }
            return null;
        }

        protected void onProgressUpdate(String...strings) {
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            TextView localTextView = (TextView) findViewById(R.id.textView1);
            localTextView.append("\n");

            Log.i(TAG, "key - value pair: " + msgSeqNum + " - " + strReceived);

            Uri providerUri = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger2.provider");
            ContentValues keyValueToInsert = new ContentValues();
            keyValueToInsert.put("key", msgSeqNum);
            keyValueToInsert.put("value", strReceived);
            getContentResolver().insert(providerUri, keyValueToInsert);
            msgSeqNum = msgSeqNum + 1;

            return;
        }
    }

    private class ClientTask extends AsyncTask<Message, Void, Void> {

        protected int getMax(int a, int b) {
            int maxVal = 0;
            if(a > b) {
                maxVal = a;
            } else {
                maxVal = b;
            }
            return maxVal;
        }

        @Override
        protected Void doInBackground(Message... msgs) {
            Message msgToSend = msgs[0];
            int i, maxSeq;

            if(msgToSend.getMsgType() == 1) {
                try {
                    Socket socket = null;
                    ObjectOutputStream dataOutC = null;
                    ObjectInputStream dataIntoC = null;
                    Message msgFromS = null;
                    maxSeq = 0;

                    HashMap<Integer, Message> map_avd = new HashMap<Integer, Message>();

                    for (i = 0; i < REMOTE_PORT.length; i++) {
                        currentAVD = Integer.parseInt(REMOTE_PORT[i]);
                        Log.i(TAG, "Current AVD: " + currentAVD);

                        if(failedRemortPort == currentAVD) {
                            Log.i(TAG, "Current AVD failed");
                        } else {
                            /* reference taken from:
                             * https://stackoverflow.com/questions/4969760/setting-a-timeout-for-socket-operations?answertab=votes#tab-top
                             * https://docs.oracle.com/javase/6/docs/api/java/net/Socket.html#connect%28java.net.SocketAddress%29
                             */
                            socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), currentAVD);
                            socket.setSoTimeout(2000);

                            try {
                                dataOutC = new ObjectOutputStream(socket.getOutputStream());
                                dataIntoC = new ObjectInputStream(socket.getInputStream());
                            } catch(IOException e) {
                                Log.e(TAG, "ClientTask: IOException");
                                e.printStackTrace();
                                failedRemortPort = currentAVD;
                                socket.close();
                                continue;
                            }

                            msgToSend.proposingPort = currentAVD;
                            msgToSend.failedAVD = failedRemortPort;
                            dataOutC.writeObject(msgToSend);
                            dataOutC.flush();

                            try {
                                msgFromS = (Message) dataIntoC.readObject();
                            } catch (ClassNotFoundException e) {
                                Log.e(TAG, "ClientTask: ClassNotFoundException");
                                e.printStackTrace();
                            }

                            String[] msgObjArr1 = new String[8];
                            msgObjArr1[0] = msgFromS.msgStr;
                            msgObjArr1[1] = msgFromS.type;
                            msgObjArr1[2] = String.valueOf(msgFromS.senderPort);
                            msgObjArr1[3] = String.valueOf(msgFromS.proposedSeq);
                            msgObjArr1[4] = String.valueOf(msgFromS.proposingPort);
                            msgObjArr1[5] = String.valueOf(msgFromS.deliverable);
                            msgObjArr1[6] = String.valueOf(msgFromS.maxAgreedSeq);
                            msgObjArr1[7] = String.valueOf(msgFromS.failedAVD);

                            try {
                                map_avd.put(msgFromS.proposingPort, msgFromS);
                            } catch (NullPointerException e) {
                                Log.e(TAG, "No value in Message object");
                                e.printStackTrace();
                            }

                            if(msgFromS.getMsgType() == 2) {
                                //maxSeq = getMax(maxSeq, msgFromS.proposedSeq);
                                maxSeq = Math.max(maxSeq, msgFromS.proposedSeq);
                            }

                            dataOutC.close();
                            dataIntoC.close();
                            socket.close();
                        }
                    }

                    Socket socket1 = null;
                    ObjectOutputStream dataOutC1 = null;
                    ObjectInputStream dataIntoC1 = null;
                    Message ack = null;

                    for(i = 0; i < REMOTE_PORT.length; i++) {
                        currentAVD = Integer.parseInt(REMOTE_PORT[i]);
                        Log.i(TAG, "Current AVD: " + currentAVD);

                        if(failedRemortPort == currentAVD) {
                            Log.i(TAG, "Current AVD failed");
                        } else {
                            socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), currentAVD);
                            try {
                                dataOutC1 = new ObjectOutputStream(socket1.getOutputStream());
                                dataIntoC1 = new ObjectInputStream(socket1.getInputStream());
                            } catch (IOException e) {
                                Log.i(TAG, "ClientTask: IOException");
                                e.printStackTrace();
                                failedRemortPort = currentAVD;
                                socket1.close();
                                continue;
                            }

                            Message mapVal = null;
                            if(!map_avd.isEmpty()) {
                                if(map_avd.containsKey(currentAVD)) {
                                    mapVal = map_avd.get(currentAVD);
                                    if(mapVal.getMsgType() == 2) {
                                        mapVal.type = "FINAL";
                                        mapVal.maxAgreedSeq = maxSeq;
                                        mapVal.failedAVD = failedRemortPort;
                                        dataOutC1.writeObject(mapVal);
                                        dataOutC1.flush();
                                        map_avd.remove(currentAVD);
                                        map_avd.put(currentAVD, mapVal);
                                    } else {
                                        Log.i(TAG, "No proposed sequences received yet");
                                    }
                                } else {
                                    Log.i(TAG, "Entry not in map_avd");
                                }
                            } else {
                                Log.i(TAG, "map_avd empty");
                            }

                            try {
                                ack = (Message) dataIntoC1.readObject();
                            } catch (ClassNotFoundException e) {
                                Log.e(TAG, "ClientTask: ClassNotFoundException");
                                e.printStackTrace();
                            }

                            if(ack.getMsgType() == 4) {
                                dataOutC1.close();
                                dataIntoC1.close();
                                socket1.close();
                            }
                        }
                    }
                } catch (SocketTimeoutException e) {
                    Log.e(TAG, "ClientTask: SocketTimeoutException");
                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask: UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask: IOException");
                    failedRemortPort = currentAVD;
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}
