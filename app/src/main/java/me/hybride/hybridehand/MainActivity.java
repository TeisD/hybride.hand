package me.hybride.hybridehand;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.nfc.tech.TagTechnology;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * HYBRIDE.ME/NS - http://hybride.me/ns
 *
 * @author Teis De Greve
 * Loosely based on the online tutorial on NFC by Ralf Wondratschek
 *
 */
@TargetApi(19)
public class MainActivity extends Activity implements NfcAdapter.ReaderCallback{

    public static final String TAG = "NfcDemo";

    private TextView mTextView;
    private NfcAdapter nfcAdapter;
    private PendingIntent nfcPendingIntent;
    private Timer t;
    ScheduledExecutorService exec;
    Future<?> future;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.textView_io);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            // Stop here, we definitely need NFC
            mTextView.setText("Dit apparaat heeft geen NFC-sensor.");
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;

        }

        if (!nfcAdapter.isEnabled()) {
            mTextView.setText("NFC is uitgeschakeld. Schakel het in in de systeeminstellingen");
        } else {
            //mTextView.setText(R.string.explanation);
            t = new Timer();
            exec = Executors.newSingleThreadScheduledExecutor();
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }
        //FLAG_ACTIVITY_SINGLE_TOP: If set, the activity will not be launched if it is already running at the top of the history stack.
        nfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // disable sound if possible.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // this should enable the reader mode for NFCA tags without the sound, but it only works once and then crashes the NFC reader so a reboot is required
            //nfcAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS, null);
            nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, null, null);
        } else {
            nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, null, null);

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //nfcAdapter.disableReaderMode(this);
            nfcAdapter.disableForegroundDispatch(this);
        } else {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    public void onNewIntent(Intent intent) { // this method is called when an NFC tag is scanned
        String action = intent.getAction();
        switch (action) {
            case NfcAdapter.ACTION_NDEF_DISCOVERED:
                //mTextView.setText("NDEF");
                break;
            case NfcAdapter.ACTION_TECH_DISCOVERED:
                //mTextView.setText("TECH");
                break;
            case NfcAdapter.ACTION_TAG_DISCOVERED:
                //mTextView.setText("TAG");
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                System.out.println(tag.toString());
                startGlove(tag);
                break;
        }
    }

    private void startGlove(Tag tag) {
        String tech = tag.getTechList()[0];
        System.out.println(tech);
        switch (tech) {
            case "android.nfc.tech.NfcA":
                startVibrating(NfcA.get(tag));
                break;
            case "android.nfc.tech.NfcB":
                startVibrating(NfcB.get(tag));
                break;
            case "android.nfc.tech.NfcF":
                startVibrating(NfcF.get(tag));
                break;
            case "android.nfc.tech.NfcV":
                startVibrating(NfcV.get(tag));
                break;
            case "android.nfc.tech.IsoDep":
                startVibrating(IsoDep.get(tag));
                break;
            case "android.nfc.tech.Ndef":
                startVibrating(Ndef.get(tag));
                break;
            case "android.nfc.tech.MifareClassic":
                startVibrating(MifareClassic.get(tag));
                break;
            case "android.nfc.tech.MifareUltralight":
                startVibrating(MifareUltralight.get(tag));
                break;
            // needs min API level 17
            // else if (tech.equals("android.nfc.tech.NfcBarcode")){
            //    startVibrating(NfcBarcode.get(tag));
            case "android.nfc.tech.NdefFormatable":
                startVibrating(NdefFormatable.get(tag));
                break;
        }
    }

    /*
     * Check every 100ms if the tag is in range. If so, vibrate
     */
    private void startVibrating(final TagTechnology t) {
        System.out.println("startVibrating");
        try {
            t.connect();
            long[] pattern = {0, 200};
            vibrator.vibrate(pattern, 0);
            Runnable task1 = new Runnable() {
                public void run() {
                    if(t.isConnected()){
                        //System.out.println(new java.util.Date());
                    } else{
                        vibrator.cancel();
                        future.cancel(true);
                    }
                }
            };
            future = exec.scheduleAtFixedRate(task1, 0, 100, TimeUnit.MILLISECONDS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        System.out.println("Tag discovered in reader mode");
        startGlove(tag);
    }
}