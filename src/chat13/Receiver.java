package chat13;
 
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;

public class Receiver extends Thread{
	
	Socket socket;
	BufferedReader in = null;
	public Receiver(Socket socket) {
		this.socket = socket;
		
		try {
			in = new BufferedReader(new InputStreamReader(this.socket.getInputStream(),"UTF-8"));
			
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
				System.out.println("[서버]"+ URLDecoder.decode(a,"UTF-8"));
			}
			catch (SocketException ne) {
				System.out.println("접속을 종료합니다.");
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
