import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;

import java.util.Random;
import java.util.Scanner;

class Message {
	public volatile String messageText = "";
	public volatile String messageType = "";
	public volatile int pid;
	public volatile int hopCount;
	public volatile String direction = "";

	public Message(String s, String t, int p, int h, String d) {
		messageText = s;
		messageType = t;
		pid = p;
		hopCount = h;
		direction = d;
	}

	public Message(String s) {
		Message m;

		String[] split = s.split("[~]");
		if (split[0] != null) {
			messageText = split[0];
		}
		if (split.length > 1 && split[1] != null) {
			messageType = split[1];
		}
		if (split.length > 2 && split[2] != null) {
			pid = Integer.parseInt(split[2]);
		}
		if (split.length > 3 && split[3] != null) {
			hopCount = Integer.parseInt(split[3]);
		}
		if (split.length > 4 && split[4] != null) {
			direction = split[4];
		}
	}

	public String toString() {
		String line = "";
		line += messageText + "~" + messageType + "~" + pid + "~" + hopCount + "~" + direction;
		return line;
	}

	public String getMessageText() {
		return messageText;
	}

	public String getMessageType() {
		return messageType;
	}

	public int getPID() {
		return pid;
	}

	public String getDirection() {
		return direction;
	}

	public int getHopCount() {
		return hopCount;
	}

}

class Channel {
	public volatile String message = "";

	public void putMessage(String s) {
		message = s;
	}

	public String getMessage() {
		return message;
	}

	public boolean hasMessage() {
		return (message.equals("")) ? false : true;
	}

	public void flush() {
		message = "";
	}
}

class Process implements Runnable {
	public static ArrayList<Process> list = new ArrayList<Process>();
	public volatile static ArrayList<Channel> fromMaster = new ArrayList<Channel>();
	public volatile static ArrayList<Channel> toMaster = new ArrayList<Channel>();
	public volatile static ArrayList<ArrayList<Channel>> processChannels = new ArrayList<ArrayList<Channel>>();
	public volatile boolean leaderKnown = false;

	public static int n;
	public int index;
	public int currentMaxPID;
	public boolean isLeader = true;
	public int currentPhase = 0;
	public int phase = -1;
	public ArrayList<Message> sendQueue = new ArrayList<>();

	@Override
	public void run() {
		// TODO Auto-generated method stub
		if (type.equals("master")) {// master starts the rounds
			while (!isDone()) {
				// initiate rounds
				for (int i = 0; i < n; i++) {
					Message m = new Message("START");
					fromMaster.get(i).putMessage(m.toString());

				}
				while (!isRoundDone()) {

				}
			}
			System.exit(0);

		} else {
			// Participant in HS Algorithm
			while (true) {
				Channel master = fromMaster.get(index);
				while (!master.hasMessage()) {

				}
				String text = master.getMessage();
				master.flush();
				Message message = new Message(text);
				if (message.getMessageText().equals("START")) {
					round();
				}
			}
		}

	}

	public boolean isDone() {
		// returns true when all threads in the ring have identified the leader
		for (int i = 0; i < n; i++) {
			if (!Process.list.get(i).leaderKnown) {
				return false;
			}
		}
		return true;
	}

	public void sendMessages() {

		while (!sendQueue.isEmpty()) {
			Message m = sendQueue.remove(0);
			sendMessage(m);
		}
	}

