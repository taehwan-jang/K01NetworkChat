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
	
	
	
	
	
}
