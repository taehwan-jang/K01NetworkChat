package chat8;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MultiServer {
	static ServerSocket serverSocket = null;
	static Socket socket = null;
	
	Map<String, PrintWriter> clientMap;
	
	public MultiServer() {
		clientMap = new HashMap<String, PrintWriter>();
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
						it_out.println(msg);
					} 
					else {
						it_out.println("["+name+"]"+msg);
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
				in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
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
				Iterator<String> it = clientMap.keySet().iterator();
				while(it.hasNext()) {
					String name2 = it.next();
					if(name2.equals(name)) {
						name=name+"temp";
						this.interrupt();
						return;
					}
				}
				sendAllMsg("","",name+"님이 입장","All");
				clientMap.put(name, out);
				System.out.println(name+" 접속");
				System.out.println("현재 접속자수"+clientMap.size()+"명");
				while(in!=null) {
					
					s = in.readLine();
					if(s==null) {
						break;
					}
					System.out.println(name+">>"+s);
					if(s.charAt(0)=='/') {
						String[] strArr = s.split(" ");
						String msgContent ="";
						for(int i=2 ; i<strArr.length;i++) {
							msgContent += strArr[i]+" ";
						}
						if(strArr[0].equals("/to")) {
							sendAllMsg(name,strArr[1], msgContent, "One");
						}
					}
					else {
						sendAllMsg("",name, s,"All");
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
