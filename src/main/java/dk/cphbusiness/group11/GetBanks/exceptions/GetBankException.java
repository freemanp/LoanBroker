package dk.cphbusiness.group11.GetBanks.exceptions;

public class GetBankException extends Exception {
	private static final long serialVersionUID = 1L;

	protected String errMsg;
	
	public GetBankException(String errMsg) {
		super();
		this.errMsg = errMsg;
	}

	@Override
	public String getMessage() {
		return "ERROR: " + errMsg;
	}

	@Override
	public String toString() {
		return this.getMessage();
	}
}
