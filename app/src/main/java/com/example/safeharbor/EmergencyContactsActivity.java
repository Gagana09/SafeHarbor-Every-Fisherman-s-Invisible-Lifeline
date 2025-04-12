package com.example.safeharbor;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.safeharbor.data.EmergencyContact;
import com.example.safeharbor.repository.EmergencyContactRepository;
import java.util.ArrayList;
import java.util.List;
import android.app.AlertDialog;

public class EmergencyContactsActivity extends AppCompatActivity {
    private EmergencyContactRepository repository;
    private TextInputEditText nameInput;
    private TextInputEditText phoneInput;
    private MaterialCheckBox primaryCheckbox;
    private ListView contactsList;
    private ArrayAdapter<String> adapter;
    private List<EmergencyContact> contacts;
    private View addContactCard;
    private MaterialButton addButton;
    private TextInputLayout nameInputLayout;
    private TextInputLayout phoneInputLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contacts);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        repository = new EmergencyContactRepository(this);
        
        // Initialize views
        nameInput = findViewById(R.id.nameInput);
        phoneInput = findViewById(R.id.phoneInput);
        primaryCheckbox = findViewById(R.id.primaryCheckbox);
        contactsList = findViewById(R.id.contactsList);
        addButton = findViewById(R.id.addButton);
        addContactCard = findViewById(R.id.addContactCard);
        nameInputLayout = findViewById(R.id.nameInputLayout);
        phoneInputLayout = findViewById(R.id.phoneInputLayout);

        // Setup contacts list
        contacts = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        contactsList.setAdapter(adapter);

        // Setup click listeners
        addButton.setOnClickListener(v -> addContact());
        contactsList.setOnItemLongClickListener((parent, view, position, id) -> {
            showDeleteDialog(position);
            return true;
        });

        loadContacts();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addContact() {
        String name = nameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        boolean isPrimary = primaryCheckbox.isChecked();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate phone number format
        if (!phone.matches("\\+?\\d{10,15}")) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if we already have a contact
        if (!contacts.isEmpty()) {
            Toast.makeText(this, "Only one emergency contact is allowed", Toast.LENGTH_SHORT).show();
            return;
        }

        EmergencyContact contact = new EmergencyContact(name, phone, true); // Always set as primary
        repository.addContact(contact);
        
        // Clear inputs
        nameInput.setText("");
        phoneInput.setText("");
        primaryCheckbox.setChecked(false);
        
        loadContacts();
        Toast.makeText(this, "Emergency contact added successfully", Toast.LENGTH_SHORT).show();
    }

    private void loadContacts() {
        contacts = repository.getContacts();
        adapter.clear();
        
        for (EmergencyContact contact : contacts) {
            String displayText = contact.getName() + " (" + contact.getPhoneNumber() + ") 🚨";
            adapter.add(displayText);
        }

        if (contacts.isEmpty()) {
            adapter.add("No emergency contact added yet");
            showAddContactForm();
        } else {
            hideAddContactForm();
        }
    }

    private void showAddContactForm() {
        addContactCard.setVisibility(View.VISIBLE);
        nameInputLayout.setEnabled(true);
        phoneInputLayout.setEnabled(true);
        addButton.setEnabled(true);
        primaryCheckbox.setChecked(true); // Always checked as primary
        primaryCheckbox.setEnabled(false); // Cannot change primary status
    }

    private void hideAddContactForm() {
        addContactCard.setVisibility(View.GONE);
        Toast.makeText(this, "Maximum one emergency contact allowed", Toast.LENGTH_SHORT).show();
    }

    private void showDeleteDialog(int position) {
        if (contacts.isEmpty() || position >= contacts.size()) {
            return;
        }

        EmergencyContact contact = contacts.get(position);
        new AlertDialog.Builder(this)
            .setTitle("Delete Emergency Contact")
            .setMessage("Are you sure you want to delete " + contact.getName() + "? You can add a different contact after deletion.")
            .setPositiveButton("Delete", (dialog, which) -> {
                repository.removeContact(contact.getPhoneNumber());
                loadContacts();
                Toast.makeText(this, "Contact deleted", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
} 