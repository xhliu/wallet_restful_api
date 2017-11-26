package hello;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallet/{userAccount}")
public class WalletController {
    private WalletClient client;

    public WalletController() throws Exception {
        client = new WalletClient();
    }

    // create an account and return account receive address
    // if the account already exists, do not create new
    @PostMapping("/create")
    public String create(@PathVariable String userAccount) {
        try {
            return this.client.createAccount(userAccount);
        } catch (Exception e) {
            return "";
        }
    }

    // query account balance
    @GetMapping("/balance")
    public double balance(@PathVariable String userAccount) {
        try {
            return this.client.getBalance(userAccount);
        } catch (Exception e) {
            return 0;
        }
    }

    // query account receive address
    @GetMapping("/address")
    public String address(@PathVariable String userAccount) {
        try {
            return this.client.getAddress(userAccount);
        } catch (Exception e) {
            return "";
        }
    }

    // transfer to another account in the same wallet
    // does not go to blockchain
    @PostMapping("/move")
    public boolean move(@PathVariable String userAccount, @RequestParam String toAccount, @RequestParam double amount) {
        try {
            return this.client.move(userAccount, toAccount, amount);
        } catch (Exception e) {
            return false;
        }
    }

    // transfer to another address, which can be not in the same wallet
    // return transaction id
    // has to go through blockchain
    @PostMapping("/sendfrom")
    public String add(@PathVariable String userAccount, @RequestParam String toAddress, @RequestParam double amount) {
        try {
            return this.client.sendFrom(userAccount, toAddress, amount);
        } catch (Exception e) {
            return "";
        }
    }
}
