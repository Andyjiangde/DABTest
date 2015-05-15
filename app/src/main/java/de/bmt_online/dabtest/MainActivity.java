package de.bmt_online.dabtest;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import org.w3c.dom.Text;


public class MainActivity extends ActionBarActivity{

    EditText editText;
    Button button;
    TextView textView;

    private CharSequence arguments;
    private int START_REQ_CODE=123;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText=(EditText)findViewById(R.id.edittext1);
        textView=(TextView)findViewById(R.id.textView1);

        button=(Button)findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                arguments=editText.getText();

                try {
                    startActivityForResult(new
                            Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse("iqsrc://" + arguments)), START_REQ_CODE);
                }catch (ActivityNotFoundException e){
                    e.printStackTrace();
                }

            }
        });


    }


    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(requestCode ==START_REQ_CODE){
                    if(resultCode ==RESULT_OK){

                            Uri contactUri=data.getData();



                        InputStream input=null;

                        try {

                            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                            StrictMode.setThreadPolicy(policy);

                            int serverPort = 36609;

                            Socket socket = new Socket("127.0.0.1", serverPort);
                            input= socket.getInputStream();

                            Socket socketComm = new Socket("192.168.200.135", 5566);

                            OutputStream outputStream=socketComm.getOutputStream();


                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = input.read(buf)) != -1) {
                                outputStream.write(buf, 0, len);

                                textView.setText("Verbindung abgebrochen!");

                            }

//                            final RTLSDRSource rtlSDRSource =
//                                    new RTLSDRSource(
//                                            "127.0.0.1",
//                                            36609);
//                            rtlSDRSource.sendCommand(RTLSDRSource.Commands.Frequency,178352000);
//                            rtlSDRSource.sendCommand(RTLSDRSource.Commands.SampleRate,2048000);
//                            byte[] packet =
//                                    rtlSDRSource.getData();
//                            int len;
//                            while ((len = input.read(packet)) != -1) {
//                                outputStream.write(packet, 0, len);
//
//                                textView.setText("Verbindung abgebrochen!");
//
//                            }

                            outputStream.close();
                            socket.close();


                        }
                        catch(UnknownHostException ex) {
                            ex.printStackTrace();


                        }
                        catch(IOException e){
                           e.printStackTrace();

                        } finally {
                            if(input !=null) {

                                try{
                                    input.close();

                                }catch (IOException e){
                                    e.printStackTrace();
                                }

                            }


                        }

                    }else {
                        try{
                            switch (data.getIntExtra("marto.rtl_tcp_andro.RtlTcpExceptionId",-1)){
                                case 0:
                                    //TODO
                                    break;
                                case 1:
                                    break;
                                case 2:
                                    break;
                                case 3:
                                    break;
                                case 4:
                                    break;
                                default:

                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

    }





    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
