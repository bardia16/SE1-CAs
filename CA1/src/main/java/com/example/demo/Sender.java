package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class Sender {

    @Autowired
    private AccountService accountService;

    @Autowired
    private JmsTemplate jmsTemplate;

    @JmsListener(destination = "INQ")
    public void receiveMessage(String message) {

        String[] parts = message.split(" ");
        String command = parts[0];

        if(command.equals("DEPOSIT")) {

            accountService.deposit(parts[1], Integer.parseInt(parts[2]));

            System.out.println("0 Deposit successful");

            jmsTemplate.convertAndSend("OUTQ", "0 Deposit successful");

        } else if(command.equals("WITHDRAW")) {

            int result = accountService.withdraw(parts[1], Integer.parseInt(parts[2]));

            String responseMessage;
            if(result == 0) {
                responseMessage = "0 Withdraw successful";
            } else if (result == 1) {
                responseMessage = "1 Insufficient funds";
            } else if (result == 2) {
                responseMessage = "2 Unknown account number";
            }
            else {
                responseMessage = "Unknown error";
            }

            System.out.println(responseMessage);

            jmsTemplate.convertAndSend("OUTQ", responseMessage);

        } else if(command.equals("BALANCE")) {

            int balance = accountService.getBalance(parts[1]);
            String responseMessage;

            if(balance == -1) {
                responseMessage = "2 Unknown account number";
            }
            else {
                responseMessage = "0 Balance:" + balance;
            }

            System.out.println(responseMessage);

            jmsTemplate.convertAndSend("OUTQ", responseMessage);

        }
        else {
            String responseMessage;
            responseMessage = "3 Unknown command";
            System.out.println(responseMessage);

            jmsTemplate.convertAndSend("OUTQ", responseMessage);
        }

    }

}