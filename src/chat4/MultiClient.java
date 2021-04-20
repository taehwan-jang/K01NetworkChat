package chat4;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class MultiClient {

	public static void main(String[] args) {
		//클라이언트의 대화명을 입력한다.
		Scanner scan = new Scanner(System.in);
		System.out.print("이름을 입력하세요:");
		String s_name = scan.nextLine();
		
		//스트림 생성시 Receiver 클래스로 옮겨진 입력스트림은 제외
		PrintWriter out =null;
		
		try {
			//서버에 접속
			String ServerIP = "localhost";
			if(args.length>0) {
				ServerIP = args[0]; 
			}
			//서버로 접속요청..
			Socket socket = new Socket(ServerIP,9999);
			//서버가 accept()하면 연결성공..
			System.out.println("서버와 연결되었습니다.");
			
			//서버에서 보내는 메세지를 읽어올 Receiver 쓰레드 객체생성 및 시작
			Thread receiver = new Receiver(socket);
			//setDaemon(true); -> 선언하지 않았으므로 독립쓰레드로 생성된다.
			receiver.start();
			
			//대화명을 서버로 전송
			out = new PrintWriter(socket.getOutputStream(),true);
			out.println(s_name);
			
			//대화명을 전송한 이후에는 메세지를 전송함.
			while(out!=null) {
				try {
					String s2 = scan.nextLine();
					if(s2.equalsIgnoreCase("q")) {
						break;
					}
					else {
						out.println(s2);
					}
				} catch (Exception e) {
					System.out.println("예외:"+e);
				}
			}
			out.close();
			socket.close();
		} 
		catch (Exception e) {
			System.out.println("예외발생[MultiClient]"+e);
		}
		
	}
}
