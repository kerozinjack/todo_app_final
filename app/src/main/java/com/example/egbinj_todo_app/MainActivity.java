package com.example.egbinj_todo_app;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {



    SharedPreferences sharedPreferences;
    TextView greeting, completedTasks;
    TableLayout tasksTable;
    ArrayList<Task> tasks;
    String file="tasks.txt";
    EditText taskName;
    boolean hide=false;
    Button settings;
    Button statistics;

    //priority sorthoz
    private int priorityValue(String p) {
        if(p==null) return -1;
        switch (p) {
            case "magas": return 2;
            case "közepes": return 1;
            case "alacsony": return 0;
            default: return -1;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });



        sharedPreferences = getSharedPreferences("pref", MODE_PRIVATE);
        //elso inditas udvozles
        greeting = findViewById(R.id.greetingTV);
        if(sharedPreferences.getBoolean("first_run", true)) {
            showNameDialog();
        } else {
            String nev = sharedPreferences.getString("name", "-");
            greeting.setText("Üdv, " + nev + "! 👋");
        }

        //settings
        settings=findViewById(R.id.settingsBtn);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        statistics=findViewById(R.id.statisticsBtn);
        statistics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int high = sharedPreferences.getInt("total_high", 0);
                int medium = sharedPreferences.getInt("total_medium", 0);
                int low = sharedPreferences.getInt("total_low", 0);

                int total = high + medium + low;

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("📊 Statisztika")
                        .setMessage(
                                "🔴 Magas: " + high + "\n" +
                                        "🟡 Közepes: " + medium + "\n" +
                                        "🟢 Alacsony: " + low + "\n\n" +
                                        "Összesen: " + total
                        )
                        .setPositiveButton("OK", null)
                        .show();
            }
        });


        tasks = new ArrayList<>();
        tasksTable=findViewById(R.id.tasksTable);
        taskName=findViewById(R.id.taskNameET);
        completedTasks=findViewById(R.id.completedTasksTV);

        //settingsbol
        hide = sharedPreferences.getBoolean("hide_completed", false);

        //betoltes tasks.txt
        loadTasks();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //doneok torles ha megkapta a flaget
        boolean clearRequest=sharedPreferences.getBoolean("clear_completed_request", false);
        if (clearRequest) {
            clearCompletedTasks();
            sharedPreferences.edit()
                    .putBoolean("clear_completed_request", false)
                    .apply();
        }

        //ha a settingsbe atirtuk
        String nev = sharedPreferences.getString("name", "-");
        greeting.setText("Üdv, " + nev + "! 👋");


        hide = sharedPreferences.getBoolean("hide_completed", false);

        refreshTasks(); //ui
        updateCompletedTasks(); //counter
    }

    //lista torles+fajlfrissites
    private void clearCompletedTasks() {
        tasks.removeIf(t -> t.done);
        saveTasks();
    }

    //elso inditas nevbekeros dialog
    private void showNameDialog() {
        EditText input = new EditText(this);
        input.setHint("Neved");

        new AlertDialog.Builder(this)
                .setTitle("Add meg a neved")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {

                    String nev = input.getText().toString();

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("first_run", false);
                    editor.putString("name", nev);
                    editor.apply();
                    greeting.setText("Üdv, " + nev + "! 👋");
                })
                .show();

    }

    public void addTask(View view) {
        String text = taskName.getText().toString();

        if (text.isEmpty()){
            Toast.makeText(this, "Hiba! Add meg a teendő nevét!", Toast.LENGTH_SHORT).show();
        }
        else{
            showPriorityDialog(text);
            taskName.setText("");
        }
    }

    private void showPriorityDialog(String text) {
        String[] priorities = {"alacsony", "közepes", "magas"};
        String defaultPriority =
                sharedPreferences.getString("default_priority", "közepes");

        int defaultIndex = priorityValue(defaultPriority);
        final int[] selected = {defaultIndex};

        new AlertDialog.Builder(this)
                .setTitle("Válassz prioritást!")
                .setSingleChoiceItems(priorities, selected[0], (dialog, which) -> {
                    selected[0] = which;
                })
                .setPositiveButton("OK", (dialog, which) -> {

                    Task task = new Task(text, priorities[selected[0]], false, false);
                    tasks.add(task);

                    saveTasks();
                    updateCompletedTasks();
                    refreshTasks();

                    taskName.setText("");
                })
                .setNegativeButton("Mégse", null)
                .show();
    }

    private void applyTaskColor(TextView text, Task task) {

        if (task.done) {
            text.setTextColor(Color.GRAY);
            return;
        }

        if (task.priority == null) {
            text.setTextColor(Color.BLACK);
            return;
        }

        switch (task.priority) {
            case "magas":
                text.setTextColor(Color.RED);
                break;

            case "közepes":
                text.setTextColor(Color.parseColor("#FFA500")); // narancs (sárga túl halvány)
                break;

            case "alacsony":
                text.setTextColor(Color.GREEN);
                break;

            default:
                text.setTextColor(Color.BLACK);
        }
    }

    //tablazat egy sor
    private void addTaskToTable(Task task) {
        TableRow row = new TableRow(this);

        TableRow.LayoutParams rowParams =
                new TableRow.LayoutParams(
                        TableRow.LayoutParams.MATCH_PARENT,
                        TableRow.LayoutParams.WRAP_CONTENT);

        rowParams.setMargins(0, 0, 0, 20);
        row.setLayoutParams(rowParams);

        row.setPadding(16, 16, 16, 16);
        row.setBackgroundColor(Color.WHITE);
        row.setMinimumHeight(120);

        CheckBox checkBox = new CheckBox(this);
        checkBox.setChecked(task.done);
        checkBox.setPadding(8, 0, 24, 0);

        TextView text = new TextView(this);
        text.setText(task.title);
        text.setTextSize(16);
        text.setPadding(12, 12, 12, 12);

        TableRow.LayoutParams textParams =
                new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
        text.setLayoutParams(textParams);

        applyTaskColor(text, task);

        Button deleteBtn = new Button(this);
        deleteBtn.setText("✕");
        deleteBtn.setTextColor(Color.WHITE);
        deleteBtn.setBackgroundColor(Color.parseColor("#E53935"));
        deleteBtn.setPadding(0, 0, 0, 0);
        deleteBtn.setMinWidth(90);

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                if (!task.done && isChecked && !task.counted) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();

                    if ("magas".equals(task.priority)) {
                        int val = sharedPreferences.getInt("total_high", 0);
                        editor.putInt("total_high", val + 1);

                    } else if ("közepes".equals(task.priority)) {
                        int val = sharedPreferences.getInt("total_medium", 0);
                        editor.putInt("total_medium", val + 1);

                    } else if ("alacsony".equals(task.priority)) {
                        int val = sharedPreferences.getInt("total_low", 0);
                        editor.putInt("total_low", val + 1);
                    }

                    editor.apply();

                    task.done = true;
                    checkBox.setEnabled(false);
                    task.counted = true;
                }

                applyTaskColor(text, task);
                saveTasks();
                updateCompletedTasks();
                refreshTasks();
            }
        });

        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Biztos?")
                        .setMessage("Biztos ki akarod törölni?")
                        .setPositiveButton("Igen", (dialog, which) -> {
                            tasks.remove(task);
                            tasksTable.removeView(row);

                            saveTasks();
                            updateCompletedTasks();
                            refreshTasks();
                        })
                        .setNegativeButton("Nem", (dialog, which) -> dialog.dismiss());

                builder.create().show();
            }
        });

        row.addView(checkBox);
        row.addView(text);
        row.addView(deleteBtn);

        tasksTable.addView(row);
    }

    //fajlbairas
    private void saveTasks() {
        try {
            FileOutputStream fos = openFileOutput(file, MODE_PRIVATE);

            StringBuilder sb = new StringBuilder();

            for (Task t : tasks) {
                sb.append(t.title)
                        .append("|")
                        .append(t.done)
                        .append("|")
                        .append(t.priority)
                        .append("|")
                        .append(t.counted)
                        .append("\n");
            }

            fos.write(sb.toString().getBytes("UTF-8"));
            fos.close();

        } catch (Exception e) {
            Toast.makeText(this, "Hiba a fájlba történő íráskor", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    //fajlbetoltes
    private void loadTasks() {

        try {
            FileInputStream fis = openFileInput(file);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");

            StringBuilder sb = new StringBuilder();

            int ch;
            while ((ch = isr.read()) != -1) {
                sb.append((char) ch);
            }

            isr.close();
            fis.close();

            String content = sb.toString();

            if (content.isEmpty()) return;
            String[] lines = content.split("\n");
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|");
                if (parts.length == 4) {
                    String title = parts[0];
                    boolean done = Boolean.parseBoolean(parts[1]);
                    String priority = parts[2];
                    boolean counted = Boolean.parseBoolean(parts[3]);

                    Task task = new Task(title, priority, done, counted);

                    tasks.add(task);
                }
            }
            refreshTasks();
            updateCompletedTasks();

        } catch (Exception e) {
            tasks = new ArrayList<>();
            Toast.makeText(this, "Hiba a tasks.txt betöltésekor", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    //done counter
    private void updateCompletedTasks() {
        int high = sharedPreferences.getInt("total_high", 0);
        int medium = sharedPreferences.getInt("total_medium", 0);
        int low = sharedPreferences.getInt("total_low", 0);

        int total = high + medium + low;

        completedTasks.setText("Összesen " + total + " teendőt teljesítettél!");
    }

    //ui+filter+sort
    private void refreshTasks() {
        //clear ui
        tasksTable.removeAllViews();

        //eredeti
        ArrayList<Task> displayList = new ArrayList<>(tasks);

        //filter
        String sortMode = sharedPreferences.getString("sort_mode", "priority");
        ArrayList<Task> filtered = new ArrayList<>();
        for (Task t : displayList) {
            if (hide && t.done) continue;
            filtered.add(t);
        }

        //sort
        if (sortMode.equals("abc")) {
            filtered.sort((a, b) ->
                    a.title.compareToIgnoreCase(b.title));

        } else if (sortMode.equals("priority")) {
            filtered.sort((a, b) ->
                    priorityValue(b.priority) - priorityValue(a.priority));
        }

        //kiiras
        for (Task t : filtered) {
            addTaskToTable(t);
        }
    }
}