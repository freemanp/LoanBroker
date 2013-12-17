package dk.cphbusiness.group11.Aggregator;

public class LoanResponseDetails {
    public LoanResponseDetails(){}
    
    public LoanResponseDetails(String ssn, String bank, double interestRate){
        this.ssn = ssn;
        this.bank = bank;
        this.interestRate = interestRate;
    }
    
    private String ssn;

    public String getSsn() {
        return ssn;
    }

    public void setSsn(String ssn) {
        this.ssn = ssn;
    }

    private String bank;

    public String getBank() {
        return bank;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

    private double interestRate;

    public double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(double interestRate) {
        this.interestRate = interestRate;
    }
}
