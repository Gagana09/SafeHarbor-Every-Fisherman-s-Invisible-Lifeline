package com.example.safeharbor.repository;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.safeharbor.data.EmergencyContact;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EmergencyContactRepository {
    private static final String PREFS_NAME = "EmergencyContacts";
    private static final String KEY_CONTACTS = "contacts";
    private final SharedPreferences prefs;

    public EmergencyContactRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveContacts(List<EmergencyContact> contacts) {
        Set<String> contactStrings = new HashSet<>();
        for (EmergencyContact contact : contacts) {
            contactStrings.add(contact.toString());
        }
        prefs.edit().putStringSet(KEY_CONTACTS, contactStrings).apply();
    }

    public List<EmergencyContact> getContacts() {
        Set<String> contactStrings = prefs.getStringSet(KEY_CONTACTS, new HashSet<>());
        List<EmergencyContact> contacts = new ArrayList<>();
        
        for (String contactString : contactStrings) {
            EmergencyContact contact = EmergencyContact.fromString(contactString);
            if (contact != null) {
                contacts.add(contact);
            }
        }
        return contacts;
    }

    public void addContact(EmergencyContact newContact) {
        List<EmergencyContact> contacts = getContacts();
        // Check for duplicates
        for (int i = 0; i < contacts.size(); i++) {
            if (contacts.get(i).getPhoneNumber().equals(newContact.getPhoneNumber())) {
                contacts.set(i, newContact); // Replace existing contact
                saveContacts(contacts);
                return;
            }
        }
        contacts.add(newContact);
        saveContacts(contacts);
    }

    public void removeContact(String phoneNumber) {
        List<EmergencyContact> contacts = getContacts();
        contacts.removeIf(contact -> contact.getPhoneNumber().equals(phoneNumber));
        saveContacts(contacts);
    }

    public List<EmergencyContact> getPrimaryContacts() {
        List<EmergencyContact> contacts = getContacts();
        List<EmergencyContact> primaryContacts = new ArrayList<>();
        for (EmergencyContact contact : contacts) {
            if (contact.isPrimary()) {
                primaryContacts.add(contact);
            }
        }
        return primaryContacts;
    }
} 