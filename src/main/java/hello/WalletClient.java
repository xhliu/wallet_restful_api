package hello;

import java.io.*;
import java.math.BigDecimal;
import java.util.Properties;

import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;
import com.neemre.btcdcli4j.core.client.BtcdClient;
import com.neemre.btcdcli4j.core.client.BtcdClientImpl;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class WalletClient {
    private BtcdClient client;

    WalletClient() throws Exception {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        CloseableHttpClient httpProvider = HttpClients.custom().setConnectionManager(cm)
                .build();
        Properties nodeConfig = new Properties();
        InputStream is = new BufferedInputStream(new FileInputStream("src/main/resources/node_config.properties"));
        nodeConfig.load(is);
        is.close();
        this.client = new BtcdClientImpl(httpProvider, nodeConfig);
    }

    // create an account and return account receive address
    // if the account already exists, do not create new
    public String createAccount(String newUserAccount) throws CommunicationException, BitcoindException {
        return this.client.getAccountAddress(newUserAccount);
    }

    // query account balance of a user
    public double getBalance(String userAccount) throws CommunicationException, BitcoindException {
        return this.getBalanceBigDecimal(userAccount).doubleValue();
    }

    // query account receive address
    public String getAddress(String userAccount) throws CommunicationException, BitcoindException {
        return this.client.getAccountAddress(userAccount);
    }

    // transfer to another account in the same wallet
    // does not go to blockchain
    public boolean move(String fromAccount, String toAccount, double amount) throws CommunicationException, BitcoindException, WalletClientException {
        BigDecimal sanitizedAmount = this.sanitizeAmount(amount);

        // this is necessary since client does not check balance when moving and balance can be negative
        BigDecimal balance = this.getBalanceBigDecimal(fromAccount);
        if (sanitizedAmount.compareTo(balance) > 0) {
            throw new WalletClientException("Account has insufficient funds");
        }

        return this.client.move(fromAccount, toAccount, sanitizedAmount);
    }

    // transfer to another address, which can be not in the same wallet
    // return transaction id
    // has to go through blockchain
    // TODO
    // 1) ensure sufficient balance for transaction fee in addition to transfer amount, to prevent overdraft
    // 2) validate address, e.g., using existing code
    public String sendFrom(String fromAccount, String toAddress, double amount) throws CommunicationException, BitcoindException, WalletClientException {
        BigDecimal sanitizedAmount = sanitizeAmount(amount);

        return this.client.sendFrom(fromAccount, toAddress, sanitizedAmount);
    }

    // round up bitcoin amount less than 1 satoshi (10^-8 bitcoin)
    // 0.000000005 -> 0.00000001
    // 0.000000004 -> 0.00000000
    public BigDecimal truncateBtcAmount(double amountInBtc) {
        return BigDecimal.valueOf(amountInBtc).setScale(8, BigDecimal.ROUND_HALF_UP);
    }

    // sanitize transfer amount
    public BigDecimal sanitizeAmount(double amount) throws WalletClientException {
        if (amount <= 0) {
            throw new WalletClientException("Transfer amount must be positive");
        }

        return truncateBtcAmount(amount);
    }

    private BigDecimal getBalanceBigDecimal(String userAccount) throws CommunicationException, BitcoindException {
        return this.client.getBalance(userAccount);
    }
}
