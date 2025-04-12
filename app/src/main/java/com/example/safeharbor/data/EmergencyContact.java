package com.example.safeharbor.data;

public class EmergencyContact {
    private String name;
    private String phoneNumber;
    private boolean isPrimary;

    public EmergencyContact() {
        // Default constructor
    }

    public EmergencyContact(String name, String phoneNumber, boolean isPrimary) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.isPrimary = isPrimary;
    }

    // Override toString for easy storage
    @Override
    public String toString() {
        return name + "," + phoneNumber + "," + isPrimary;
    }

    // Parse from string
    public static EmergencyContact fromString(String data) {
        String[] parts = data.split(",");
        if (parts.length == 3) {
            return new EmergencyContact(
                parts[0],
                parts[1],
                Boolean.parseBoolean(parts[2])
            );
        }
        return null;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }
} 