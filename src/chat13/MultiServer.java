package chat13;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
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
	Map<RoomList, Integer> roomMap;
	Map<String, String> entered;
	Map<String, String> fixto;
	Map<String, String> block;
	Set<String> black;
	Set<String> pWord;
	RoomList room;
	
	public MultiServer() {
		super("kosmo","1234");
		clientMap = new HashMap<String, PrintWriter>();
		roomMap = new HashMap<RoomList, Integer>();
		entered = new HashMap<String, String>();
		fixto = new HashMap<String, String>();
		block = new HashMap<String, String>();
		Collections.synchronizedMap(clientMap);
		Collections.synchronizedMap(roomMap);
		Collections.synchronizedMap(entered);
		Collections.synchronizedMap(fixto);
		Collections.synchronizedMap(block);
		
		black = new HashSet<String>();
		black.add("kosmo");
		pWord = new HashSet<String>();
		pWord.add("개새끼");
		pWord.add("개새꺄");
		pWord.add("병신");
		pWord.add("꺼져");
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
	
	public void sendAllMsg(String sendName, String name, String msg, String flag, String room) {
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
				if(entered.get(clientName).equalsIgnoreCase(room)) {
					boolean send2 = false;
					boolean send3 = false;
					if(block.containsKey(name)) {
						send2=block.get(name).equals(clientName);
					}
					if(block.containsKey(clientName)) {
						send3=block.get(clientName).equals(name);
					}
					boolean send = !send2&&!send3;
					if(send) {
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
				}
			} catch (Exception e) {}
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
		public void roomChat(String name, String s, String room) {
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
			if(s.charAt(0)=='/') {
				if(strArr[0].equalsIgnoreCase("/roomout")) {
					entered.remove(name);
					sendAllMsg("","",name+"님이 퇴장","All",strArr[1]);
					out.println("채팅방에서 나가셨습니다.");
				}
				else if(strArr[0].equalsIgnoreCase("/myroomlist")) {
					Set<String> keys = entered.keySet();
					out.println("=접속자리스트=");
					int count=1;
					for(String key : keys) {
						String value = entered.get(key);
						if(value.equalsIgnoreCase(room)) {
							out.println(count++ +"."+key);
						}
					}
				}
				else if(strArr[0].equalsIgnoreCase("/to")) {
					sendAllMsg(name,strArr[1], msgContent, "One",room);
				}
				else if(strArr[0].equalsIgnoreCase("/fixto")) {
					if(entered.containsKey(strArr[1])) {
						fixto.put(name, strArr[1]);
						out.println(strArr[1]+"님에게 귓속말 고정");
					}
					else {
						out.println("유효한 사용자가 아닙니다.");
					}
				}
				else if(strArr[0].equalsIgnoreCase("/unfixto")) {
					if(fixto.get(name).equals(strArr[1])) {
						fixto.remove(name);
						out.println("고정귓속말 해제");
					}
					else {
						out.println("대상이 잘못되었습니다.");
					}
				}
				else if(strArr[0].equalsIgnoreCase("/block")) {
					if(entered.containsKey(strArr[1])) {
						block.put(name,strArr[1]);
						out.println(strArr[1]+"님의 메세지 차단");
					}
					else {
						out.println("유효한 사용자가 아닙니다.");
					}
				}
				else if(strArr[0].equalsIgnoreCase("/unblock")) {
					if(block.get(name).equalsIgnoreCase(strArr[1])) {
						block.remove(name);
						out.println(strArr[1]+"님 차단 해제");
					}
					else {
						out.println("차단된 사용자가 아닙니다.");
					}
				}
			}
			else {
				if(fixto.containsKey(name)) {
					sendAllMsg(name,fixto.get(name), msgContent, "One",room);
				}
				else {
					sendAllMsg("",name, msgContent, "All",room);
				}
			}
		}
		@Override
		public void run() {
			String name = "";
			String s = "";
			
			try {
				name = in.readLine();
				name = URLDecoder.decode(name,"UTF-8");
				//블랙리스트는 내비두고
				Iterator<String> it2 = black.iterator();
				while(it2.hasNext()) {
					String name2 = it2.next();
					if(name2.equals(name)) {
						out.println("당신은 블랙리스트");
						this.interrupt();
						return;
					}
				}
				//아이디 중복검사도 내비두고
				Iterator<String> it = clientMap.keySet().iterator();
				while(it.hasNext()) {
					String name2 = it.next();
					if(name2.equals(name)) {
						name=name+"temp";
						this.interrupt();
						return;
					}
				}
				//접속자수도 내비두고
				if(clientMap.size()>2) {
					out.println("접속자수초과");
					this.interrupt();
					return;
				}
				clientMap.put(name, out);
				System.out.println(name+" 접속");
				System.out.println("현재 접속자수"+clientMap.size()+"명");
				while(in!=null) {
					s = in.readLine();
					s = URLDecoder.decode(s,"UTF-8");
					String[] strArr = s.split(" ");
					if(entered.containsKey(name)) {
						roomChat(name,s,entered.get(name));
					}
					else if(strArr[0].equalsIgnoreCase("/makeroom")) {
						//방이름이 중복된 객체는 아닌지 확인해보기
						Iterator<RoomList> is = roomMap.keySet().iterator();
						boolean flag2 = false;
						while(is.hasNext()) {
							RoomList a = (RoomList)is.next();
							if(a.roomName.equalsIgnoreCase(strArr[1])){
								flag2 = true;
							}
						}
						if(!flag2) {
							if(strArr.length>3) {
								room = new RoomList
										(strArr[1],strArr[2],Integer.parseInt(strArr[3]));
								roomMap.put(room, Integer.parseInt(strArr[3])-1);//개설자 바로 들어가짐
								entered.put(name, strArr[1]);//개설자 방 입장 map저장
								sendAllMsg("","",name+"님이 입장","All",strArr[1]);
							}
							else if(strArr.length<4) {
								room = new RoomList
										(strArr[1],Integer.parseInt(strArr[2]));
								roomMap.put(room, Integer.parseInt(strArr[3])-1);//개설자 바로 들어가짐
								entered.put(name, strArr[1]);//개설자 방 입장 map저장
								sendAllMsg("","",name+"님이 입장","All",strArr[1]);
							}
						}
						else {
							System.out.println("채팅방이 이미 존재합니다.");
						}
					}
					else if(strArr[0].equalsIgnoreCase("/roomenter")) {
						Iterator<RoomList> is = roomMap.keySet().iterator();
						boolean findroom = false;
						while(is.hasNext()) {
							RoomList a = (RoomList)is.next();
							if(a.roomName.equals(strArr[1])){
								findroom = true;
								if(roomMap.get(a)>=1) {
									if(a.passWord.equals("")) {
										roomMap.replace(a, roomMap.get(a), roomMap.get(a)-1);
										entered.put(name, strArr[1]);
										sendAllMsg("","",name+"님이 입장","All",strArr[1]);
									}
									else if(a.passWord.equalsIgnoreCase(strArr[2])){
										roomMap.replace(a, roomMap.get(a), roomMap.get(a)-1);
										entered.put(name, strArr[1]);
										sendAllMsg("","",name+"님이 입장","All",strArr[1]);
									}
									else {
										out.println("패스워드가 틀렸습니다.");
									}
								}
								else {
									out.println("인원수가 가득찼습니다.");
								}
							}
						}
						if(!findroom){
							out.println("존재하지 않는 방입니다.");
						}
					}
				}
			}
			catch (NullPointerException e) {
				
			}
			catch (Exception e) {
				System.out.println("예외:"+e);
			}
			finally {
				clientMap.remove(name);
				sendAllMsg("","",name+"님이 퇴장하심","All",entered.get(name));
				entered.remove(name);
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
