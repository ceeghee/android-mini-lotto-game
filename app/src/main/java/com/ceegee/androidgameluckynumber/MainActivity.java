package com.ceegee.androidgameluckynumber;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ceegee.androidgameluckynumber.Common.Common;

import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    TextView txt_count, txt_result, txt_money, txt_status, txt_score;
    Button btn_submit, btn_disconnect;
    EditText edt_place_money, edt_place_value;
    Socket socket;

    Boolean isDisconnect = false, isBet = false, canPlay = true;
    int resultNumber = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edt_place_money = findViewById(R.id.edt_money_bet_value);
        edt_place_value = findViewById(R.id.edt_bet_value);

        txt_count = findViewById(R.id.txt_count);
        txt_result = findViewById(R.id.txt_result);
        txt_money = findViewById(R.id.txt_money);
        txt_score = findViewById(R.id.txt_score);
        txt_status = findViewById(R.id.txt_status);
        btn_submit = findViewById(R.id.btn_submit);
        btn_disconnect = findViewById(R.id.btn_disconnect);
        btn_submit.setOnClickListener(this);
        btn_disconnect.setOnClickListener(this);
        _Connect();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_disconnect:
                Disconnect();
                break;
            case R.id.btn_submit:
                Submit();
                break;
            default:
                break;

        }
    }

    public void Disconnect() {
        if (!isDisconnect) {
            socket.disconnect();
            btn_disconnect.setText("CONNECT");
            isDisconnect = true;
        } else {
            _Connect();
            isDisconnect = false;
            btn_disconnect.setText("DISCONNECT");
        }
    }

    public void Submit() {
        try {
            if (canPlay) {
                if (!isBet) {
                    int money_bet_value = Integer.parseInt(edt_place_money.getText().toString());
                    if (Common.score >= money_bet_value) {
                        // Create Json Object to Send
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("money", Integer.parseInt(edt_place_money.getText().toString()));
                        jsonObject.put("betValue", Integer.parseInt(edt_place_value.getText().toString()));
                        socket.emit("client_send_money", jsonObject);

                        Common.score -= money_bet_value;
                        txt_score.setText(String.valueOf(Common.score));
                        isBet = true; // prevent another betting in current round, i.e wait till next round

                    }
                } else {
                    MakeToast("You Already Placed a Bet in this Turn");
                }
            } else {
                MakeToast("Please wait for next turn");
            }
        } catch (Exception ex) {
            MakeToast(ex.getMessage());
        }
    }

    //Connect to Socket Server

    public void _Connect() {
        try {
            socket = IO.socket("http://192.168.43.123:3000");
            socket.on(socket.EVENT_CONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MakeToast("Connected");
                        }
                    });
                }
            });
            socket.connect();
            registerAllEventForGame();
        } catch (Exception ex) {
            MakeToast(ex.getMessage());
        }
    }

    private void registerAllEventForGame() {
        socket.on("broadcast", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                // Retrieve timer
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txt_count.setText(new StringBuilder("Timer: ").append(args[0]));
                        txt_result.setText("");
                        txt_status.setText("");
                    }
                });
            }
        });

        socket.on("wait_before_restart", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                canPlay = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txt_status.setText("Please wait for" + args[0] + " seconds");
                        txt_count.setText("Wait...");
                        isBet = false;
                    }
                });
            }
        });

        socket.on("money_send", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                canPlay = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txt_money.setText(String.valueOf(args[0]));
                    }
                });
            }
        });

        socket.on("restart", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                canPlay = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txt_result.setVisibility(View.GONE);
                    }
                });
            }
        });

        socket.on("result", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                resultNumber = Integer.parseInt(args[0].toString());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txt_result.setVisibility(View.VISIBLE);
                        txt_result.setText(new StringBuilder("Result : ").append(args[0].toString()));
                    }
                });
            }
        });

        socket.on("reward", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //User win
                        txt_result.setText(new StringBuilder("Result: ").append(resultNumber).append(" | Congrats! You WON ")
                                .append(args[0])
                                .append(" USD "));
                        txt_result.setBackgroundResource(R.drawable.win_textview_bg);
                        Log.d("Reward", "You Receive " + args[0] + " USD");
                        if(args[0] != null)
                        Common.score += Integer.parseInt(args[0].toString());

                        txt_score.setText(String.valueOf(Common.score));
                    }
                });
            }
        });

        socket.on("lose", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //User Lost
                        int money = Integer.parseInt(args[0].toString());
                        txt_result.setText(new StringBuilder("Result: ")
                                .append(resultNumber)
                                .append(" | ")
                                .append(" You Lost: ")
                                .append(money)
                                .append(" USD "));
                        txt_result.setBackgroundResource(R.drawable.lose_textview_bg);
                    }
                });
            }
        });

        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //User win
                        txt_result.setText("DISCONNECT");
                        txt_count.setText("DISCONNECT");
                        txt_money.setText("DISCONNECT");
                    }
                });
            }
        });


    }

    private void MakeToast(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

}
