package com.example.trifonsheykin.smartlock;

import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

public class AccessCodeActivity extends AppCompatActivity {

    private ImageButton ibQrScanner;
    private Button bClear;
    private Button bPaste;
    private Button bSave;
    private EditText etAccessCode;
    private EditText etKeyTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access_code);

        ibQrScanner = findViewById(R.id.ib_qr_code);
        bClear = findViewById(R.id.b_clear);
        bPaste = findViewById(R.id.b_paste_from_clipboard);
        bSave = findViewById(R.id.b_save_access_code);
        etAccessCode = findViewById(R.id.et_access_code);
        etKeyTitle = findViewById(R.id.et_key_title);

        bSave.setEnabled(false);

        bClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etAccessCode.setText("");
            }
        });

        bPaste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard.hasPrimaryClip()) {
                    android.content.ClipDescription description = clipboard.getPrimaryClipDescription();
                    android.content.ClipData data = clipboard.getPrimaryClip();
                    if (data != null && description != null
                            && (description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                            || description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)))
                        etAccessCode.setText(String.valueOf(data.getItemAt(0).getText()));
                }
            }
        });

        bSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ibQrScanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AccessCodeActivity.this, QrReadActivity.class);
                startActivityForResult(intent, 0);
            }
        });


        etAccessCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(etAccessCode.getText().toString().length() > 57){
                    bSave.setEnabled(true);
                }else{
                    bSave.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == 0){
            String result = data.getStringExtra("result");
            etAccessCode.setText(Base64.decode(result, Base64.DEFAULT).toString());

        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }
}
