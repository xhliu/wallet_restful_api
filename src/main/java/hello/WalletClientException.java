package hello;

// exception throw by wallet client
public class WalletClientException extends Exception {
    public WalletClientException(String msg) {
        super(msg);
    }
}
