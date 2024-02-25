package com.example.demo;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AccountService {

    private Map<String, Integer> accounts = new HashMap<>();

    public void deposit(String accountNo, int amount) {
        accounts.putIfAbsent(accountNo, 0);
        accounts.computeIfPresent(accountNo, (k, v) -> v + amount);
    }

    public int withdraw(String accountNo, int amount) {
        if(!accounts.containsKey(accountNo)) {
            return 2; // Unknown account
        }
        int balance = accounts.get(accountNo);
        if(balance < amount) {
            return 1; // Insufficient balance
        }
        accounts.computeIfPresent(accountNo, (k, v) -> v - amount);
        return 0; // Success
    }

    public int getBalance(String accountNo) {
        if(!accounts.containsKey(accountNo)) {
            return -1; // Unknown account
        }
        return accounts.get(accountNo);
    }

}