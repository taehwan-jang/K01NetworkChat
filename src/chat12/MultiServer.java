package chat12;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import chat11.jdbc.IConnectImpl;

public class MultiServer extends IConnectImpl {
	static ServerSocket serverSocket = null;
	static Socket socket = null;
	
	Map<String, PrintWriter> clientMap;
	Set<String> black;
	Set<String> pWord;
	String fixName ="";
	String toName ="";
	String blockName ="";
	String btoName ="";
	
	public MultiServer() {
		super("kosmo","1234");
		clientMap = new HashMap<String, PrintWriter>();
		black = new HashSet<String>();
		black.add("kosmo");
		pWord = new HashSet<String>();
		pWord.add("개새끼");
		pWord.add("개새꺄");
		pWord.add("병신");
		pWord.add("꺼져");
		Collections.synchronizedMap(clientMap);
	} 
	public void init() {
		try {
			serverSocket = new ServerSocket(9999);
			System.out.println("서버가 시작되었습니다.");
			
			while(true) {
				socket = serverSocket.accept();
				Thread mst = new MultiServerT(socket);
				mst.start();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				serverSocket.close();
				
			} 
			catch (Exception e2) {
				e2.printStackTrace();
			}
		}
	}
	public static void main(String[] args) {
		MultiServer ms = new MultiServer();
		ms.init();
	}
	public void sendAllMsg(String sendName, String name, String msg, String flag) {
		Iterator<String> it = clientMap.keySet().iterator();
		if(flag.equals("All")) {
			try {
				String query = "INSERT INTO chat_talking VALUES(SEQ_TALKING.nextval,?,default,?,SYSDATE)";
				psmt = con.prepareStatement(query);
				psmt.setString(1, name);
				psmt.setString(2, URLDecoder.decode(msg,"UTF-8"));
				
				psmt.executeUpdate();
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		while(it.hasNext()) {
			try {
				String clientName = it.next();
				PrintWriter it_out =(PrintWriter)
						clientMap.get(clientName);
				if(!(blockName.equals(name)&&btoName.equals(clientName)||
						blockName.equals(clientName)&&btoName.equals(name))) {
					if(flag.equals("One")) {
						if(name.equals(clientName)) {
							it_out.println("[귓속말]"+sendName+":"+msg);
							try {
								String query = "INSERT INTO chat_talking VALUES(SEQ_TALKING.nextval,?,?,?,SYSDATE)";
								psmt = con.prepareStatement(query);
								psmt.setString(1, sendName);
								psmt.setString(2, name);
								psmt.setString(3, URLDecoder.decode(msg,"UTF-8"));
								
								psmt.executeUpdate();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
					else {
						if(name.equals("")) {
							it_out.println(URLEncoder.encode(msg,"UTF-8"));
						} 
						else {
							it_out.println("["+name+"]"+URLEncoder.encode(msg,"UTF-8"));
						}
					}
				}
			} catch (Exception e) {
				System.out.println("예외:"+e);
			}
		}
	}
	class MultiServerT extends Thread{
		
		Socket socket;
		PrintWriter out = null;
		BufferedReader in = null;
		public MultiServerT(Socket socket) {
			this.socket=socket;
			try {
				out = new PrintWriter(this.socket.getOutputStream(),true);
				in = new BufferedReader(new InputStreamReader(this.socket.getInputStream(),"UTF-8"));
				
			} 
			catch (Exception e) {
				System.out.println("예외:"+e);
			}
		}
		@Override
		public void run() {

			String name = "";
			String s = "";
			
			try {
				name = in.readLine();
				name = URLDecoder.decode(name,"UTF-8");
				
				Iterator<String> it2 = black.iterator();
				while(it2.hasNext()) {
					String name2 = it2.next();
					if(name2.equals(name)) {
						out.println("당신은 블랙리스트");
						this.interrupt();
						return;
					}
				}
				Iterator<String> it = clientMap.keySet().iterator();
				while(it.hasNext()) {
					String name2 = it.next();
					if(name2.equals(name)) {
						name=name+"temp";
						this.interrupt();
						return;
					}
				}
				if(clientMap.size()>2) {
					out.println("접속자수초과");
					this.interrupt();
					return;
				}
				sendAllMsg("","",name+"님이 입장","All");
				clientMap.put(name, out);
				System.out.println(name+" 접속");
				System.out.println("현재 접속자수"+clientMap.size()+"명");
				while(in!=null) {
					
					s = in.readLine();
					s = URLDecoder.decode(s,"UTF-8");
					if(fixName.equals(name)) {
						s="/to "+toName+" "+ s;
					}
					if(s==null) {
						break;
					}
					System.out.println(name+">>"+s);
					if(s.charAt(0)=='/') {
						String[] strArr = s.split(" ");
						String msgContent ="";
						for(int i=2 ; i<strArr.length;i++) {
							for(String a : pWord) {
								if(strArr[i].equals(a)) {
									strArr[i]="나쁜말";
								}
							}
							msgContent += strArr[i]+" ";
						}
						if(strArr[0].equalsIgnoreCase("/list")) {
							Iterator<String> list = clientMap.keySet().iterator();
							out.println("=접속자리스트=");
							int count=1;
							while(list.hasNext()) {
								String name2 = list.next();
								out.println(count++ +"."+name2);
							}
						}
						if(strArr[0].equalsIgnoreCase("/to")) {
							if(strArr[2].equals("/unfixto")) {
								fixName = "";
								toName = "";
								out.println("고정귓속말 해제");
							}
							else {
								sendAllMsg(name,strArr[1], msgContent, "One");
							}
						}
						if(strArr[0].equalsIgnoreCase("/fixto")) {
							if(clientMap.containsKey(strArr[1])) {
								fixName = name;
								toName = strArr[1];
								out.println(strArr[1]+"님에게 귓속말 고정");
							}
							else {
								out.println("유효한 사용자가 아닙니다.");
							}
						}
						if(strArr[0].equalsIgnoreCase("/block")) {
							if(clientMap.containsKey(strArr[1])) {
								blockName = strArr[1];
								btoName = name;
								out.println(strArr[1]+"님의 메세지 차단");
							}
							else {
								out.println("유효한 사용자가 아닙니다.");
							}
						}
						if(strArr[0].equalsIgnoreCase("/unblock")) {
							if(strArr[1].equals(blockName)) {
								blockName = "";
								btoName = "";
								out.println(strArr[1]+"님 차단 해제");
							}
							else {
								out.println("차단된 사용자가 아닙니다.");
							}
						}
					}
					else {
						String[] strArr = s.split(" ");
						String msgContent ="";
						for(int i=0 ; i<strArr.length;i++) {
							for(String a : pWord) {
								if(strArr[i].equals(a)) {
									strArr[i]="나쁜말";
								}
							}
							msgContent += strArr[i]+" ";
						}
						sendAllMsg("",name, msgContent, "All");
					}
				}
				
			}
			catch (Exception e) {
				System.out.println("예외:"+e);
			}
			finally {
				clientMap.remove(name);
				sendAllMsg("","",name+"님이 퇴장하심","All");
				System.out.println(name+"["+Thread.currentThread().getName()
						+"] 퇴장");
				System.out.println("현재 접속자수"+clientMap.size()+"명");
				try {
					in.close();
					out.close();
					socket.close();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
		}
	}
}
