package qm.ds.lab2.restservice;

public class ResponseErrors implements Response{
	
	private String error = "";
	
	public ResponseErrors(String error) {
		this.error = error;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}
}
