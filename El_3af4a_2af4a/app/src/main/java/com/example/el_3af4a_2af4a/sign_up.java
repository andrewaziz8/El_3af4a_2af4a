package com.example.el_3af4a_2af4a;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;

public class sign_up extends AppCompatActivity {

    private TextView sign_in;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.linear_sign_up);
        sign_in = (TextView)findViewById(R.id.SIGNUP);
        sign_in.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(sign_up.this, MainActivity.class);
                startActivity(i);
            }
        });
    }
}
