package dk.cphbusiness.group11.GetBanks.exceptions;

public class GetBankWritingException extends GetBankException {
	private static final long serialVersionUID = 1L;
	
	public GetBankWritingException(String errMsg) {
		super(errMsg);
	}
	
	@Override
	public String getMessage() {
		return "ERROR: Could not write XML (" + errMsg + ")";
	}
}
