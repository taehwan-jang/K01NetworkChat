package chat12;
 
import java.net.Socket;
import java.util.Scanner;

public class MultiClient {

	public static void main(String[] args) {
		//클라이언트의 대화명을 입력한다.
		Scanner scan = new Scanner(System.in);
		System.out.print("이름을 입력하세요:");
		String s_name = scan.nextLine();
		
		/*
		메세지 송수진을 위한 클래스를 별도로 만들었으므로
		Client클래스에서는 Socket 객체만 생성한다.
		 */
		try {
			//서버에 접속요청
			String ServerIP = "localhost";
			if(args.length>0) {
				ServerIP = args[0]; 
			}
			Socket socket = new Socket(ServerIP,9999);
			System.out.println("서버와 연결되었습니다.");
			
			//서버가 Echo해준 메세지를 받기 위한 리시버 쓰레드
			Thread receiver = new Receiver(socket);
			receiver.start();
			
			//서버에 메세지를 전송하기 위한 샌더 쓰레드
			Thread sender = new Sender(socket,s_name);
			sender.start();
			
		} 
		catch (Exception e) {
			System.out.println("예외발생[MultiClient]"+e);
		}
		
	}
}
