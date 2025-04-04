package com.jorge.accounts.utils;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class AccountUtils {
    private static final int ACCOUNT_NUMBER_LENGTH = 14;

    // To keep it simple
    public String generateAccountNumber() {
        Random random = new Random();
        StringBuilder accountNumber = new StringBuilder();

        for (int i = 0; i < ACCOUNT_NUMBER_LENGTH; i++) {
            accountNumber.append(random.nextInt(10));
        }

        return accountNumber.toString();
    }
}
