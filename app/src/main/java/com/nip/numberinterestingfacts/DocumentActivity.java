package com.nip.numberinterestingfacts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.nip.numberinterestingfacts.ui.EdgeToEdgeInsets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Shows a bundled text document: the privacy policy (formerly MenuActivity) or licenses. */
public class DocumentActivity extends AppCompatActivity {
    static final String PRIVACY_POLICY_ASSET = "TermsAndCondition.txt";
    static final String LICENSES_ASSET = "licenses/open_source_licenses.txt";
    private static final String EXTRA_ASSET = "asset";
    private static final String EXTRA_TITLE = "title";

    public static Intent privacyPolicy(@NonNull Context context) {
        return create(context, PRIVACY_POLICY_ASSET, R.string.privacy_policy_title);
    }

    public static Intent licenses(@NonNull Context context) {
        return create(context, LICENSES_ASSET, R.string.licenses_title);
    }

    private static Intent create(Context context, String asset, @StringRes int title) {
        return new Intent(context, DocumentActivity.class)
                .putExtra(EXTRA_ASSET, asset)
                .putExtra(EXTRA_TITLE, title);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document);
        EdgeToEdgeInsets.apply(this, findViewById(R.id.root), findViewById(R.id.appBar),
                findViewById(R.id.scroll), null);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        String asset = getIntent().getStringExtra(EXTRA_ASSET);
        int title = getIntent().getIntExtra(EXTRA_TITLE, R.string.privacy_policy_title);
        // Only the two bundled documents may be opened.
        if (!LICENSES_ASSET.equals(asset)) {
            asset = PRIVACY_POLICY_ASSET;
            title = R.string.privacy_policy_title;
        }
        toolbar.setTitle(title);

        TextView documentText = findViewById(R.id.documentText);
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getAssets().open(asset), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) text.append(line).append('\n');
            documentText.setText(text.toString().trim());
        } catch (IOException e) {
            documentText.setText(R.string.document_load_error);
        }
    }
}
