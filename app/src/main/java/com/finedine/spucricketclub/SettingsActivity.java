package com.finedine.spucricketclub;

import android.os.Bundle;

import androidx.activity.ComponentActivity;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.switchmaterial.SwitchMaterial;

import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.ViewGroup;
import android.view.View;
import android.content.Context;
import android.util.TypedValue;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use a vertical LinearLayout as root
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);

        // Toolbar
        Toolbar toolbar = new Toolbar(this, null, com.google.android.material.R.style.Widget_Material3_Toolbar);
        toolbar.setTitle("Settings");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        root.addView(toolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(this, 56)));

        root.addView(createSwitchRow(this, "Enable AI Player Recognition"));
        root.addView(createSwitchRow(this, "Enable Auto-Scoring"));
        root.addView(createSwitchRow(this, "Share Analytics Data"));
        root.addView(createSwitchRow(this, "Enable Fulltrack AI Integration"));

        setContentView(root);
    }

    private View createSwitchRow(Context c, String label) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, dp(c, 16), 0, dp(c, 16));

        TextView tv = new TextView(c);
        tv.setText(label == null ? "Option" : label);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        SwitchMaterial sw = new SwitchMaterial(c, null, com.google.android.material.R.style.Widget_Material3_CompoundButton_Switch);
        sw.setChecked(false);
        sw.setText(""); // Remove label text on switch (M3 best practice)
        sw.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(tv);
        row.addView(sw);
        return row;
    }

    private int dp(Context c, int val) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, val, c.getResources().getDisplayMetrics()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
