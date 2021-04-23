package chat13;

public class RoomList {
	
	String roomName ="";
	String passWord	="";
	int maxEntered=0;
	
	//방의 속성을 2가지로 나눌 수 있음
	//password가 있는 경우
	public RoomList(String roomName, String passWord, int maxEntered) {
		this.roomName=roomName;
		this.passWord=passWord;
		this.maxEntered=maxEntered;
	}
	//password가 없는 경우
	public RoomList(String roomName, int maxEntered) {
		this.roomName=roomName;
		this.maxEntered=maxEntered;
	}
	//필요한 메소드가 있을까? 여기서 계속 진행하는걸로?
	//해당 key값을 이용해서 flag 전달? 그거 좋은 방법인거같다
	
	
	
	
	
}
