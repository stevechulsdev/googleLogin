package com.ksc.test01;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.vending.billing.util.IabException;
import com.android.vending.billing.util.IabHelper;
import com.android.vending.billing.util.IabResult;
import com.android.vending.billing.util.Inventory;
import com.android.vending.billing.util.Purchase;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private GoogleSignInClient mGoogleSignInClient;
    private GoogleApiClient mGoogleApiClient;
    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "MainActivity";

    private FirebaseAuth mAuth;

    private IabHelper mHelper;

    static final int RC_REQUEST = 10001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.buy_inapp).setOnClickListener(this);
        findViewById(R.id.navi_btn).setOnClickListener(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("35188028322-vjvok5s3t4h11pckge8r8blmrdpgh94j.apps.googleusercontent.com")
                .requestEmail()
                .build();

       mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

       mAuth = FirebaseAuth.getInstance();

//        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvv+Nnccdl4rNhQmdqzUL+uvsc649gjb7fm6atfAuhkMPsV1ng4tT5F4+AMaml4UpmOn6f4GyDPUGNGjYYYhHeSpLwZYrBz1/RdfMM8ivcY2fD0NFYROIz5xw69lMcFY/g+Ym9Git7KNngowd3WZa79TtvGLudFlQs2eiEKScZD0TdOcT3O2ZvDIvWDMGHMaZiu8twsoFbzQgXmyLvBsMVFZci/KSUG42wQNsywXuaz11FBmkudnvRyNCWrr48p/gTMVtW1tDmLOZqhwAjp3oe5TyzXcBihyf73hyfvFdwvm8jAaSj6kvYv81BIhi+XJJ8DdIxLcII4K3QVfn1IgABQIDAQAB";

        mHelper = new IabHelper(this, base64EncodedPublicKey);

        mHelper.enableDebugLogging(true);

        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if(!result.isSuccess())
                {
                    Toast.makeText(MainActivity.this, "Error : " + result, Toast.LENGTH_SHORT).show();
                    return;
                }

                if(mHelper == null)
                {
                    return;
                }

                try
                {
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {

            if(mHelper == null)
            {
                return;
            }

            if(result.isFailure())
            {
                return;
            }

            Purchase vPurchase = inventory.getPurchase("com.stevechuls.item01");

            if(vPurchase != null && verifyDeveloperPayload(vPurchase))
            {

                mHelper.consumeAsync(inventory.getPurchase("com.stevechuls.item01"), mConsumeFinishedListener);
            }
        }
    };

    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        @Override
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            if(mHelper == null)
            {
                return;
            }

            if(result.isSuccess())
            {
                return;
            }
        }
    };

    boolean verifyDeveloperPayload(Purchase p)
    {
        String payload = p.getDeveloperPayload();

        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();
//        Toast.makeText(this, "currentUser_getDisplayName : " + currentUser.getDisplayName(), Toast.LENGTH_SHORT).show();

    }

    private void signIn()
    {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signOut()
    {
        mAuth.signOut();

        mGoogleSignInClient.signOut().addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.d(TAG, "google_signOut");
            }
        });
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if(i == R.id.sign_in_button)
        {
            signIn();
        }
        else if(i == R.id.sign_out_button)
        {
            signOut();
        }
        else if(i == R.id.buy_inapp)
        {
            charge();
        }
        else if(i == R.id.navi_btn)
        {
            Intent intent = new Intent(this, NavigationActivity.class);
            this.startActivity(intent);
        }
    }

    private void charge()
    {
        try
        {
            mHelper.launchPurchaseFlow(this, "com.stevechuls.item01", RC_REQUEST, mPurchaseFinishedListener, "");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            if(mHelper == null)
            {
                return;
            }

            if(result.isFailure())
            {
                return;
            }

            if(!verifyDeveloperPayload(purchase))
            {
                return;
            }

            if(purchase.getSku().equals("com.stevechuls.item01"))
            {
                try
                {
                    mHelper.consumeAsync(purchase, mConsumeFinishedListener);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                    return;
                }
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == RC_SIGN_IN)
        {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
//            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
            try
            {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
                Toast.makeText(this, "account_getDisplayName : " + account.getDisplayName(), Toast.LENGTH_SHORT).show();
            }
            catch (ApiException e)
            {
                e.printStackTrace();
            }
        }

        if(mHelper == null)
        {
            return;
        }

        if(!mHelper.handleActivityResult(requestCode, resultCode, data))
        {
            super.onActivityResult(requestCode, resultCode, data);
        }

    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct)
    {
        Log.d(TAG, "firebaseAuthWithGoogle : " + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful())
                        {
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                        }
                        else
                        {
                            Log.d(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed. ", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


}
