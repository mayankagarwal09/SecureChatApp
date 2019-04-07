
package com.mayank.securechat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.udacity.friendlychat.R;
import com.google.gson.Gson;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    public static final int RC_SIGN_IN=1;


    private ListView mUserListView;
    private MessageAdapter mMessageAdapter;
    private UsersAdapter mUserAdapter;
    List<User> users=new ArrayList<>();


    private String mUsername;
    private FirebaseDatabase mFirebaseDatabase;

    private DatabaseReference mUsersDatabaseReference;
    private DatabaseReference mUsersref;
    private ChildEventListener mChildEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsername = ANONYMOUS;

        mFirebaseDatabase=FirebaseDatabase.getInstance();
        mFirebaseAuth=FirebaseAuth.getInstance();

        mUsersDatabaseReference=mFirebaseDatabase.getReference().child("users");
        // Initialize references to views

        mUserListView = (ListView) findViewById(R.id.userListView);





        mAuthStateListener=new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                final FirebaseUser user=firebaseAuth.getCurrentUser();


                if(user!=null){


                   mUsersDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                       @Override
                       public void onDataChange(DataSnapshot dataSnapshot) {
                           if(!dataSnapshot.hasChild(user.getUid())){
                             generateKeyPairs(user);
                           }else {
                               loadUserKey();
                           }
                       }

                       @Override
                       public void onCancelled(DatabaseError databaseError) {

                       }
                   });

                    onSignedInInitialize(user.getDisplayName());
                    Toast.makeText(getApplicationContext(),"You are now signed in",Toast.LENGTH_SHORT).show();
                    getAllUsersFromFirebase();
                }else {
                    //signed out

                    onSignedOutCleanUp();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.EmailBuilder().build(),
                                            new AuthUI.IdpConfig.GoogleBuilder().build()
                                            )
                                    )
                                    .setLogo(R.drawable.securechat)
                                    .build(),
                            RC_SIGN_IN);
                }
            }

        };

        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    private void generateKeyPairs(FirebaseUser user){
        KeyPair keyPair = generateKeys();

        byte[] publicKey = keyPair.getPublic().getEncoded();
        byte[] privateKey = keyPair.getPrivate().getEncoded();
        String private_key=Base64.encodeToString(privateKey, Base64.NO_WRAP);
        String public_key=Base64.encodeToString(publicKey, Base64.NO_WRAP);
        Log.d("private_key_gen","key-"+keyPair.getPrivate());
        Log.d("public_key_gen","key-"+keyPair.getPublic());
        savePrivateKey(private_key);



        Log.d( "private_key_base64",Base64.encodeToString(privateKey, Base64.NO_WRAP));
        Log.d("public_key_base64", Base64.encodeToString(publicKey, Base64.NO_WRAP) );

        User newUser=new User(user.getUid(),user.getDisplayName(),user.getEmail(),public_key);
        mUsersDatabaseReference
                .child(user.getUid())
                .setValue(newUser)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d("user","successfully added");
                    }
                });
    }

    private void loadUserKey() {
        SharedPreferences pref=getSharedPreferences("CHAT_PREF",MODE_PRIVATE);
        Constants.PRIVATE_KEY=pref.getString("private_key",null);
        if(Constants.PRIVATE_KEY == null)
            generateKeyPairs(FirebaseAuth.getInstance().getCurrentUser());
    }

    private void savePrivateKey(String private_key) {
        Constants.PRIVATE_KEY=private_key;
        SharedPreferences.Editor editor=getSharedPreferences("CHAT_PREF",MODE_PRIVATE).edit();
        editor.putString("private_key",private_key);
        editor.apply();
    }

    public KeyPair generateKeys() {
        KeyPair keyPair = null;
        try {
            // get instance of rsa cipher
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);            // initialize key generator
            keyPair = keyGen.generateKeyPair(); // generate pair of keys
        } catch(GeneralSecurityException e) {
            System.out.println(e);
        }
        return keyPair;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(MainActivity.this, "signed in", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED){
                Toast.makeText(MainActivity.this,"signed in cancelled",Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    public void getAllUsersFromFirebase() {
        final String[] myPublic = {null};

        /*
        mUsersDatabaseReference
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Iterator<DataSnapshot> dataSnapshots = dataSnapshot.getChildren()
                                .iterator();
                        while (dataSnapshots.hasNext()) {
                            DataSnapshot dataSnapshotChild = dataSnapshots.next();
                            User user = dataSnapshotChild.getValue(User.class);
                            if (!TextUtils.equals(user.uid,
                                    FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                                if(!users.contains(user)) {
                                    Log.d("onDataChange","usersList-"+(new Gson().toJson(users))+"\nsize-"+users.size());
                                    users.add(user);
                                }
                            }else {
                                myPublic[0] =user.getPublicKey();
                            }
                        }

                        mUserAdapter = new UsersAdapter(MainActivity.this, R.layout.item_user, users);
                        mUserListView.setAdapter(mUserAdapter);

                        Log.d("userslist","list-"+users.toString());
                        // All users are retrieved except the one who is currently logged
                        // in device.
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Unable to retrieve the users.
                    }
                });
                */

        mUsersDatabaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                User user = dataSnapshot.getValue(User.class);
                if (!TextUtils.equals(user.uid,
                        FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                    if(!users.contains(user)) {
                        Log.d("childAdded","usersList-"+(new Gson().toJson(users))+"\nsize-"+users.size());
                        users.add(user);
                    }
                }else {
                    myPublic[0] =user.getPublicKey();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mUserAdapter = new UsersAdapter(MainActivity.this, R.layout.item_user, users);
                        mUserListView.setAdapter(mUserAdapter);
                    }
                });

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if(users.contains(user)) {
                    users.remove(user);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(mUserAdapter!=null)
                                mUserAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mUserListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                User user=mUserAdapter.getItem(position);

                Intent intent=new Intent(MainActivity.this,MessageActivity.class);
                Bundle bundle=new Bundle();
                bundle.putString("sender",FirebaseAuth.getInstance().getCurrentUser().getUid());
                bundle.putString("receiver",user.getUid());
                bundle.putString("public_key",user.getPublicKey());
                intent.putExtra("data",bundle);
                startActivity(intent);

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.sign_out_menu:
                mUsersDatabaseReference.child(FirebaseAuth.getInstance().getUid()).removeValue();
                AuthUI.getInstance().signOut(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();


    }

    private void onSignedInInitialize(String username){
        mUsername=username;

    }

    private void onSignedOutCleanUp(){

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
    }
}
