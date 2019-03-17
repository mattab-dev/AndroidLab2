package pl.edu.pwr.wiz.laboratorium2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 0;
    private static final int MY_PERMISSIONS_REQUEST_PHONE = 1;
    private static final int MY_PERMISSIONS_REQUEST_CONTACTS = 2;
    private static final int MY_PERMISSIONS_REQUEST_SMS = 3;
    private static final int MY_PICK_FROM_GALLERY = 4;

    private static final int REQUEST_IMAGE_CAPTURE = 0;
    private static final int REQUEST_CONTACT_DATA = 1;
    private static final int REQUEST_LOAD_PHOTO = 2;

    private String number;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.photoLoader:
                return loadPhoto();
            case R.id.action_photo:
                return takePhoto();

            case R.id.action_contacts:
                return fetchContact();

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /* Callback po nadaniu praw dostępu */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Dostęp nadany, uruchamiamy ponownie zrobienie zdjęcia
                    takePhoto();
                } else {
                    // Dostęp nie udany. Wyświetlamy Toasta
                    Toast.makeText(getApplicationContext(), R.string.access_denied, Toast.LENGTH_LONG).show();
                }
                return;
            }

            case MY_PERMISSIONS_REQUEST_CONTACTS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Dostęp nadany, uruchamiamy ponownie zrobienie zdjęcia
                    fetchContact();
                } else {
                    // Dostęp nie udany. Wyświetlamy Toasta
                    Toast.makeText(getApplicationContext(), R.string.access_denied, Toast.LENGTH_LONG).show();
                }
                return;
            }

            case MY_PERMISSIONS_REQUEST_PHONE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Dostęp nadany, uruchamiamy ponownie zrobienie zdjęcia
                    callNumber();
                } else {
                    // Dostęp nie udany. Wyświetlamy Toasta
                    Toast.makeText(getApplicationContext(), R.string.access_denied, Toast.LENGTH_LONG).show();
                }
                return;
            }

            case MY_PERMISSIONS_REQUEST_SMS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Dostęp nadany, mozna wyslac sms
                    sendSms();
                } else {
                    // Dostęp nie udany. Wyświetlamy Toasta
                    Toast.makeText(getApplicationContext(), R.string.access_denied, Toast.LENGTH_LONG).show();
                }
                return;
            }

            case MY_PICK_FROM_GALLERY: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Dostęp nadany, mozna otworzyc galerie
                    loadPhoto();
                } else {
                    // Dostęp nie udany. Wyświetlamy Toasta
                    Toast.makeText(getApplicationContext(), R.string.access_denied, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /* Callback do rezultatów z różnych wywoływanych aktywności */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            /* Czyscimy aktualny content */
            ViewGroup contentMain = (ViewGroup) this.findViewById(R.id.content_main);
            contentMain.removeAllViews();

            /* Tworzymy nowy obrazek */
            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(imageBitmap);
            imageView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));

            /* Dodajemy nowy obrazek do layoutu */
            contentMain.addView(imageView);
        } else if (requestCode == REQUEST_LOAD_PHOTO && resultCode == RESULT_OK) {
            ViewGroup contentMain = this.findViewById(R.id.content_main);
            contentMain.removeAllViews();
            ImageView view = new ImageView(this);
            view.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                view.setImageBitmap(selectedImage);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            contentMain.addView(view);
        } else if (requestCode == REQUEST_CONTACT_DATA && resultCode == RESULT_OK) {
            /* Czyscimy aktualny content */
            ViewGroup contentMain = (ViewGroup) this.findViewById(R.id.content_main);
            contentMain.removeAllViews();

            Uri contactData = data.getData();
            Cursor c = managedQuery(contactData, null, null, null, null);

            if (c.moveToFirst()) {
                String id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                String hasPhone = c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

                /* Tworzymy pole na nazwę */
                String name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                TextView nameView = new TextView(this);
                nameView.setText(name);

                /* Ustawiamy parametry dla widoku */
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                nameView.setLayoutParams(params);

                /* Dodajemy nowy tekst do layoutu */
                contentMain.addView(nameView);

                if (hasPhone.equalsIgnoreCase("1")) {
                    Cursor phones = getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,
                            null, null);
                    phones.moveToFirst();
                    number = phones.getString(phones.getColumnIndex("data1"));

                    /* Tworzymy nowe pole tekstowe na numer */
                    TextView numberView = new TextView(this);
                    numberView.setText(number);

                    /* Ustawiamy parametry */
                    numberView.setLayoutParams(params);

                    /* Dodajemy do layoutu */
                    contentMain.addView(numberView);

                    /* Dodajemy przycisk do dzwonienia */
                    Button callBtn = new Button(this);
                    Button sendSmsBtn = new Button(this);
                    Button editSmsBtn = new Button(this);
                    callBtn.setText(R.string.button_call);
                    callBtn.setLayoutParams(params);
                    sendSmsBtn.setText(R.string.button_send_sms);
                    sendSmsBtn.setLayoutParams(params);
                    editSmsBtn.setText(R.string.button_edit_sms);
                    editSmsBtn.setLayoutParams(params);

                    /* Obsluga klikniecia w przycisk */
                    callBtn.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            callNumber();
                        }
                    });
                    sendSmsBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            sendSms();
                        }
                    });
                    editSmsBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            editSms();
                        }
                    });

                    /* Dodajemy do layoutu */
                    contentMain.addView(callBtn);
                    contentMain.addView(sendSmsBtn);
                    contentMain.addView(editSmsBtn);
                }

            }
        }
    }

    /* Pobieranie zdjęcia */
    private boolean takePhoto() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);

            return false;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            return true;
        }

        return false;
    }

    private boolean loadPhoto() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PICK_FROM_GALLERY);
        }

        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, REQUEST_LOAD_PHOTO);
        return true;
    }

    /* Pobieranie kontaktu */
    private boolean fetchContact() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    MY_PERMISSIONS_REQUEST_CONTACTS);

            return false;
        }

        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, REQUEST_CONTACT_DATA);

        return true;
    }

    /* Dzwonienie do aktualnie wybranej osoby */
    private boolean callNumber() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE},
                    MY_PERMISSIONS_REQUEST_PHONE);

            return false;
        }

        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
        startActivity(intent);

        return true;
    }

    private boolean sendSms() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    MY_PERMISSIONS_REQUEST_SMS);

            return false;
        }
        SmsManager manager = SmsManager.getDefault();
        manager.sendTextMessage(number, null, "Nie dam rady dojechać", null, null);
        return true;
    }

    private boolean editSms() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    MY_PERMISSIONS_REQUEST_SMS);

            return false;
        }
        Intent smsEditor = new Intent(Intent.ACTION_VIEW);
        smsEditor.setData(Uri.fromParts("sms", number, null));
        smsEditor.putExtra("sms_body", "Nie dam rady dojechać");
        startActivity(smsEditor);
        return true;
    }
}