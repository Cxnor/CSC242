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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;

public class MainActivity extends AppCompatActivity {
    private TicTacToe tttGame;
    private Button [][] buttons;
    private TextView status;
    private Gson gson;
    private Handler handler = new Handler();
    private boolean shouldRequestMove = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tttGame = new TicTacToe();
        buildGuiByCode();
        gson = new Gson();
        updateTurnStatus();

        // Initialize the handler to request moves every second (adjust the interval as needed)
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (shouldRequestMove) {
                    requestMove();
                }

                // Request moves again after a delay (every second)
                handler.postDelayed(this, 1000); // 1000 milliseconds = 1 second
            }
        }, 1000); // Initial delay before the first request (1 second)
    }

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

    public void update( int row, int col ) {
        int play = tttGame.play( row, col );
        if( play == 1 )
            buttons[row][col].setText( "X" );
        else if( play == 2 )
            buttons[row][col].setText( "O" );
        if( tttGame.isGameOver( ) ) {
            status.setBackgroundColor( Color.RED );
            enableButtons( false );
            updateTurnStatus();
            status.setText( tttGame.result( ) );
            showNewGameDialog( );	// offer to play again
        }
    }

    public void enableButtons( boolean enabled ) {
        for( int row = 0; row < TicTacToe.SIDE; row++ )
            for( int col = 0; col < TicTacToe.SIDE; col++ )
                buttons[row][col].setEnabled( enabled );
    }

    public void resetButtons( ) {
        for( int row = 0; row < TicTacToe.SIDE; row++ )
            for( int col = 0; col < TicTacToe.SIDE; col++ )
                buttons[row][col].setText( "" );
    }

    public void showNewGameDialog( ) {
        AlertDialog.Builder alert = new AlertDialog.Builder( this );
        alert.setTitle(tttGame.result());
        alert.setTitle( "This is fun" );
        alert.setMessage( "Do you want to play again?" );
        PlayDialog playAgain = new PlayDialog( );
        alert.setPositiveButton( "YES", playAgain );
        alert.setNegativeButton( "NO", playAgain );
        alert.show( );
    }

    private class ButtonHandler implements View.OnClickListener {
        public void onClick( View v ) {
            Log.d("button clicked", "button clicked");

            for( int row = 0; row < TicTacToe.SIDE; row ++ )
                for( int column = 0; column < TicTacToe.SIDE; column++ )
                    if( v == buttons[row][column] )
                        update( row, column );
        }
    }

    private class PlayDialog implements DialogInterface.OnClickListener {
        public void onClick(DialogInterface dialog, int id) {
            if (id == -1) /* YES button */ {
                // Switch the starting player for the next game
                if (tttGame.getPlayer() == 1) {
                    tttGame.setPlayer(2); // Set player to 2 for the next game
                } else {
                    tttGame.setPlayer(1); // Set player to 1 for the next game
                }

                tttGame.resetGame();
                enableButtons(true);
                resetButtons();
                status.setBackgroundColor(Color.GREEN);
                updateTurnStatus(); // Call this to set the initial status and button states.
            } else if (id == -2) // NO button
                MainActivity.this.finish();
        }
    }
    private void updateTurnStatus() {
        if (tttGame.getPlayer() == tttGame.getTurn()) {
            // It's the current player's turn
            status.setText("Your Turn");
            shouldRequestMove = false; // Disable request move
            enableButtons(true); // Enable buttons for the current player
        } else {
            // It's the opponent's turn
            status.setText("Waiting for Opponent");
            shouldRequestMove = true; // Enable request move
            enableButtons(false); // Disable buttons for the opponent
        }
    }
    private void requestMove() {
        // Create a request object with the type "REQUEST_MOVE"
        Request request = new Request(RequestType.REQUEST_MOVE);

        // Use the SocketClient to send the request in the networkIO thread
        Thread networkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Use the SocketClient class to send the request to the server
                // Replace this with your actual networking code
                // Example: Response response = socketClient.sendRequest(request, Response.class);

                // Check if a valid move is received in the response
                if (response != null && response.isValidMove()) {
                    // Update the game board in the main thread using runOnUiThread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Call the update() function with the received move
                            int row = response.getRow();
                            int col = response.getCol();
                            update(row, col);
                        }
                    });
                }
            }
        });
        networkThread.start();
    }
    private void sendMove(Move move) {
        // Create a request object with the type "SEND_MOVE" and set the data attribute with a serialized move
        Request request = new Request(RequestType.SEND_MOVE);
        request.setData(gson.toJson(move));
    }


    private void requestMove() {
    }


}