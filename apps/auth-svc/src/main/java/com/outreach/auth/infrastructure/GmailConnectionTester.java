package com.outreach.auth.infrastructure;

import jakarta.mail.*;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class GmailConnectionTester {
    
    private static final String GMAIL_IMAP_HOST = "imap.gmail.com";
    private static final int GMAIL_IMAP_PORT = 993;
    
    /**
     * Test Gmail IMAP connection with provided credentials
     * @param email Gmail email address
     * @param appPassword Gmail app-specific password
     * @return true if connection successful, false otherwise
     * @throws MessagingException if connection fails
     */
    public boolean testConnection(String email, String appPassword) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", GMAIL_IMAP_HOST);
        props.put("mail.imaps.port", GMAIL_IMAP_PORT);
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.timeout", "5000");
        props.put("mail.imaps.connectiontimeout", "5000");
        
        Session session = Session.getInstance(props);
        Store store = null;
        
        try {
            store = session.getStore("imaps");
            store.connect(GMAIL_IMAP_HOST, email, appPassword);
            return store.isConnected();
        } finally {
            if (store != null && store.isConnected()) {
                store.close();
            }
        }
    }
}
