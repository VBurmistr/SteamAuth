import java.math.BigInteger;

public class Confirmation {

	public BigInteger ID;
	public BigInteger Key;
	public int IntType;
	public BigInteger Creator;
	public ConfirmationType ConfType;

	public Confirmation(BigInteger id, BigInteger key, int type, BigInteger creator) {
		this.ID = id;
		this.Key = key;
		this.IntType = type;
		this.Creator = creator;

		switch (type) {
		case 1:
			this.ConfType = ConfirmationType.GenericConfirmation;
			break;
		case 2:
			this.ConfType = ConfirmationType.Trade;
			break;
		case 3:
			this.ConfType = ConfirmationType.MarketSellTransaction;
			break;
		default:
			this.ConfType = ConfirmationType.Unknown;
			break;
		}
	}

	public enum ConfirmationType {
		GenericConfirmation, Trade, MarketSellTransaction, Unknown
	}
}