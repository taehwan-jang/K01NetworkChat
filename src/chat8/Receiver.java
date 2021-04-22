package chat8;
 
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;

public class Receiver extends Thread{
	
	Socket socket;
	BufferedReader in = null;
	public Receiver(Socket socket) {
		this.socket = socket;
		
		try {
			in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
			
		} catch (Exception e) {
			System.out.println("예외1:"+e);
		}
	}
	@Override
	public void run() {
		while(in != null) {
			try {
				String a = in.readLine();
				if(a==null) {
					break;
				}
				System.out.println("Thread Receive : "+ a);
			}
			catch (SocketException ne) {
				System.out.println("SocketException 발생됨. 루프탈출");
				break;
			}	
			catch (Exception e) {
				System.out.println("예외2:"+e);
			}
		}
		try {
			in.close();
			socket.close();
		} 
		catch (Exception e) {
			System.out.println("예외3:"+e);
		}
		finally {
			System.out.println("접속이 종료되었습니다.");
		}
	}
}
