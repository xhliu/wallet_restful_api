package hello;

import com.neemre.btcdcli4j.core.CommunicationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;

import static org.junit.Assert.*;

public class WalletClientTest {
    private WalletClient client;
    private static final String DEFAULT_ACCOUNT = "";
    private static final double MIN_AMOUNT = 1e-5;
    // less than a satoshi
    private static final double DELTA = 1e-9;
    // https://testnet.manu.backend.hamburg/faucet
    private static final String TESTNET_FAUCET_ADDR = "2N8hwP1WmJrFF5QWABn38y63uYLhnJYJYTF";

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        // NOTE:
        // 1) make sure bitcoin core w/ wallet is running
        // 2) get some testnet bitcoins from faucet to fund wallet
        client = new WalletClient();
    }

    @Test
    public void createAccount() throws Exception {
        String addr = client.createAccount("foo");
        // TODO: add more checks other than length
        // testnet address is of 26 to 34 chars
        assertTrue(addr.length() >= 26);
        assertTrue(addr.length() <= 34);
    }

    @Test
    public void getBalance() throws Exception {
        // default account
        double balance = client.getBalance(DEFAULT_ACCOUNT);
        assertTrue(balance > 0);
    }

    @Test
    public void getAddress() throws Exception {
        String addr = client.getAddress(DEFAULT_ACCOUNT);
        assertTrue(addr.length() >= 26);
        assertTrue(addr.length() <= 34);
    }

    @Test
    public void move() throws Exception {
        double balance = client.getBalance(DEFAULT_ACCOUNT);

        // TODO: replace "foo" with unique account every run
        // move: test equality since there is no tx fee
        client.move(DEFAULT_ACCOUNT, "foo", balance);
        assertEquals(balance, client.getBalance("foo"), DELTA);
        assertEquals(0, client.getBalance(DEFAULT_ACCOUNT), DELTA);

        // move back
        client.move("foo", DEFAULT_ACCOUNT, balance);
        assertEquals(balance, client.getBalance(DEFAULT_ACCOUNT), DELTA);
        assertEquals(0, client.getBalance("foo"), DELTA);

        // try to overdraw
        exception.expect(WalletClientException.class);
        exception.expectMessage("Account has insufficient funds");
        client.move(DEFAULT_ACCOUNT, "foo", balance + MIN_AMOUNT);
    }

    @Test
    public void sendFrom() throws Exception {
        double balance = client.getBalance(DEFAULT_ACCOUNT);

        // move: strictly smaller since there is tx fee
        client.sendFrom(DEFAULT_ACCOUNT, TESTNET_FAUCET_ADDR, MIN_AMOUNT);
        assertTrue(client.getBalance(DEFAULT_ACCOUNT) < (balance - MIN_AMOUNT));

        // try to overdraw
        exception.expect(CommunicationException.class);
        exception.expectMessage("Error #1003002: The server responded with a non-OK (5xx) HTTP status code. Status line: HTTP/1.1 500 Internal Server Error");
        client.sendFrom(DEFAULT_ACCOUNT, TESTNET_FAUCET_ADDR, balance + MIN_AMOUNT);
    }

    @Test
    public void truncateBtcAmount() throws Exception {
        {
            // round up 9th digit `5` right of the decimal point
            double amount = 0.1234567850001;
            BigDecimal truncatedAmount = client.truncateBtcAmount(amount);
            BigDecimal expectedAmount = BigDecimal.valueOf(0.12345679);
            assertTrue(truncatedAmount.compareTo(expectedAmount) == 0);
        }
        {
            // round down 9th digit `4` right of the decimal point
            double amount = 0.123456784999;
            BigDecimal truncatedAmount = client.truncateBtcAmount(amount);
            BigDecimal expectedAmount = BigDecimal.valueOf(0.12345678);
            assertTrue(truncatedAmount.compareTo(expectedAmount) == 0);
        }
    }

    @Test
    public void negativeTransfer() throws Exception {
        exception.expect(WalletClientException.class);
        exception.expectMessage("Transfer amount must be positive");
        client.sanitizeAmount(-1);
    }

    @Test
    public void positiveTransfer() throws Exception {
        client.sanitizeAmount(1);
    }
}