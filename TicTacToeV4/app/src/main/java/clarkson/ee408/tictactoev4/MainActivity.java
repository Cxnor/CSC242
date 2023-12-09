package clarkson.ee408.tictactoev4;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import clarkson.ee408.tictactoev4.client.AppExecutors;
import clarkson.ee408.tictactoev4.client.SocketClient;
import clarkson.ee408.tictactoev4.socket.GamingResponse;
import clarkson.ee408.tictactoev4.socket.Request;
import clarkson.ee408.tictactoev4.socket.Response;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MAIN_ACTIVITY";

    private TicTacToe tttGame;
    private Button [][] buttons;
    private TextView status;
    private Gson gson;
    private Handler handler;
    private Runnable refresh;

    private boolean shouldRequestMove;

    /**
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int player = getIntent().getIntExtra("player", 1);
        tttGame = new TicTacToe(player);
        shouldRequestMove = true;
        buildGuiByCode();
        gson = new GsonBuilder().serializeNulls().create();
        updateTurnStatus();
        handler = new Handler();
        refresh = () -> {
            if(shouldRequestMove)requestMove();
            handler.postDelayed(refresh, 500);
        };
        handler.post(refresh);
    }

    /**
     *
     */
    public void buildGuiByCode( ) {
        // Get width of the screen
        Point size = new Point( );
        getWindowManager( ).getDefaultDisplay( ).getSize( size );
        int w = size.x / TicTacToe.SIDE;

        // Create the layout manager as a GridLayout
        GridLayout gridLayout = new GridLayout( this );
        gridLayout.setColumnCount( TicTacToe.SIDE );
        gridLayout.setRowCount( TicTacToe.SIDE + 2 );

        // Create the buttons and add them to gridLayout
        buttons = new Button[TicTacToe.SIDE][TicTacToe.SIDE];
        ButtonHandler bh = new ButtonHandler( );

//        GridLayout.LayoutParams bParams = new GridLayout.LayoutParams();
//        bParams.width = w - 10;
//        bParams.height = w -10;
//        bParams.bottomMargin = 15;
//        bParams.rightMargin = 15;

        gridLayout.setUseDefaultMargins(true);

        for( int row = 0; row < TicTacToe.SIDE; row++ ) {
            for( int col = 0; col < TicTacToe.SIDE; col++ ) {
                buttons[row][col] = new Button( this );
                buttons[row][col].setTextSize( ( int ) ( w * .2 ) );
                buttons[row][col].setOnClickListener( bh );
                GridLayout.LayoutParams bParams = new GridLayout.LayoutParams();
//                bParams.width = w - 10;
//                bParams.height = w -40;

                bParams.topMargin = 0;
                bParams.bottomMargin = 10;
                bParams.leftMargin = 0;
                bParams.rightMargin = 10;
                bParams.width=w-10;
                bParams.height=w-10;
                buttons[row][col].setLayoutParams(bParams);
                gridLayout.addView( buttons[row][col]);
//                gridLayout.addView( buttons[row][col], bParams );
            }
        }

        // set up layout parameters of 4th row of gridLayout
        status = new TextView( this );
        GridLayout.Spec rowSpec = GridLayout.spec( TicTacToe.SIDE, 2 );
        GridLayout.Spec columnSpec = GridLayout.spec( 0, TicTacToe.SIDE );
        GridLayout.LayoutParams lpStatus
                = new GridLayout.LayoutParams( rowSpec, columnSpec );
        status.setLayoutParams( lpStatus );

        // set up status' characteristics
        status.setWidth( TicTacToe.SIDE * w );
        status.setHeight( w );
        status.setGravity( Gravity.CENTER );
        status.setBackgroundColor( Color.GREEN );
        status.setTextSize( ( int ) ( w * .15 ) );
        status.setText( tttGame.result( ) );

        gridLayout.addView( status );

        // Set gridLayout as the View of this Activity
        setContentView( gridLayout );
    }

    /**
     *
     * @param row
     * @param col
     */
    public void update( int row, int col ) {
        int play = tttGame.play( row, col );
        if( play == 1 )
            buttons[row][col].setText( "X" );
        else if( play == 2 )
            buttons[row][col].setText( "O" );
        if( tttGame.isGameOver( ) ) {
            status.setBackgroundColor( Color.RED );
            enableButtons( false );
            status.setText( tttGame.result( ) );
            shouldRequestMove = false;
            showNewGameDialog( );	// offer to play again
        } else {
            updateTurnStatus();
        }
    }

    /**
     *
     * @param enabled
     */
    public void enableButtons( boolean enabled ) {
        for( int row = 0; row < TicTacToe.SIDE; row++ )
            for( int col = 0; col < TicTacToe.SIDE; col++ )
                buttons[row][col].setEnabled( enabled );
    }

    /**
     *
     */
    public void resetButtons( ) {
        for( int row = 0; row < TicTacToe.SIDE; row++ )
            for( int col = 0; col < TicTacToe.SIDE; col++ )
                buttons[row][col].setText( "" );
    }

    /**
     *
     */
    public void showNewGameDialog( ) {
        AlertDialog.Builder alert = new AlertDialog.Builder( this );
        alert.setTitle(tttGame.result());
        alert.setMessage( "Do you want to play again?" );
        PlayDialog playAgain = new PlayDialog( );
        alert.setPositiveButton( "YES", playAgain );
        alert.setNegativeButton( "NO", playAgain );
        alert.show( );
    }


    private class ButtonHandler implements View.OnClickListener {
        /**
         *
         * @param v
         */
        public void onClick( View v ) {
            Log.d("button clicked", "button clicked");

            for( int row = 0; row < TicTacToe.SIDE; row ++ )
                for( int column = 0; column < TicTacToe.SIDE; column++ )
                    if( v == buttons[row][column] ) {
                        sendMove((row * TicTacToe.SIDE) + column);
                        update(row, column);
                    }
        }
    }

    private class PlayDialog implements DialogInterface.OnClickListener {
        /**
         *
         * @param dialog
         * @param id
         */
        public void onClick( DialogInterface dialog, int id ) {
            if( id == -1 ) /* YES button */ {
                tttGame.resetGame( );
                enableButtons( true );
                resetButtons( );
                status.setBackgroundColor( Color.GREEN );
                status.setText( tttGame.result( ) );
                tttGame.setPlayer(tttGame.getPlayer() == 1 ? 2:1);
                updateTurnStatus();
                shouldRequestMove = true;
            }
            else if( id == -2 ) // NO button
                MainActivity.this.finish( );
        }
    }

    /**
     *
     */
    private void updateTurnStatus() {
        if (tttGame.getPlayer() == tttGame.getTurn()) {
            // It's the current player's turn
            status.setText("Your Turn");
            enableButtons(true);
        } else {
            status.setText("Waiting for Opponent");
            enableButtons(false);
        }
    }

    /**
     *
     */
    private void requestMove() {
        Request request = new Request();
        request.setType(Request.RequestType.REQUEST_MOVE);

        AppExecutors.getInstance().networkIO().execute(() -> {
            GamingResponse response = SocketClient.getInstance().sendRequest(request, GamingResponse.class);

            AppExecutors.getInstance().mainThread().execute(() -> {
                if (response == null) {
                    Toast.makeText(getApplicationContext(), "Network Error", Toast.LENGTH_LONG).show();
                } else if (response.getStatus() == Response.ResponseStatus.FAILURE) {
                    Toast.makeText(getApplicationContext(), response.getMessage(), Toast.LENGTH_LONG).show();
                } else if (!response.isActive()) {
                    status.setBackgroundColor(Color.RED);
                    enableButtons(false);
                    status.setText(response.getMessage());
                    shouldRequestMove = false;
                    tttGame = null;
                } else if (response.getMove() != -1) {
                    int row = response.getMove() / 3;
                    int col = response.getMove() % 3;
                    update(row, col);
                }
            });
        });
    }

    /**
     *
     * @param move
     */
    private void sendMove(int move) {
        Request request = new Request();
        request.setType(Request.RequestType.SEND_MOVE);
        request.setData(gson.toJson(move));

        Log.e(TAG, "Sending Move: " + move);
        AppExecutors.getInstance().networkIO().execute(()-> {
            Response response = SocketClient.getInstance().sendRequest(request, Response.class);
            AppExecutors.getInstance().mainThread().execute(()-> {
                if(response == null) {
                    Toast.makeText(this, "Couldn't send game move", Toast.LENGTH_SHORT).show();
                } else if(response.getStatus() == Response.ResponseStatus.FAILURE) {
                    Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                }else{ //Success
                    Log.e(TAG, "Move sent");
                }
            });
        });
    }

    /**
     *
     */
    private void abortGame() {
        Request request = new Request();
        request.setType(Request.RequestType.ABORT_GAME);
        AppExecutors.getInstance().networkIO().execute(() -> {
            Response response = SocketClient.getInstance().sendRequest(request, Response.class);
            AppExecutors.getInstance().mainThread().execute(() -> {
                if (response == null) {
                    Toast.makeText(getApplicationContext(), "Error with Network", Toast.LENGTH_LONG).show();
                } else if (response.getStatus() == Response.ResponseStatus.FAILURE) {
                    Toast.makeText(getApplicationContext(), response.getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Game Aborted", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     *
     */
    private void completeGame() {
        Request request = new Request();
        request.setType(Request.RequestType.COMPLETE_GAME);
        AppExecutors.getInstance().networkIO().execute(() -> {
            Response response = SocketClient.getInstance().sendRequest(request, Response.class);
            AppExecutors.getInstance().mainThread().execute(() -> {
                if (response == null) {
                    Toast.makeText(getApplicationContext(), "Error with Network", Toast.LENGTH_LONG).show();
                } else if (response.getStatus() == Response.ResponseStatus.FAILURE) {
                    Toast.makeText(getApplicationContext(), response.getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Game Completed", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     *
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(refresh);
        if(tttGame != null) {
            if (tttGame.isGameOver()) {
                completeGame();
            } else {
                abortGame();
            }
        }
    }
}