	public void receiveMessages() {
		// check both left and right channels
		Channel channelLeft = processChannels.get(index).get(0);
		Channel channelRight = processChannels.get(index).get(1);
		while (!channelLeft.hasMessage() || !channelRight.hasMessage()) {
		}

		// most of HS algorithm handling is done here
		String sLeft = channelLeft.getMessage();
		Message left = new Message(sLeft);
		channelLeft.flush();
		boolean bCapturedLeft = false;
		boolean bSendingLeft = false;
		boolean bSendingRight = false;

		if (!leaderKnown && left.getMessageText().equals("LEADER")) {
			leaderKnown = true;
			Message r = new Message("LEADER", "*", -1, -1, "RIGHT");
			sendQueue.add(r);
			bSendingRight = true;
			System.out.println(id + "> Leader is " + currentMaxPID);
		}

		if (left.getMessageType().equals("OUT")) {

			if (left.getPID() == id) {

				if (!leaderKnown) {
					System.out.println(id + "> I am the leader!");

					leaderKnown = true;
					Message l = new Message("LEADER", "*", -1, -1, "LEFT");
					Message r = new Message("LEADER", "*", -1, -1, "RIGHT");
					sendQueue.add(l);
					sendQueue.add(r);
					bSendingLeft = true;
					bSendingRight = true;
				}

				// System.exit(0);
			}

			if (left.getPID() > id) {
				currentMaxPID = left.getPID();
				isLeader = false;

				if (left.getHopCount() - 1 == 0) {
					// Turn it around
					if (!leaderKnown) {
						Message m = new Message("HS", "IN", left.getPID(), -1, "LEFT");
						sendQueue.add(m);
						bSendingLeft = true;
					}
				} else {
					if (!leaderKnown) {
						Message m = new Message("HS", "OUT", left.getPID(), left.getHopCount() - 1, "RIGHT");
						sendQueue.add(m);
						bSendingRight = true;
					}
				}
			}

		}

		if (left.getMessageType().equals("IN")) {

			if (left.getPID() == id) {
				bCapturedLeft = true;
			} else {
				Message r = new Message("HS", "IN", left.getPID(), -1, "RIGHT");
				sendQueue.add(r);
				bSendingRight = true;
			}
		}

		String sRight = channelRight.getMessage();
		Message right = new Message(sRight);
		channelRight.flush();

		if (!leaderKnown && right.getMessageText().equals("LEADER")) {

			leaderKnown = true;
			Message l = new Message("LEADER", "*", -1, -1, "LEFT");
			sendQueue.add(l);
			bSendingLeft = true;
			System.out.println(id + "> Leader is " + currentMaxPID);
		}

		if (right.getMessageType().equals("OUT")) {
			if (right.getPID() == id) {
				if (!leaderKnown) {
					System.out.println(id + "> I am the leader!");
					leaderKnown = true;
					Message l = new Message("LEADER", "*", -1, -1, "LEFT");
					Message r = new Message("LEADER", "*", -1, -1, "RIGHT");
					sendQueue.add(l);
					sendQueue.add(r);
					bSendingLeft = true;
					bSendingRight = true;
				}

			}
			if (right.getPID() > id) {
				currentMaxPID = right.getPID();
				isLeader = false;

				if (right.getHopCount() - 1 == 0) {
					// Turn it around
					if (!leaderKnown) {
						Message m = new Message("HS", "IN", right.getPID(), -1, "RIGHT");
						sendQueue.add(m);
						bSendingRight = true;
					}
				} else {
					if (!leaderKnown) {
						Message m = new Message("HS", "OUT", right.getPID(), right.getHopCount() - 1, "LEFT");
						sendQueue.add(m);
						bSendingLeft = true;
					}
				}
			}

		}
		if (right.getMessageType().equals("IN")) {

			if (right.getPID() == id && bCapturedLeft) {
				phase++;
				Message l = new Message("HS", "OUT", id, (int) Math.pow(2, phase), "LEFT");
				Message r = new Message("HS", "OUT", id, (int) Math.pow(2, phase), "RIGHT");
				sendQueue.add(l);
				sendQueue.add(r);
				bSendingLeft = true;
				bSendingRight = true;
			} else {
				if (right.getPID() != id && !leaderKnown) {
					Message r = new Message("HS", "IN", right.getPID(), -1, "LEFT");
					bSendingLeft = true;
					sendQueue.add(r);
				}
			}
		}

		Message dummyLeft = new Message("DUMMY", "DUMMY", -1, -1, "LEFT");
		Message dummyRight = new Message("DUMMY", "DUMMY", -1, -1, "RIGHT");
		if (!bSendingLeft) {
			sendQueue.add(dummyLeft);
		}
		if (!bSendingRight) {
			sendQueue.add(dummyRight);
		}

	}

