package guMeddelandesystem;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class MsgServer extends Thread {
	private Thread thread;
	private int port;
	private ArrayList<String> userReg = new ArrayList<String>();
	private LinkedList<Message> msgBuffer = new LinkedList<Message>();
	private ArrayList<User> users = new ArrayList<User>();
	private ServerSocket serverSocket;
	private Socket socket;
	private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
	private LocalDateTime dateAndTime;
	private final static Logger logger = Logger.getLogger("requests");
	private FileHandler requests;

	public MsgServer(int port) {
		this.port = port;
		try {
			serverSocket = new ServerSocket(port);
			requests = new FileHandler("files/requestLog.log");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void start() {
		if(thread == null) {
			thread = new Thread(this);
			thread.start();
		}
	}
	
	public void run() {
		System.out.println("Server startad");
		try {
			while (true) {
				try {
					socket = serverSocket.accept();
					new ClientHandler(socket);
				} catch (IOException e) {
					e.printStackTrace();
					socket.close();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Server stoppad");
		}
	}
	
	public String dateAndTime() {
		dateAndTime = LocalDateTime.now();
		String date = dateTimeFormatter.format(dateAndTime);
		return date;
	}

//	private class StartServer extends Thread {
//		private int port;
//
//		public StartServer(int port) throws InterruptedException {
//			this.port = port;
//		}
//
//		public void run() {
//			Socket socket = null;
//			System.out.println("Server startad");
//			try {
//				ServerSocket serverSocket = new ServerSocket(port);
//				while (true) {
//					try {
//						socket = serverSocket.accept();
//						new ClientHandler(socket);
//					} catch (IOException e) {
//						e.printStackTrace();
//						socket.close();
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//				}
//			} catch (IOException e) {
//				e.printStackTrace();
//				System.out.println("Server stoppad");
//			}
//		}
//	}
	

	private class ClientHandler extends Thread {
		private Socket socket;
		private ObjectOutputStream oos;
		private ObjectInputStream ois;
		private InetAddress clientAddress;

		public ClientHandler(Socket socket) throws InterruptedException {
			this.socket = socket;
			try {
				this.clientAddress = socket.getInetAddress();
				oos = new ObjectOutputStream(socket.getOutputStream());
				ois = new ObjectInputStream(socket.getInputStream());
				start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void run() {
			Message msg;
			String username = new String();
			Object obj;
			boolean userInReg = false;

			try {
				username = (String) ois.readObject();
				for(int i = 0; i < users.size(); i++) {
					if(username.equals(users.get(i).getUsername())) {
						userInReg = true;
					}
				}
				if(!userInReg) {
					userReg.add(username);
					users.add(new User(username));
					System.out.println("Server: Added user " + username);
				} else {
					System.out.println("Server: Did not add a user");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			while (true) {
				try {	
					obj = ois.readObject();
					if (obj instanceof Message) {
						msg = (Message) obj;
						msg.setTimeRecievedServer(dateAndTime());
						for(int i = 0; i < users.size(); i++) {
							if(msg.getReceivers().contains(users.get(i).getUsername())) {
								users.get(i).addMessage(msg);
							} else {
								msgBuffer.add(msg);
							}
						}
						System.out.println("Server: Message added to buffer");
					}
					if (obj instanceof String) {
						if (obj.equals("getUserReg")) {
							oos.writeUnshared(userReg);
							oos.flush();
							System.out.println("Server: User list sent to client");
						}
						if (obj.equals("getMsgBuffer")) {
							int nbrOfMessages = 0;
							for(int i = 0; i < users.size(); i++) {
								if(username.equals(users.get(i).getUsername())) {
									nbrOfMessages = users.get(i).getMessages().size();
									}
								}
							Message[] messages = new Message[nbrOfMessages];
							for(int i = 0; i < users.size(); i++) {
								if(users.get(i).getUsername().equals(username)) {
									for(int j = 0; j < users.get(i).getMessages().size(); j++) {
										messages[j] = users.get(i).getMessages().remove(j);
									}
								}
							}
								
//							for(int i = 0; i < msgBuffer.size(); i++) {
//								if(msgBuffer.get(i).getReceivers().contains(username)) {
//									nbrOfMessages++;
//								}
//							}
//							Message[] messages = new Message[nbrOfMessages];
//							for(int j = 0; j < messages.length; j++) {
//								for(int i = 0; i < msgBuffer.size(); i++) {
//									if(msgBuffer.get(i).getReceivers().contains(username)) {
//										messages[j] = msgBuffer.get(i);
//									}
//								}
//							}
//							Message[] messagesTemp = new Message[msgBuffer.size()];
//							for(int i = 0; i < messagesTemp.length; i++) {
//								messagesTemp[i] = msgBuffer.get(i);
//							}
//							
//							for (int i = 0; i < messagesTemp.length; i++) {
//								if (!messagesTemp[i].getReceivers().contains(username)) {
//									messagesTemp[i] = null;
//								}
//							}
//
//							int nbrOfMessages = 0;
//							for (int i = 0; i < messagesTemp.length; i++) {
//								if (messagesTemp[i] == null) {
//									if (!(i + 1 > messagesTemp.length)) {
//										messagesTemp[i] = messagesTemp[i + 1];
//										messagesTemp[i + 1] = null;
//									}
//								} else {
//									nbrOfMessages++;
//								}
//							}
//							Message[] messages = new Message[nbrOfMessages];
//							for (int i = 0; i < nbrOfMessages; i++) {
//								messages[i] = messagesTemp[i];
//							}
							oos.writeObject(messages);
							oos.flush();
							System.out.println("Server: List of messages sent");
						}
						if (obj.equals("logOut")) {
							System.out.println("Server: Received logOut");
							oos.writeObject("requestUsername");
							System.out.println("Server: Requests username");
							String removeUser = (String) ois.readObject();
							System.out.println("Server: Received username: " + removeUser);
							for (int i = 0; i < userReg.size(); i++) {
								if (userReg.get(i).equals(removeUser)) {
									userReg.remove(i);
									System.out.println("User " + removeUser + " removed");
								}
							}
						}
					}
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
	}
}