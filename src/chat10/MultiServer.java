package chat10;

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

public class MultiServer {
	static ServerSocket serverSocket = null;
	static Socket socket = null;
	
	Map<String, PrintWriter> clientMap;
	Set<String> black;
	Set<String> pWord;
	
	public MultiServer() {
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
		System.out.println(sendName);

		while(it.hasNext()) {
			try {
				String clientName = it.next();
				PrintWriter it_out =(PrintWriter)
						clientMap.get(clientName);
				if(flag.equals("One")) {
					if(name.equals(clientName)) {
						it_out.println("[귓속말]"+sendName+":"+msg);
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
						name=name+"temp";
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
						if(strArr[0].equals("/to")) {
							sendAllMsg(name,strArr[1], msgContent, "One");
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