	public void round() {

		if (phase == -1) {
			// every process enters phase 0
			phase++;
			Message left = new Message("HS", "OUT", currentMaxPID, (int) Math.pow(2, phase), "LEFT");
			Message right = new Message("HS", "OUT", currentMaxPID, (int) Math.pow(2, phase), "RIGHT");
			sendQueue.add(left);
			sendQueue.add(right);

		}

		sendMessages();

		receiveMessages();

		Channel master = toMaster.get(index);
		Message m = new Message("DONE");
		master.putMessage(m.toString());
	}

	public boolean isRoundDone() {

		for (int i = 0; i < n; i++) {
			Channel c = toMaster.get(i);
			String sText = c.getMessage();
			Message message = new Message(sText);

			if (!message.getMessageText().equals("DONE")) {
				return false;
			}

		}
		for (int i = 0; i < n; i++) {
			toMaster.get(i).flush();
		}
		return true;

	}

	public static void logChannels() {
		for (int i = 0; i < n; i++) {
			System.out.println(i + ">Left: " + processChannels.get(i).get(0).message + ",Right: "
					+ processChannels.get(i).get(1).message);
		}
	}

	public void sendMessage(Message m) {
		String direction = m.getDirection();

		int left;
		if (index == 0) {
			left = n - 1;
		} else {
			left = index - 1;
		}
		int neighbor = (direction.equals("LEFT")) ? left : (index + 1) % n;
		int target = (direction.equals("LEFT")) ? 1 : 0;
		Channel targetChannel = processChannels.get(neighbor).get(target);
		/*
		 * while(targetChannel.hasMessage()){ System.out.println("STUCK123"); }
		 */
		if (m.toString().startsWith("DUMMY")) {
		}
		targetChannel.putMessage(m.toString());
	}

	public Process(int id, int index) {
		this.id = id;
		this.currentMaxPID = id;
		this.index = index;
		leaderKnown = false;
	}

	public static void spawnThreads(int[] id) {
		ids = id;
		// Random random = new Random();
		// HashSet<Integer> set = new HashSet<>();
		int max = Integer.MIN_VALUE;
		for (int i = 0; i < n; i++) {
			// ids[i] = random.nextInt(Integer.MAX_VALUE);
			// while (set.contains(ids[i])) {
			// ids[i] = random.nextInt(Integer.MAX_VALUE);
			// }
			if (max < ids[i]) {
				max = ids[i];
			}
			// set.add(ids[i]);

			Channel inboundMaster = new Channel();
			Channel outboundMaster = new Channel();
			Channel inboundLeft = new Channel();
			Channel inboundRight = new Channel();
			toMaster.add(inboundMaster);
			fromMaster.add(outboundMaster);
			ArrayList<Channel> processChannel = new ArrayList<Channel>();
			processChannel.add(inboundLeft);
			processChannel.add(inboundRight);
			processChannels.add(processChannel);
			Process p = new Process(ids[i], i);
			p.type = "slave";
			list.add(p);
			Thread t = new Thread(p);
			t.start();
		}

		System.out.println("Max Id: " + max);

	}

	public static int[] ids;
	public static String[] messagesFromMaster;
	public static volatile String[] messagesToMaster;
	public String type;
	public int id;
	public volatile boolean roundDone = false;

}

public class HS {

	public static int n = 0;
	public static int id[];

	public static void readFile(String path) throws FileNotFoundException {
		Scanner s = new Scanner(new File(path));
		n = s.nextInt();
		id = new int[n];
		int i = 0;
		while (s.hasNext()) {
			id[i++] = s.nextInt();
		}
		s.close();
	}

	public static void main(String args[]) {

		try {
			readFile(args[0]);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Process.n = n;
		Process.spawnThreads(id);
		Process master = new Process(-1, -1);
		master.type = "master";
		Thread masterThread = new Thread(master);
		masterThread.start();

	}

}
