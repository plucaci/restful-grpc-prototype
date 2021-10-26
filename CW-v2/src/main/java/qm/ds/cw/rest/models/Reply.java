package qm.ds.cw.rest.models;

public class Reply implements Response {
	
	private String message = "";
	private ReplyType replyType;
	
	public Reply(String message, ReplyType replyType) {
		this.message   = message;
		this.replyType = replyType;
	}
	
	public String getMessage() {
		return this.replyType.name() + ": " + this.message;
	}
	
}
