package com.mayank.securechat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.udacity.friendlychat.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileWriter;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import static com.mayank.securechat.MainActivity.DEFAULT_MSG_LENGTH_LIMIT;

public class MessageActivity extends AppCompatActivity {

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;
    public static String mUsername;
    private String senderUid;
    private String receiverUid;
    String public_key;
    HashMap<String,FriendlyMessage> messageHashMap=new HashMap<>();
    ArrayList<FriendlyMessage> messageArrayList=new ArrayList<>();
    List<FriendlyMessage> friendlyMessages;


    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference;
    private ChildEventListener mChildEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        Intent intent=getIntent();
        Bundle bundle=intent.getBundleExtra("data");
        senderUid=bundle.getString("sender");
        receiverUid=bundle.getString("receiver");
        public_key=bundle.getString("public_key");
        mUsername=FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        //mUsername="anonymous";

        mFirebaseDatabase=FirebaseDatabase.getInstance();
        mMessagesDatabaseReference=mFirebaseDatabase.getReference().child("messages");
        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);

        // Initialize message ListView and its adapter
         friendlyMessages= new ArrayList<>();
        loadMessages();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Fire an intent to show an image picker
            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Send messages on click

                String msg=mMessageEditText.getText().toString();
                FriendlyMessage friendlyMessage=new FriendlyMessage(msg,mUsername,null);
                mMessageAdapter.add(friendlyMessage);
                mMessageAdapter.notifyDataSetChanged();
                String timeStamp=new Date().toString();
                messageHashMap.put(timeStamp,friendlyMessage);
                String encrypted_msg=encrypt(msg);
                friendlyMessage=new FriendlyMessage(encrypted_msg,mUsername,null);
                //mMessagesDatabaseReference.push().setValue(friendlyMessage);
                // Clear input box

                //messageArrayList.add(friendlyMessage);
                sendMessageToFirebaseUser(timeStamp,friendlyMessage);
                mMessageEditText.setText("");
            }
        });

    }

    private void loadMessages() {
        SharedPreferences preferences=getSharedPreferences("CHAT_PREF",MODE_PRIVATE);
        String messages=preferences.getString(receiverUid,null);
        if(messages!=null){
            Gson gson=new Gson();
            messageHashMap=gson.fromJson(messages,new TypeToken<HashMap<String,FriendlyMessage>>(){}.getType());
            Map<String, FriendlyMessage> map = new TreeMap<>(messageHashMap);
            friendlyMessages.addAll(map.values());
            
        }
    }

    public String encrypt(String msg){
        Cipher cipher = null;
        String encrypted=null;
        try {



            byte[] keyBytes = Base64.decode(public_key,Base64.NO_WRAP);

            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            Key publicKey = null;


           publicKey = keyFactory.generatePublic(x509KeySpec);

            Log.d("public_key_enc","key-"+publicKey);
            cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            //byte[] encode=Base64.encode(msg.getBytes(),Base64.NO_WRAP);
            byte[] encryptedBytes = cipher.doFinal(msg.getBytes());
            /*
            Log.d("encrypted_bytes","val-"+encryptedBytes);
            Log.d("encrypted_default","val-"+Base64.encodeToString(encryptedBytes, Base64.NO_WRAP));
            */
            encrypted = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP);

            //encrypted=byte2hex(encryptedBytes);
            System.out.println("EEncrypted?????" + encrypted);
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }

        Log.d("encrypt","msg-"+msg+" encrypted-"+encrypted);
        return encrypted;
    }


    public String decrypt(String msg){
        Cipher cipher = null;
        String decrypted=null;
        try {

            byte[] keyBytes = Base64.decode(Constants.PRIVATE_KEY,Base64.NO_WRAP);

            PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            Key privateKey = keyFactory.generatePrivate(pkcs8KeySpec);

            Log.d("private_key_decrypt","key-"+privateKey);
            cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);


            byte[] decode=Base64.decode(msg,Base64.NO_WRAP);
            //byte[] decode = hex2byte(msg.getBytes());
            byte[] decryptedBytes = cipher.doFinal(decode);
            decrypted = new String(decryptedBytes);
            //decrypted=byte2hex(decryptedBytes);

        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        Log.d("decrypt","msg-"+msg+" decrypted-"+decrypted);
        return decrypted;
    }

    ValueEventListener valueEventListener=new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            if(messageHashMap!=null) {

                Iterator it = messageHashMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String,FriendlyMessage> msg = (Map.Entry<String, FriendlyMessage>)it.next();

                    if (!dataSnapshot.child(msg.getKey()).exists()) {
                        FriendlyMessage chat1=msg.getValue();
                        it.remove();
                        friendlyMessages.remove(chat1);
                        Log.d("Messagekey", "not exists");
                        //do ur stuff
                    } else {
                        Log.d("Messagekey", "exists");
                        //do something if not exists
                    }
                }
                if(mMessageAdapter!=null){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMessageAdapter.notifyDataSetChanged();
                        }
                    });
                }

            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };

    ChildEventListener messageListener=new ChildEventListener() {
        @Override
        public void onChildAdded(final DataSnapshot dataSnapshot, String s) {
            // Chat message is retreived.
            Log.d("firebaseMessage","onchildadded-"+(new Gson().toJson(dataSnapshot.toString())));
            final FriendlyMessage chat = dataSnapshot.getValue(FriendlyMessage.class);
            Log.d("firebaseMessage","onchildaddedChat-"+(new Gson().toJson(chat)));
            if(!messageHashMap.containsKey(dataSnapshot.getKey())) {
                Log.d("messages","key not present");
                final String decrypted=decrypt(chat.getText());
                if(decrypted!=null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            chat.setText(decrypted);
                            messageHashMap.put(dataSnapshot.getKey(), chat);
                            mMessageAdapter.add(chat);
                            mMessageAdapter.notifyDataSetChanged();
                        }
                    });

                }
            }else {
                Log.d("messages","key present-"+(new Gson().toJson(messageHashMap.get(dataSnapshot.getKey()))));
            }
            for (FriendlyMessage msg:messageHashMap.values()
                    ) {
                Log.d("messages","val-"+msg.getText());
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            Log.d("firebaseMessage","onchildChnaged-"+(new Gson().toJson(dataSnapshot.toString())));
        }

        @Override
        public void onChildRemoved(final DataSnapshot dataSnapshot) {
            Log.d("firebaseMessage","onchildremoved-"+(new Gson().toJson(dataSnapshot.toString())));
            if(messageHashMap.containsKey(dataSnapshot.getKey())) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        FriendlyMessage chat1=messageHashMap.remove(dataSnapshot.getKey());
                        boolean b=friendlyMessages.remove(chat1);
                        mMessageAdapter.notifyDataSetChanged();
                    }
                });

            }
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            Log.d("firebaseMessage","onchildmoved-"+(new Gson().toJson(dataSnapshot.toString())));
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.d("firebaseMessage","oncancelled-"+databaseError.getMessage());
            // Unable to get message.
        }

    };

    public void sendMessageToFirebaseUser(final String timestamp,final FriendlyMessage chat) {
        final String room_type_1 = senderUid + "_" + receiverUid;
        final String room_type_2 = receiverUid + "_" + senderUid;

        final DatabaseReference databaseReference = FirebaseDatabase.getInstance()
                .getReference();

        databaseReference.child("messages")
                .getRef()
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild(room_type_1)) {
                            Log.e("messages", "sendMessageToFirebaseUser: " + room_type_1 + " exists");
                            databaseReference.child("messages")
                                    .child(room_type_1)
                                    .child(timestamp)
                                    .setValue(chat);
                        } else if (dataSnapshot.hasChild(room_type_2)) {
                            Log.e("messages", "sendMessageToFirebaseUser: " + room_type_2 + " exists");
                            databaseReference.child("messages")
                                    .child(room_type_2)
                                    .child(timestamp)
                                    .setValue(chat);
                        } else {
                            Log.e("messages", "sendMessageToFirebaseUser: success");
                            databaseReference.child("messages")
                                    .child(room_type_1)
                                    .child(timestamp)
                                    .setValue(chat);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Unable to send message.
                    }
                });
    }

    public void getMessageFromFirebaseUser() {
        final String room_type_1 = senderUid + "_" + receiverUid;
        final String room_type_2 = receiverUid + "_" + senderUid;



        mMessagesDatabaseReference
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild(room_type_1)) {
                            Log.e("message", "getMessageFromFirebaseUser: " + room_type_1 + " exists");
                            mMessagesDatabaseReference
                                    .child(room_type_1)
                                    .addChildEventListener(messageListener);
                            mMessagesDatabaseReference
                                    .child(room_type_1)
                                    .addListenerForSingleValueEvent(valueEventListener);

                        } else if (dataSnapshot.hasChild(room_type_2)) {
                            Log.e("message", "getMessageFromFirebaseUser: " + room_type_2 + " exists");
                            mMessagesDatabaseReference
                                    .child(room_type_2)
                                    .addChildEventListener(messageListener);
                            mMessagesDatabaseReference
                                    .child(room_type_2)
                                    .addListenerForSingleValueEvent(valueEventListener);
                        } else {
                            Log.e("message", "getMessageFromFirebaseUser: no such room available");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Unable to get message
                    }
                });


    }

    @Override
    protected void onResume() {
        super.onResume();
        getMessageFromFirebaseUser();
    }


    private void DetachReadListener(){
        if(mChildEventListener!=null){
            mMessagesDatabaseReference.removeEventListener(mChildEventListener);
        }
        if(mMessageAdapter!=null)
            mMessageAdapter.clear();
    }


    @Override
    protected void onStop() {
        super.onStop();
        DetachReadListener();
        saveMessages();
    }

    private void saveMessages() {
        SharedPreferences.Editor editor=getSharedPreferences("CHAT_PREF",MODE_PRIVATE).edit();
        try {
            Gson gson=new Gson();
            String data=gson.toJson(messageHashMap);
            editor.putString(receiverUid,data);
            editor.apply();
            Log.d("messageListsave","list-"+(new Gson().toJson(messageHashMap)));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        messageHashMap.clear();
        friendlyMessages.clear();
        messageArrayList.clear();

    }
}
