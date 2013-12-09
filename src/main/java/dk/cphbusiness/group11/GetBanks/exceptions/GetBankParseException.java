package dk.cphbusiness.group11.GetBanks.exceptions;

public class GetBankParseException extends GetBankException {

	private static final long serialVersionUID = 1L;
	
	public GetBankParseException(String errMsg) {
		super(errMsg);
	}
	
	@Override
	public String getMessage() {
		return "ERROR: XML is incorrectly formatted (" + errMsg + ").";
	}
}
