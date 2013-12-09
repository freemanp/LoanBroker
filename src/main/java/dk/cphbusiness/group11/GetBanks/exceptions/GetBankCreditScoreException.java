package dk.cphbusiness.group11.GetBanks.exceptions;

public class GetBankCreditScoreException extends GetBankException {

	private static final long serialVersionUID = 1L;
	
	public GetBankCreditScoreException(String errMsg) {
		super(errMsg);
	}
	
	@Override
	public String getMessage() {
		
		return "ERROR: Invalid credit score: " + errMsg + "\nCredit score must be between 0 and 800.";
	}
}
