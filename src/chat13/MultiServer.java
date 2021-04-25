package chat13;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import chat11.jdbc.IConnectImpl;

public class MultiServer extends IConnectImpl {
	static ServerSocket serverSocket = null;
	static Socket socket = null;
	
	Map<String, PrintWriter> clientMap;
	Map<RoomList, Integer> roomMap;
	Map<String, RoomList> roomMaster;
	Map<String, RoomList> inviteRoom;
	Map<String, String> entered;
	Map<String, String> fixto;
	Map<String, String> block;
	Map<String, Integer> player;
	Map<String, char[][]> ticTacToe;
	Set<String> black;
	Set<String> pWord;
	RoomList room;
	
	public MultiServer() {
		super("kosmo","1234");
		clientMap = new HashMap<String, PrintWriter>();
		roomMap = new HashMap<RoomList, Integer>();
		roomMaster = new HashMap<String, RoomList>();
		inviteRoom = new HashMap<String, RoomList>();
		entered = new HashMap<String, String>();
		fixto = new HashMap<String, String>();
		block = new HashMap<String, String>();
		player = new HashMap<String, Integer>();
		ticTacToe = new HashMap<String, char[][]>();
		Collections.synchronizedMap(clientMap);
		Collections.synchronizedMap(roomMap);
		Collections.synchronizedMap(roomMaster);
		Collections.synchronizedMap(inviteRoom);
		Collections.synchronizedMap(entered);
		Collections.synchronizedMap(fixto);
		Collections.synchronizedMap(block);
		Collections.synchronizedMap(player);
		Collections.synchronizedMap(ticTacToe);
		
		black = new HashSet<String>();
		black.add("kosmo");
		black.add("기모찌맨");
		pWord = new HashSet<String>();
		pWord.add("바보");
		pWord.add("멍청이");
		pWord.add("기모찌맨");
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
				if(entered.get(clientName).equals(room)) {
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
			} 
			catch (NullPointerException e) {
			}
			catch (Exception e) {
				System.out.println(e.getMessage());
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
		public void gameStart(String chatroom, String flag) {
			int x=1;
			int y=1;
			int a=1;
			int b=1;
			char[][] game = ticTacToe.get(chatroom);
			int count =0;
			while(true) {
				sendAllMsg("","","좌표입력[x(1~3),y(1~3)]","All",chatroom);
				String pla1;
				try {
					pla1 = in.readLine();
					if(flag.equals("player1")) {
						x=pla1.charAt(0)-'0';
						y=pla1.charAt(2)-'0';
					}
					else {
						a=pla1.charAt(0)-'0';
						b=pla1.charAt(2)-'0';
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				game[x-1][y-1]='O';
				game[a-1][b-1]='X';
				count++;
				for(int i=0 ; i<game.length ; i++) {
					for(int j =0 ; j<game[i].length ; j++) {
						sendAllMsg("",""," │ "+game[i][j]+" │ ","All",chatroom);
					}
					if(i!=2)
						sendAllMsg("","","───┼───┼───","All",chatroom);
				}
				
				if(game[0][0]=='O'&&game[1][1]=='O'&&game[2][2]=='O') {
					sendAllMsg("","","player1 승리","All",chatroom);
					break;
				}
				else if(game[0][2]=='O'&&game[1][1]=='O'&&game[2][0]=='O') {
					sendAllMsg("","","player1 승리","All",chatroom);
					break;
				}
				else if(game[0][0]=='O'&&game[0][1]=='O'&&game[0][2]=='O') {
					sendAllMsg("","","player1 승리","All",chatroom);
					break;
				}
				else if(game[1][0]=='O'&&game[1][1]=='O'&&game[1][2]=='O') {
					sendAllMsg("","","player1 승리","All",chatroom);
					break;
				}
				else if(game[2][0]=='O'&&game[2][1]=='O'&&game[2][2]=='O') {
					sendAllMsg("","","player1 승리","All",chatroom);
					break;
				}
				else if(game[0][0]=='O'&&game[1][0]=='O'&&game[2][0]=='O') {
					sendAllMsg("","","player1 승리","All",chatroom);
					break;
				}
				else if(game[0][1]=='O'&&game[1][1]=='O'&&game[2][1]=='O') {
					sendAllMsg("","","player1 승리","All",chatroom);
					break;
				}
				else if(game[0][2]=='O'&&game[1][2]=='O'&&game[2][2]=='O') {
					sendAllMsg("","","player1 승리","All",chatroom);
					break;
				}
				else if(game[0][0]=='X'&&game[1][1]=='X'&&game[2][2]=='X') {
					sendAllMsg("","","player2 승리","All",chatroom);
					break;
				}
				else if(game[0][2]=='X'&&game[1][1]=='X'&&game[2][0]=='X') {
					sendAllMsg("","","player2 승리","All",chatroom);
					break;
				}
				else if(game[0][0]=='X'&&game[0][1]=='X'&&game[0][2]=='X') {
					sendAllMsg("","","player2 승리","All",chatroom);
					break;
				}
				else if(game[1][0]=='X'&&game[1][1]=='X'&&game[1][2]=='X') {
					sendAllMsg("","","player2 승리","All",chatroom);
					break;
				}
				else if(game[2][0]=='X'&&game[2][1]=='X'&&game[2][2]=='X') {
					sendAllMsg("","","player2 승리","All",chatroom);
					break;
				}
				else if(game[0][0]=='X'&&game[1][0]=='X'&&game[2][0]=='X') {
					sendAllMsg("","","player2 승리","All",chatroom);
					break;
				}
				else if(game[0][1]=='X'&&game[1][1]=='X'&&game[2][1]=='X') {
					sendAllMsg("","","player2 승리","All",chatroom);
					break;
				}
				else if(game[0][2]=='X'&&game[1][2]=='X'&&game[2][2]=='X') {
					sendAllMsg("","","player2 승리","All",chatroom);
					break;
				}
				else if(count==9) {
					sendAllMsg("","","무승부입니다.","All",chatroom);
					break;
				}
			}
		}
		public void masterOut(String mName,String mRoom) {
			out.println("채팅방을 종료합니다.");
			sendAllMsg("","",mName+"님이 채팅방을 닫으셨습니다.","All",mRoom);
			System.out.println(mName+"님의 채팅방이 종료되었습니다.");
			
			Set<String> clear = entered.keySet();
			Set<String> clearlist = new HashSet<String>();
			for(String a : clear) {
				if(entered.get(a).equals(mRoom)) {
					clearlist.add(a);
				}
			}
			for(String a : clearlist) {
				entered.remove(a);
			}
			roomMap.remove(roomMaster.get(mName));
			roomMaster.remove(mRoom);
			
		}
		public void roomChat(String name, String s, String chatroom) {
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
			try {
				if(player.containsKey(name)) {
					gameStart(chatroom,"player2");
					player.remove(name);
				}
				else if(s.charAt(0)=='/') {
					if(strArr[0].equalsIgnoreCase("/game")) {
						ticTacToe.put(chatroom, new char[3][3]);
						player.put(name,0);
						player.put(strArr[1],0);
						gameStart(chatroom,"player1");
					}
					else if(strArr[0].equalsIgnoreCase("/invite")) {
						if(!entered.containsKey(strArr[1])) {
							Set<RoomList> keys = roomMap.keySet();
							for(RoomList key : keys) {
								if(key.roomName.equals(chatroom)) {
									int member = roomMap.get(key);
									if(member>=1) {
										inviteRoom.put(strArr[1], key);
										out.println("초대메세지를 보냈습니다.");
										System.out.println(name+"님 대화초대 ->"+strArr[1]);
									}
									else {
										out.println("채팅방이 꽉찼습니다.");
									}
								}
							}
						}
						else {
							out.println("대상을 초대할 수 없습니다.");
						}
					}
					else if(strArr[0].equalsIgnoreCase("/boom")) {
						if(roomMaster.get(name).roomName.equals(chatroom)) {
							masterOut(name,chatroom);
						}
					}
					else if(strArr[0].equalsIgnoreCase("/redcard")) {
						if(roomMaster.get(name).roomName.equals(chatroom)) {
							Iterator<RoomList> outroom = roomMap.keySet().iterator();
							while(outroom.hasNext()) {
								RoomList a = (RoomList)outroom.next();
								if(a.roomName.equals(chatroom)){
									roomMap.replace(a, roomMap.get(a), roomMap.get(a)+1);
									sendAllMsg("","",strArr[1]+"님이 퇴장당하셨습니다.","All",chatroom);
									entered.remove(strArr[1]);
									System.out.println("["+chatroom+"에서 "+strArr[1]+"님 강제퇴장]");
								}
							}
						}
					}
					else if(strArr[0].equalsIgnoreCase("/roomout")) {
						if(roomMaster.containsKey(name)){
							masterOut(name,chatroom);
						}
						else {
							Iterator<RoomList> outroom = roomMap.keySet().iterator();
							while(outroom.hasNext()) {
								RoomList a = (RoomList)outroom.next();
								if(a.roomName.equals(chatroom)){
									roomMap.replace(a, roomMap.get(a), roomMap.get(a)+1);
									sendAllMsg("","",name+"님이 채팅방에서 나가셨습니다.","All",chatroom);
									entered.remove(name);
								}
							}
						}
					}
					else if(strArr[0].equalsIgnoreCase("/myroomlist")) {
						Set<String> keys = entered.keySet();
						System.out.println("["+name+"님 접속자리스트 요청");
						out.println("=접속자리스트=");
						int count=1;
						for(String key : keys) {
							String value = entered.get(key);
							if(value.equalsIgnoreCase(chatroom)) {
								out.println(count++ +"."+key);
							}
						}
					}
					else if(strArr[0].equalsIgnoreCase("/to")) {
						msgContent="";
						for(int i=2 ; i<strArr.length;i++) {
							for(String a : pWord) {
								if(strArr[i].equals(a)) {
									strArr[i]="나쁜말";
								}
							}
							msgContent += strArr[i]+" ";
						}
						sendAllMsg(name,strArr[1], msgContent, "One",chatroom);
					}
					else if(strArr[0].equalsIgnoreCase("/fixto")) {
						if(entered.containsKey(strArr[1])) {
							fixto.put(name, strArr[1]);
							out.println(strArr[1]+"님에게 귓속말 고정");
							System.out.println("["+name+" 님 "+strArr[1]+" 님에 귓속말 고정]");
						}
						else {
							out.println("유효한 사용자가 아닙니다.");
						}
					} 
					else if(strArr[0].equalsIgnoreCase("/unfixto")) {
						if(fixto.get(name).equals(strArr[1])) {
							fixto.remove(name);
							out.println("고정귓속말 해제");
							System.out.println("["+name+" 님 "+strArr[1]+" 님에 귓속말 해제]");
						}
						else {
							out.println("대상이 잘못되었습니다.");
						}
					}
					else if(strArr[0].equalsIgnoreCase("/block")) {
						if(entered.containsKey(strArr[1])) {
							block.put(name,strArr[1]);
							out.println(strArr[1]+"님의 메세지 차단");
							System.out.println("["+name+" 님 "+strArr[1]+"님 차단 설정]");
						}
						else {
							out.println("유효한 사용자가 아닙니다.");
						}
					}
					else if(strArr[0].equalsIgnoreCase("/unblock")) {
						if(block.get(name).equalsIgnoreCase(strArr[1])) {
							block.remove(name);
							out.println(strArr[1]+"님 차단 해제");
							System.out.println("["+name+" 님 "+strArr[1]+"님 차단 해제]");
						}
						else {
							out.println("차단된 사용자가 아닙니다.");
						}
					}
				}
				else {
					if(fixto.containsKey(name)) {
						sendAllMsg(name,fixto.get(name), msgContent, "One",chatroom);
					}
					else {
						sendAllMsg("",name, msgContent, "All",chatroom);
					}
				}
			}
			catch (NullPointerException e) {
				out.println("잘못된 요청입니다.");
			} 
			catch (Exception e) {
				System.out.println("chatroom에러"+e);
				e.printStackTrace();
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
				if(black.contains(name)) {
					out.println("당신은 블랙리스트입니다.");
					System.out.println("[블랙리스트 접속차단:"+name+"]");
					this.interrupt();
					return;
				}
				//아이디 중복검사도 내비두고
				if(clientMap.containsKey(name)) {
					name=name+"temp";
					out.println("중복된 아이디입니다.");
					System.out.println("[중복아이디 차단:"+name+"]");
					this.interrupt();
					return;
				}
			
				//접속자수도 내비두고
				if(clientMap.size()>10) {
					out.println("접속자수가 초과되었습니다");
					System.out.println("[초과접속자 차단]");
					this.interrupt();
					return;
				}
				clientMap.put(name, out);
				out.println("명령어 확인은 /?를 입력하세요");
				System.out.println("["+name+" 접속]");
				System.out.println("[현재 접속자수"+clientMap.size()+"명]");
				while(in!=null) {
					s = in.readLine();
					s = URLDecoder.decode(s,"UTF-8");
					String[] strArr = s.split(" ");
					if(inviteRoom.containsKey(name)) {
						RoomList a = inviteRoom.get(name);
						if(strArr[0].equalsIgnoreCase("y")) {
							roomMap.replace(a, roomMap.get(a), roomMap.get(a)-1);
							entered.put(name, a.roomName);
							sendAllMsg("","",name+"님이 입장하셨습니다.","All",a.roomName);
							inviteRoom.remove(name);
						}
						else if(strArr[0].equalsIgnoreCase("n")) {
							out.println("초대를 거절하셨습니다.");
							inviteRoom.remove(name);
						}
						else {
							out.println(a.roomName+"채팅방에 초대되셨습니다."+
									"\n수락(Y)/거절(N) 입력:");
						}
					}
					else if(entered.containsKey(name)) {
						roomChat(name,s,entered.get(name));
					}
					else if(strArr[0].equals("/?")) {
						out.println("1.채팅방개설 : /makeroom\n"
								+ "2.채팅방입장 : /roomenter\n"
								+ "3.채팅방목록 : /chatlist");
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
							if(strArr.length==4) {
								room = new RoomList
										(strArr[1],strArr[2],Integer.parseInt(strArr[3]));
								roomMap.put(room, Integer.parseInt(strArr[3])-1);//개설자 바로 들어가짐
								roomMaster.put(name,room);//키값은 방장이름, 벨류는 방객체
								entered.put(name, strArr[1]);//개설자 방 입장 map저장
								sendAllMsg("","",name+"님이 입장","All",strArr[1]);
								System.out.println(strArr[1]+" 채팅방이 개설되었습니다.");
							}
							else if(strArr.length==3) {
								room = new RoomList
										(strArr[1],Integer.parseInt(strArr[2]));
								roomMap.put(room, Integer.parseInt(strArr[2])-1);//개설자 바로 들어가짐
								roomMaster.put(name,room);//키값은 방장이름, 벨류는 방객체
								entered.put(name, strArr[1]);//개설자 방 입장 map저장
								sendAllMsg("","",name+"님이 입장","All",strArr[1]);
								System.out.println(strArr[1]+" 채팅방이 개설되었습니다.");
							}
							else {
								out.println("/makeroom 방이름 비밀번호(비공개방) 입장인원 을 입력해주세요");
							}
						}
						else {
							out.println("채팅방이 이미 존재합니다.");
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
										sendAllMsg("","",name+"님이 입장하셨습니다.","All",strArr[1]);
									}
									else if(a.passWord.equalsIgnoreCase(strArr[2])){
										roomMap.replace(a, roomMap.get(a), roomMap.get(a)-1);
										entered.put(name, strArr[1]);
										sendAllMsg("","",name+"님이 입장하셨습니다.","All",strArr[1]);
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
					else if(strArr[0].equalsIgnoreCase("/chatlist")) {
						Set<RoomList> keys = roomMap.keySet();
						System.out.println("["+name+"님이 채팅방리스트 요청]");
						out.println("=채팅방리스트=");
						int count=1;
						for(RoomList key : keys) {
							int max = key.maxEntered;
							int member = roomMap.get(key);
							String roomName = key.roomName;
							String pass = key.passWord;
							if(pass.equals("")) {
								out.println(count++ +"."+" [공 개] "+roomName+" ("
										+(max-member)+"/"+max+")");
							}
							else {
								out.println(count++ +"."+" [비공개] "+roomName+" ("
										+(max-member)+"/"+max+")");
								
							}
						}
					}
				}
			}
			catch (NullPointerException e) {}
			catch (Exception e) {}
			finally {
				if(roomMaster.containsKey(name)) {
					masterOut(name,roomMaster.get(name).roomName);
				}
				if(block.containsKey(name)) {
					block.remove(name);
				}
				if(fixto.containsKey(name)) {
					fixto.remove(name);
				}
				clientMap.remove(name);
				sendAllMsg("","",name+"님이 퇴장하셨습니다.","All",entered.get(name));
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
