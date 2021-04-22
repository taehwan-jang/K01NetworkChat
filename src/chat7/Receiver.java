package chat7;
 
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;

//서버가 보내는 Echo메세지를 읽어오는 쓰레드 클래스 
public class Receiver extends Thread{
	
	Socket socket;
	BufferedReader in = null;
	//클라이언트가 접속시 생성한 Socket객체를 생성자에서 매개변수로 받음
	public Receiver(Socket socket) {
		this.socket = socket;
		
		//Socket객체를 기반으로 입력스트림을 생성한다.
		try {
			in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
			
		} catch (Exception e) {
			System.out.println("예외1:"+e);
		}
	}
	/*
	run()메소드에서는 서버가 보내는 Echo메세지를
	지속적으로 읽어오고, 예외발생시 while문을 탈출한다.
	 */
	@Override
	public void run() {
		while(in != null) {
			try {

				System.out.println("Thread Receive : "+ in.readLine());
			}
			catch (SocketException ne) {
				System.out.println("SocketException 발생됨. 루프탈출");
				/*
				클라이언트가 q를 입력하여 접속을 종료하면 무한루프가 발생되므로
				탈출할 수 있도록 별도의 catch블럭을 추가하고 break를 걸어준다.
				 */
				break;
			}	
			catch (Exception e) {
				/*
				클라이언트가 접속을 종료할 경우 SocketException이 발생되면서
				무한루프에 빠지게 된다.
				 */
				System.out.println("예외2:"+e);
			}
		}
		try {
			in.close();
		} catch (Exception e) {
			System.out.println("예외3:"+e);
		}
	}
}
