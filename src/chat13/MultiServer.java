package chat13;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.SQLIntegrityConstraintViolationException;
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
	
	Map<String, PrintWriter> clientMap;//이름과 printwriter 객체 저장
	Map<RoomList, Integer> roomMap;//채팅방과 인원수 저장
	Map<String, RoomList> roomMaster;//방장과 방객체 저장
	Map<String, RoomList> inviteRoom;//초대받은 경우 대상과 방객체 저장
	Map<String, String> entered;//방에 입장한 경우 이름과 방이름 저장
	Map<String, String> fixto;//귓속말 고정시 이름과 대상이름 저장
	Map<String, String> block;//블럭 
	Set<String> black;//블랙리스트
	Set<String> pWord;//금칙어
	RoomList room;//방객체
	
	public MultiServer() {
		super("kosmo","1234");
		clientMap = new HashMap<String, PrintWriter>();
		roomMap = new HashMap<RoomList, Integer>();
		roomMaster = new HashMap<String, RoomList>();
		inviteRoom = new HashMap<String, RoomList>();
		entered = new HashMap<String, String>();
		fixto = new HashMap<String, String>();
		block = new HashMap<String, String>();
		Collections.synchronizedMap(clientMap);
		Collections.synchronizedMap(roomMap);
		Collections.synchronizedMap(roomMaster);
		Collections.synchronizedMap(inviteRoom);
		Collections.synchronizedMap(entered);
		Collections.synchronizedMap(fixto);
		Collections.synchronizedMap(block);
		
		black = new HashSet<String>();
		black.add("kosmo");//블랙리스트
		black.add("기모찌맨");
		pWord = new HashSet<String>();
		pWord.add("바보");//금칙어
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
			try {//시스템 알림 오라클 저장
				String query = "INSERT INTO chat_talking VALUES"
						+ "(SEQ_TALKING.nextval,?,?,default,?,SYSDATE)";
				psmt = con.prepareStatement(query);
				psmt.setString(1, room);
				psmt.setString(2, name);
				psmt.setString(3, URLDecoder.decode(msg,"UTF-8"));
				
				psmt.executeUpdate();
			} 
			catch (Exception e) {}
		}
		while(it.hasNext()) {
			try {
				String clientName = it.next();
				PrintWriter it_out =(PrintWriter)
						clientMap.get(clientName);
				if(entered.get(clientName).equals(room)) {//msg낼때 채팅방 확인
					boolean send2 = false;
					boolean send3 = false;
					if(block.containsKey(name)) {//block 대상인지 확인
						send2=block.get(name).equals(clientName);
					}
					if(block.containsKey(clientName)) {//block 대상인지 확인
						send3=block.get(clientName).equals(name);
					}
					boolean send = !send2&&!send3;
					if(send) {
						if(flag.equals("One")) {
							if(name.equals(clientName)) {
								it_out.println("[귓속말]"+sendName+":"+msg);
								try {
									String query = "INSERT INTO chat_talking VALUES"
											+ "(SEQ_TALKING.nextval,?,?,?,?,SYSDATE)";
									psmt = con.prepareStatement(query);
									psmt.setString(1, room);
									psmt.setString(2, sendName);
									psmt.setString(3, name);
									psmt.setString(4, URLDecoder.decode(msg,"UTF-8"));
									
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

		public void masterOut(String mName,String mRoom) {//방장이 나갈경우
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
			for(int i=0 ; i<strArr.length;i++) {//금칙어 필터링
				for(String a : pWord) {
					if(strArr[i].equals(a)) {
						strArr[i]="나쁜말";
					}
				}
				msgContent += strArr[i]+" ";
			}
			try {
				if(s.charAt(0)=='/') {
					if(strArr[0].equalsIgnoreCase("/invite")) {//초대할 경우
						if(!entered.containsKey(strArr[1])) {//대상이 대기실에 있는경우만 초대
							Set<RoomList> keys = roomMap.keySet();//roomMap에는 채팅방의 객체와 인원수
							for(RoomList key : keys) {//해당 방객체를 찾아서
								if(key.roomName.equals(chatroom)) {
									int member = roomMap.get(key);
									if(member>=1) {//접속자가 꽉차지 않았을경우
										inviteRoom.put(strArr[1], key);//초대Map에 대상이름과 방객체 저장
										out.println("초대메세지를 보냈습니다.");
										System.out.println(name+"님 대화초대 ->"+strArr[1]);
									}
									else {//채팅방이 꽉찼을경우 초대실패
										out.println("채팅방이 꽉찼습니다.");
									}
								}
							}
						}
						else {//대상이 다른 채팅방에 있는경우 초대불가
							out.println("대상을 초대할 수 없습니다.");
						}
					}
					else if(strArr[0].equalsIgnoreCase("/boom")) {//채팅방 폭파
						if(roomMaster.get(name).roomName.equals(chatroom)) {//방장인지 확인하고
							masterOut(name,chatroom);//방장이 나가는 경우의 메소드 호출
						}
					}
					else if(strArr[0].equalsIgnoreCase("/redcard")) {//강퇴
						if(roomMaster.get(name).roomName.equals(chatroom)) {//방장인지 확인하고
							Iterator<RoomList> outroom = roomMap.keySet().iterator();
							while(outroom.hasNext()) {//강퇴시 접속인원 변경을 위해 iterator사용
								RoomList a = (RoomList)outroom.next();
								if(a.roomName.equals(chatroom)){
									roomMap.replace(a, roomMap.get(a), roomMap.get(a)+1);//접속자수 변경
									sendAllMsg("","",strArr[1]+"님이 퇴장당하셨습니다.","All",chatroom);
									entered.remove(strArr[1]);//실질적인 강퇴
									System.out.println("["+chatroom+"에서 "+strArr[1]+"님 강제퇴장]");
								}
							}
						}
					}
					else if(strArr[0].equalsIgnoreCase("/roomout")) {//채팅방에서 나가는 경우
						if(roomMaster.containsKey(name)){//방장이 나갈땐 채팅방 자동폭파
							masterOut(name,chatroom);
						}
						else {
							Iterator<RoomList> outroom = roomMap.keySet().iterator();
							while(outroom.hasNext()) {
								RoomList a = (RoomList)outroom.next();
								if(a.roomName.equals(chatroom)){//iterator를 통해 방객체를 찾아
									roomMap.replace(a, roomMap.get(a), roomMap.get(a)+1);//접속자수 수정
									sendAllMsg("","",name+"님이 채팅방에서 나가셨습니다.","All",chatroom);
									entered.remove(name);//entered맵에서 사용자 삭제
								}
							}
						}
					}
					else if(strArr[0].equalsIgnoreCase("/myroomlist")) {//채팅방에 접속한 사용자 명단
						Set<String> keys = entered.keySet();
						System.out.println("["+name+"님 접속자리스트 요청");
						out.println("=접속자리스트=");
						int count=1;
						for(String key : keys) {//entered는 key값이 사용자,value가 방이름
							String value = entered.get(key);
							if(value.equalsIgnoreCase(chatroom)) {
								out.println(count++ +"."+key);//요청한 방과 같은 key값 출력
							}
						}
					}
					else if(strArr[0].equalsIgnoreCase("/to")) {//1회용 귓속말
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
					else if(strArr[0].equalsIgnoreCase("/fixto")) {//귓속말 고정
						if(entered.containsKey(strArr[1])) {//대화방에 입장한 사람인지 확인
							fixto.put(name, strArr[1]);//fixtoMap에 사용자 본인(key)과 대상(value) 저장
							out.println(strArr[1]+"님에게 귓속말 고정");
							System.out.println("["+name+" 님 "+strArr[1]+" 님에 귓속말 고정]");
						}
						else {
							out.println("유효한 사용자가 아닙니다.");
						}
					} 
					else if(strArr[0].equalsIgnoreCase("/unfixto")) {//고정귓속말 해제
						if(fixto.get(name).equals(strArr[1])) {//대상이 맞는지 확인
							fixto.remove(name);//fixtoMap에서 삭제
							out.println("고정귓속말 해제");
							System.out.println("["+name+" 님 "+strArr[1]+" 님에 귓속말 해제]");
						}
						else {
							out.println("대상이 잘못되었습니다.");
						}
					}
					else if(strArr[0].equalsIgnoreCase("/block")) {//차단기능
						if(entered.containsKey(strArr[1])) {//대화방에 입장한 사람인지 확인
							block.put(name,strArr[1]);//차단Map에 저장
							out.println(strArr[1]+"님의 메세지 차단");
							System.out.println("["+name+" 님 "+strArr[1]+"님 차단 설정]");
						}
						else {
							out.println("유효한 사용자가 아닙니다.");
						}
					}
					else if(strArr[0].equalsIgnoreCase("/unblock")) {//차단해제
						if(block.get(name).equalsIgnoreCase(strArr[1])) {
							block.remove(name);//맵에서 삭제
							out.println(strArr[1]+"님 차단 해제");
							System.out.println("["+name+" 님 "+strArr[1]+"님 차단 해제]");
						}
						else {
							out.println("차단된 사용자가 아닙니다.");
						}
					}
				}
				else {
					if(fixto.containsKey(name)) {//고정귓속말을 설정한 사람인 경우 귓속말로 보냄
						sendAllMsg(name,fixto.get(name), msgContent, "One",chatroom);
					}
					else {//아닌경우 전체메세지                     해당 채팅방에 보냄
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
				//블랙리스트
				if(black.contains(name)) {
					out.println("당신은 블랙리스트입니다.");
					System.out.println("[블랙리스트 접속차단:"+name+"]");
					this.interrupt();
					return;
				}
				//아이디 중복검사 ->receiver 설명
				if(clientMap.containsKey(name)) {
					out.println("중복된 아이디입니다.");
					System.out.println("[중복아이디 차단:"+name+"]");
					name=name+"temp";
					this.interrupt();
					return;
				}
			
				//접속자수 제한        인터페이스 상수로 해야되는데..그냥 씀..
				if(clientMap.size()>10) {
					out.println("접속자수가 초과되었습니다");
					System.out.println("[초과접속자 차단]");
					this.interrupt();
					return;
				}
				clientMap.put(name, out);
				out.println("명령어 확인은 /?를 입력하세요");//최초 접속시 명령어 확인을 위해 안내문출력
				System.out.println("["+name+" 접속]");
				System.out.println("[현재 접속자수"+clientMap.size()+"명]");
				while(in!=null) {
					s = in.readLine();
					s = URLDecoder.decode(s,"UTF-8");
					String[] strArr = s.split(" ");
					
					if(inviteRoom.containsKey(name)) {//초대된 방이 있는 경우
						RoomList a = inviteRoom.get(name);
						if(strArr[0].equalsIgnoreCase("y")) {//초대 수락시
							roomMap.replace(a, roomMap.get(a), roomMap.get(a)-1);
							entered.put(name, a.roomName);
							sendAllMsg("","",name+"님이 입장하셨습니다.","All",a.roomName);
							inviteRoom.remove(name);
						}
						else if(strArr[0].equalsIgnoreCase("n")) {//초대 거절시
							out.println("초대를 거절하셨습니다.");
							inviteRoom.remove(name);
						}
						else {
							out.println(a.roomName+"채팅방에 초대되셨습니다."+
									"\n수락(Y)/거절(N) 입력:");
						}
					}
					else if(entered.containsKey(name)) {//개설된 채팅방에 접속한 사람인 경우
						roomChat(name,s,entered.get(name));//자동으로 roomchat메소드로 보냄
					}
					else if(strArr[0].equals("/?")) {// /?실행했을때
						out.println("1.채팅방개설 : /makeroom\n"
								+ "2.채팅방입장 : /roomenter\n"
								+ "3.채팅방목록 : /chatlist");
					}
					else if(strArr[0].equalsIgnoreCase("/makeroom")) {//방 개설
						//방이름이 중복된 객체는 아닌지 확인
						Iterator<RoomList> is = roomMap.keySet().iterator();
						boolean flag2 = true;
						while(is.hasNext()) {
							RoomList a = (RoomList)is.next();
							if(a.roomName.equalsIgnoreCase(strArr[1])){
								flag2 = false;
							}
						}
						if(flag2) {
							if(strArr.length==4) {//pass있는 경우 총 길이 4
								room = new RoomList
										(strArr[1],strArr[2],Integer.parseInt(strArr[3]));
								roomMap.put(room, Integer.parseInt(strArr[3])-1);//개설자 바로 들어가짐
								roomMaster.put(name,room);//방장권한 설정 key값은 방장이름, value는 방객체
								entered.put(name, strArr[1]);//개설자 방 입장 map저장
								sendAllMsg("","",name+"님이 입장","All",strArr[1]);
								System.out.println(strArr[1]+" 채팅방이 개설되었습니다.");
							}
							else if(strArr.length==3) {//pass없는 경우 총 길이 3
								room = new RoomList
										(strArr[1],Integer.parseInt(strArr[2]));
								roomMap.put(room, Integer.parseInt(strArr[2])-1);//개설자 바로 들어가짐
								roomMaster.put(name,room);//키값은 방장이름, 벨류는 방객체
								entered.put(name, strArr[1]);//개설자 방 입장 map저장
								sendAllMsg("","",name+"님이 입장","All",strArr[1]);
								System.out.println(strArr[1]+" 채팅방이 개설되었습니다.");
							}
							else {//그 외는 잘못된 양식으로 안내문 출력
								out.println("/makeroom 방이름 비밀번호(비공개방) 입장인원 을 입력해주세요");
							}
						}
						else {//채팅방이 이미 있을경우
							out.println("채팅방이 이미 존재합니다.");
						}
					}
					else if(strArr[0].equalsIgnoreCase("/roomenter")) {//채팅방 입장
						Iterator<RoomList> is = roomMap.keySet().iterator();
						boolean findroom = false;
						while(is.hasNext()) {//입력한 방이 유효한지 점검
							RoomList a = (RoomList)is.next();
							if(a.roomName.equals(strArr[1])){//유효한 경우
								findroom = true;
								if(roomMap.get(a)>=1) {//입장제한 확인
									if(a.passWord.equals("")) {//공개방의 경우
										roomMap.replace(a, roomMap.get(a), roomMap.get(a)-1);
										entered.put(name, strArr[1]);
										sendAllMsg("","",name+"님이 입장하셨습니다.","All",strArr[1]);
									}
									else if(a.passWord.equalsIgnoreCase(strArr[2])){//비밀방의 경우 비번확인
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
						if(!findroom){//findroom이 true인경우 not연산자사용 
							out.println("존재하지 않는 방입니다.");
						}
					}
					else if(strArr[0].equalsIgnoreCase("/chatlist")) {//리스트 요청시 roomMap의 key로 정보출력
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
			finally {//접속이 끊어졌을때 권한,각종 정보들 삭제
				if(roomMaster.containsKey(name)) {//방장인경우 방 자동폭파
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
				}

				catch (Exception e2) {
					e2.printStackTrace();
				}
			}
		}
	}
}
