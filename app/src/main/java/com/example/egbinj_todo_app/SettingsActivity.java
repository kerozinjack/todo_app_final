package com.example.egbinj_todo_app;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


public class SettingsActivity extends AppCompatActivity {
    Button backToMain;
    SharedPreferences sharedPreferences;
    TextView greeting;
    Button clearCompleted;
    Switch switchHideCompleted;
    RadioGroup defaultPriority;
    RadioGroup sortMode;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sharedPreferences = getSharedPreferences("pref", MODE_PRIVATE);

        //nev
        greeting = findViewById(R.id.greetingTV);
        String nev = sharedPreferences.getString("name", "-");
        greeting.setText("Üdv, " + nev + "! 👋");

        greeting.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                EditText input = new EditText(SettingsActivity.this);
                input.setHint("Új neved");

                //jelenlegi nev
                String currentName = sharedPreferences.getString("name", "");
                input.setText(currentName);
                input.setSelection(currentName.length());

                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Név módosítása")
                        .setView(input)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                String newName = input.getText().toString().trim();

                                if (newName.isEmpty()) {
                                    Toast.makeText(SettingsActivity.this,
                                            "A név nem lehet üres!",
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("name", newName);
                                editor.apply();

                                greeting.setText("Üdv, " + newName + "! 👋");
                            }
                        })
                        .setNegativeButton("Mégse", null)
                        .show();

                return true;
            }
        });

        //doneok torles dialog
        clearCompleted = findViewById(R.id.clearCompletedBtn);
        clearCompleted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                builder.setTitle("Biztos?").setMessage("Biztos ki akarod törölni?").setPositiveButton("Igen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clearCompleted();
                    }
                }).setNegativeButton("Nem", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show();
            }
        });

        //done hdie
        switchHideCompleted = findViewById(R.id.switchHideCompleted);
        switchHideCompleted.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                sharedPreferences.edit()
                        .putBoolean("hide_completed", isChecked)
                        .apply();
            }
        });

        //prioritas
        defaultPriority = findViewById(R.id.defaultPriorityGroup);
        defaultPriority.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull RadioGroup group, int checkedId) {
                String value = "közepes";
                if (checkedId == R.id.priorityLow) {
                    value = "alacsony";
                } else if (checkedId == R.id.priorityHigh) {
                    value = "magas";
                }
                sharedPreferences.edit()
                        .putString("default_priority", value)
                        .apply();
            }
        });

        //sort
        sortMode = findViewById(R.id.sortModeGroup);
        sortMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull RadioGroup group, int checkedId) {
                String value = "priority";
                if (checkedId == R.id.sortNone) {
                    value = "none";
                } else if (checkedId == R.id.sortABC) {
                    value = "abc";
                }
                sharedPreferences.edit()
                        .putString("sort_mode", value)
                        .apply();
            }
        });

        //vissza mainre
        backToMain=findViewById(R.id.backBtn);
        backToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //ertekek beallitasa
        loadSettings();
    }

    private void loadSettings(){
        boolean hide = sharedPreferences.getBoolean("hide_completed", false);
        switchHideCompleted.setChecked(hide);

        String priority = sharedPreferences.getString("default_priority", "közepes");

        if (priority.equals("alacsony")) {
            defaultPriority.check(R.id.priorityLow);
        } else if (priority.equals("magas")) {
            defaultPriority.check(R.id.priorityHigh);
        } else {
            defaultPriority.check(R.id.priorityMedium);
        }

        String sort = sharedPreferences.getString("sort_mode", "priority");

        if (sort.equals("none")) {
            sortMode.check(R.id.sortNone);
        } else if (sort.equals("abc")) {
            sortMode.check(R.id.sortABC);
        } else {
            sortMode.check(R.id.sortPriority);
        }
    }

    private void clearCompleted() {
        sharedPreferences.edit()
                .putBoolean("clear_completed_request", true)
                .apply();

        finish();
    }

}