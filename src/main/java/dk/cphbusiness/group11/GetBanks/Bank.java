package dk.cphbusiness.group11.GetBanks;

public class Bank {
	private String id;
	private int minimumCreditScore;
	
	public Bank(String id, int minimumCreditScore) {
		this.id = id;
		this.minimumCreditScore = minimumCreditScore;
	}

	public String getId() {
		return id;
	}

	public int getMinimumCreditScore() {
		return minimumCreditScore;
	}
}
