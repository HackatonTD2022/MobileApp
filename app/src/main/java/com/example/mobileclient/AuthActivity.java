package com.example.mobileclient;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

public class AuthActivity extends AppCompatActivity {

    private boolean doubleBackToExitPressedOnce = false;
    private BluetoothDevice server = null;
    private BluetoothSocket socket = null;
    private AESSecurityCap securityCap = null;

    private final int bufferSize = 2048;

    private InputStream input;
    private OutputStream output;

    private class User implements Serializable {
        private final String UserName;
        private final String UUID;

        public User(String userName, String uuid) {
            UserName = userName;
            UUID = uuid;
        }

        public String getUserName() {
            return UserName;
        }

        public String getUUID() {
            return UUID;
        }
    }

    private ArrayList<User> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        securityCap = new AESSecurityCap();

        // deserialize user list.
        try {
            FileInputStream fileIn = new FileInputStream("users.db");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            userList = (ArrayList<User>) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        if(Objects.isNull(userList))
            userList = new ArrayList<>();


        Intent intent = getIntent();

        try {
            BluetoothDevice server = intent.getParcelableExtra("Server");
            UUID uuid = UUID.fromString(intent.getStringExtra("SocketUUID"));
            BluetoothSocket socket = server.createRfcommSocketToServiceRecord(uuid);
            socket.connect();
            if(!socket.isConnected()) {
                Intent i = new Intent();
                setResult(RESULT_CANCELED, i);
                finish();
            }

            input = socket.getInputStream();
            output = socket.getOutputStream();

            //initPubKeys();

            showStart();

        } catch (IOException | SecurityException e) {
            e.printStackTrace();
        }
    }

    private void initPubKeys() {
        StringBuilder sb = new StringBuilder();
        sb.append("pubkey ").append(securityCap.getPublickey().toString());

        sendToServer(sb.toString());

        String answer = recieveFromServer();

        if(strEq(answer, "pubkey")) {
            String pubkey = answer.substring(answer.indexOf(' ') + 1);
            //securityCap.setReceiverPublicKey();
        }
    }

    @Override
    protected void onDestroy() {
        sendToServer("close");

        // serialize user list
        try {
            FileOutputStream fileOut = new FileOutputStream("users.db");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(userList);
            out.close();
            fileOut.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        super.onDestroy();
    }

    private void showStart() {
        setContentView(R.layout.auth_start);
        Button regBtn = (Button) findViewById(R.id.RegisterButton);
        Button logBtn = (Button) findViewById(R.id.LoginButton);

        regBtn.setOnClickListener(this::regOnClick);
        logBtn.setOnClickListener(this::logOnClick);
    }

    @Override
    public void onBackPressed() {
        showStart();

        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
    }

    // Registration
    private void regOnClick(View view) {
        setContentView(R.layout.auth_register);
        Button submit = (Button) findViewById(R.id.SubmitButton);
        submit.setOnClickListener(this::createNewUser);
    }

    private void createNewUser(View view) {
        TextInputEditText textInputEditText = (TextInputEditText) findViewById(R.id.TextInput);

        StringBuilder str = new StringBuilder();
        str.append("create_user ");

        String username = textInputEditText.getText().toString();
        if(username.length() <= 0) {
            Toast.makeText(view.getContext(), "No username defined", Toast.LENGTH_SHORT).show();
            return;
        }
        String uuidStr = UUID.randomUUID().toString();

        str.append(username)
                .append(' ')
                .append("UUID ")
                .append(uuidStr)
                .append(' ');

        sendToServer(str.toString());

        String answer = recieveFromServer();
        if(strEq(answer, "ok")) {
            User user = new User(username, uuidStr);
            userList.add(user);
            Toast.makeText(view.getContext(), "Success", Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(view.getContext(), "This username already defined", Toast.LENGTH_LONG).show();
        }
    }
    //----------------------------------------------

    // Login
    public class UsersAdapter extends ArrayAdapter<User> {
        AuthActivity authActivity = null;
        public UsersAdapter(Context context, ArrayList<User> users, AuthActivity activity) {
            super(context, 0, users);
            authActivity = activity;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            User user = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.auth_user_list_view, parent, false);
            }
            // Lookup view for data population
            TextView tvName = (TextView) convertView.findViewById(R.id.TextView);
            // Populate the data into the template view using the data object
            tvName.setText(user.getUserName());

            return convertView;
        }
    }

    /*private void loginUser(User user) {

    }*/


    private void logOnClick(View view) {
        setContentView(R.layout.auth_login);

        ListView listView = (ListView) findViewById(R.id.UserListView);
        UsersAdapter adapter = new UsersAdapter(view.getContext(), userList, this);

        listView.setAdapter(adapter);

        TextView textView = new TextView(this);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setText("List of users");

        listView.addHeaderView(textView);

        listView.setOnItemClickListener((parent, view1, position, id) -> {

            User user = userList.get(position - 1);

            StringBuilder sb = new StringBuilder();
            sb.append("login ")
                    .append(user.getUserName())
                    .append(' ')
                    .append("UUID ")
                    .append(user.getUUID())
                    .append(' ');

            sendToServer(sb.toString());

            String answer = recieveFromServer();
            if(!strEq(answer, "ok")) {
                Toast.makeText(this, "Can't login as " + user.getUserName(), Toast.LENGTH_LONG).show();
            }
        });

    }
    //----------------------------------------------

    private boolean strEq(String as, String bs) {
        char[] a = as.toCharArray();
        char[] b = bs.toCharArray();
        boolean eq = true;
        for(int i = 0; i < b.length; i++) {
            if(a[i] != b[i]) {
                eq = false;
                break;
            }
        }
        return eq;
    }

    private void sendToServer(String Message) {
        try {
            output.write(Message.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String recieveFromServer() {
        byte[] buffer = new byte[bufferSize];
        String msg = null;
        try {
            int r = input.read(buffer);
            if(r > 0)
                msg = new String(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return msg;
    }
}