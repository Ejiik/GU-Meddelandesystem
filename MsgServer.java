package guMeddelandesystem;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;

public class MsgServer {
	private int port;
	private ArrayList<String> userReg = new ArrayList<String>();
	private LinkedList<Message> msgBuffer = new LinkedList<Message>();

	public MsgServer(int port) {
		this.port = port;

		try {
			new StartServer(port).start();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private class StartServer extends Thread {
		private int port;

		public StartServer(int port) throws InterruptedException {
			this.port = port;
		}

		public void run() {
			Socket socket = null;

			System.out.println("Server startad");
			try {
				ServerSocket serverSocket = new ServerSocket(port);

				while (true) {
					try {
						socket = serverSocket.accept();

						new ClientHandler(socket);

					} catch (IOException e) {
						e.printStackTrace();
						socket.close();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Server stoppad");
		}

	}

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
			Message msg = null;
			String username = null;
			Object obj;
//			String[] tempUserReg = userReg.toArray();
			
			try{
				obj = ois.readUTF();
				for(int i=0; i<userReg.size(); i++){
					if(obj == userReg.get(i)){
						username = (String)obj;
						break;
					}else{
						if(i == userReg.size()-1){
							username = (String)obj;
							userReg.add(username);
							break;
						}
					}
						
				}
			} catch(Exception e){
				e.printStackTrace();
			}
			
			while (true) {
				try {
					
					obj = ois.readUTF();
					
					
					if (obj instanceof Message) {
						msg = (Message)obj;
					
						msgBuffer.add(msg);
						
					}
					

					if (obj instanceof String) {
						
						if(obj.equals("getUserReg")) {
							oos.writeObject(userReg);
						}
						
						if (obj.equals("getMsgBuffer")) {
							Message[] messagesTemp = new Message[msgBuffer.size()];
		
							messagesTemp = (Message[]) msgBuffer.toArray();

							for (int i = 0; i < messagesTemp.length; i++) {
								if (!messagesTemp[i].getReceivers().contains(username)) {
									messagesTemp[i] = null;

								}
							}
							
							int nbrOfMessages = 0;
							
							for (int i = 0; i < messagesTemp.length; i++) {

								if (messagesTemp[i] == null) {
									if (!(i + 1 > messagesTemp.length)) {
										messagesTemp[i] = messagesTemp[i + 1];
										messagesTemp[i+1] = null;
									}
								} else{
									nbrOfMessages++;
								}
							}
							Message[] messages = new Message[nbrOfMessages];
							
							for(int i = 0; i<nbrOfMessages; i++){
								messages[i] = messagesTemp[i];
							}
							oos.writeObject(messages);
							
						}
						//Tar bort en användare ur userReg som loggar ut i sin klient.
						if (obj.equals("logOut")) {
							oos.writeUTF("requestUsername");
							String removeUser = ois.readUTF();
							for(int i = 0; i < userReg.size(); i++) {
								if(userReg.get(i).equals(removeUser)) {
									userReg.remove(i);
								}
							}
						}
					}

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void main(String[] args) {
		new MsgServer(3500);
	}

}