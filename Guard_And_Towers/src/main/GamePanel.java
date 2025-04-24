package main;

import piece.Guard;
import piece.Tower;
import piece.Piece;

import javax.swing.JPanel;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class GamePanel extends JPanel implements Runnable {
    public static final int WIDTH = 1000;
    public static final int HEIGHT = 700;
    final int FPS = 60;
    Thread gameThread;
    Board board = new Board();
    Mouse mouse = new Mouse();

    //Enter your FEN-String below to get a custom starting position (don't enter a string to get the normal starting position)
    //String FEN;
    String FEN = "r1r11RG1r1r1/2r11r12/3r13/7/3b13/2b11b12/b1b11BG1b1b1 r";

    //Pieces
    public static ArrayList<Piece> pieces = new ArrayList<>();
    public static ArrayList<Piece> simPieces = new ArrayList<>();
    Piece activeP;

    //Color
    public static final int BLUE = 0;
    public static final int RED = 1;
    int currentColor;

    //Booleans
    boolean canMove;
    boolean validSquare;
    boolean gameover = false;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH,HEIGHT));
        setBackground(Color.black);
        addMouseMotionListener(mouse);
        addMouseListener(mouse);

        setPieces();
        copyPieces(pieces, simPieces);
    }

    public void launchGame() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void setPieces() {
        if (FEN == null) { //normal starting position
            currentColor = RED;

            //Blue Team
            pieces.add(new Guard(BLUE,3,6, 1));
            pieces.add(new Tower(BLUE,0,6, 1));
            pieces.add(new Tower(BLUE,1,6, 1));
            pieces.add(new Tower(BLUE,2,5, 1));
            pieces.add(new Tower(BLUE,3,4, 1));
            pieces.add(new Tower(BLUE,4,5, 1));
            pieces.add(new Tower(BLUE,5,6, 1));
            pieces.add(new Tower(BLUE,6,6, 1));

            //Red Team
            pieces.add(new Guard(RED,3,0, 1));
            pieces.add(new Tower(RED,0,0, 1));
            pieces.add(new Tower(RED,1,0, 1));
            pieces.add(new Tower(RED,2,1, 1));
            pieces.add(new Tower(RED,3,2, 1));
            pieces.add(new Tower(RED,4,1, 1));
            pieces.add(new Tower(RED,5,0, 1));
            pieces.add(new Tower(RED,6,0, 1));
        }
        else { //custom starting position
            //check if FEN-String is valid (probably doesn't filter out all incorrect FEN-Strings)
            if (FEN.charAt(FEN.length() - 2) != ' ') {
                throw new IllegalArgumentException("incorrect FEN-String: missing spacebar at the end");
            }

            if (FEN.charAt(FEN.length() - 1) == 'r') {
                currentColor = RED;
            }
            else if (FEN.charAt(FEN.length() - 1) == 'b') {
                currentColor = BLUE;
            }
            else {
                throw new IllegalArgumentException("incorrect FEN-String: which player's turn is it?");
            }

            String newFEN = FEN.substring(0, FEN.length() - 2);
            checkFENString(newFEN);

            //turn FEN-String into correct position on the board
            int col = 0;
            int row = 0;
            String tower = "no";
            boolean guard = false;

            try {
                for (char c : newFEN.toCharArray()) {
                    if (guard) {
                        guard = false;
                        col++;
                    }
                    else {
                        if (c == 'r') {
                            tower = "red";
                        }
                        else if (c == 'b') {
                            tower = "blue";
                        }
                        else if (c == 'R') {
                            guard = true;
                            pieces.add(new Guard(RED,col,row, 1));
                        }
                        else if (c == 'B') {
                            guard = true;
                            pieces.add(new Guard(BLUE,col,row, 1));
                        }
                        else if (c == '/') {
                            if (col != 7) {
                                throw new IllegalArgumentException("incorrect FEN-String: wrong number of squares in row: " + (row + 1));
                            }
                            col = 0;
                            row++;
                        }
                        else if (tower.equals("no")) {
                            col += c - '0';
                        }
                        else if (tower.equals("red")){
                            int number = c - '0';
                            pieces.add(new Tower(RED,col,row,number));
                            tower = "no";
                            col++;
                        }
                        else if (tower.equals("blue")){
                            int number = c - '0';
                            pieces.add(new Tower(BLUE,col,row,number));
                            tower = "no";
                            col++;
                        }
                    }
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("incorrect FEN-String");
            }
        }
        //check for illegal amount of towers (more than 7)
        int redTowers = 0;
        int blueTowers = 0;
        for (Piece p : pieces) {
            if (p instanceof Tower) {
                if (p.color == RED) {
                    redTowers += p.Height;
                }
                else {
                    blueTowers += p.Height;
                }
            }
        }
        if (redTowers > 7 || blueTowers > 7) {
            throw new IllegalArgumentException("incorrect FEN-String: too many towers");
        }
    }

    private void copyPieces(ArrayList<Piece> source, ArrayList<Piece> target) {
        target.clear();
        target.addAll(source);
    }

    @Override
    public void run() {
        //Game Loop
        double drawInterval = 1000000000/FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (gameThread != null) {
            currentTime = System.nanoTime();

            delta += (currentTime - lastTime)/drawInterval;
            lastTime = currentTime;

            if(delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    private void update() {
        if (!gameover) {
            if (mouse.pressed) {
                if(activeP == null) {
                    for (Piece piece : simPieces) {
                        if (piece.color == currentColor && piece.col == mouse.x / Board.SQUARE_SIZE && piece.row == mouse.y / Board.SQUARE_SIZE) {
                            activeP = piece;
                        }
                    }
                }
                else {
                    simulate();
                }
            }

            if (!mouse.pressed) {
                if (activeP != null) {
                    if (validSquare) {
                        int prevHeight = activeP.Height;
                        int steps = Math.abs(activeP.preRow - activeP.row) + Math.abs(activeP.preCol - activeP.col);
                        activeP.Height = steps;

                        if (prevHeight > steps) {
                            simPieces.add(new Tower(activeP.color, activeP.preCol, activeP.preRow, prevHeight - steps));
                        }

                        if (activeP.hittingP != null && activeP.color == activeP.hittingP.color && activeP instanceof Tower && activeP.hittingP instanceof Tower) {
                            activeP.Height += activeP.hittingP.Height;
                            simPieces.remove(activeP.hittingP.getIndex());
                        }

                        activeP.setImage();
                        copyPieces(simPieces, pieces);
                        activeP.updatePosition();

                        gameover = gameIsOver();

                        if (!gameover) {
                            changePlayer();
                        }
                    }
                    else {
                        copyPieces(pieces, simPieces);
                        activeP.resetPosition();
                    }
                    activeP = null;
                }
            }
        }

    }

    private void simulate() {
        canMove = false;
        validSquare = false;

        copyPieces(pieces, simPieces);

        activeP.x = mouse.x - Board.HALF_SQUARE_SIZE;
        activeP.y = mouse.y - Board.HALF_SQUARE_SIZE;
        activeP.col = activeP.getCol(activeP.x);
        activeP.row = activeP.getRow(activeP.y);

        if (activeP.canMove(activeP.col, activeP.row)) {
            canMove = true;

            if (activeP.hittingP != null) {
                if (activeP.hittingP.color != activeP.color) {
                    if (activeP instanceof Guard || activeP.Height >= activeP.hittingP.Height) { //mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm
                        simPieces.remove(activeP.hittingP.getIndex());
                    }
                }
            }

            validSquare = true;
        }
    }

    private void changePlayer() {
        if (currentColor == BLUE) {
            currentColor = RED;
        }
        else {
            currentColor = BLUE;
        }
        activeP = null;
    }

    private boolean gameIsOver() {
        int guardAmount = 0;
        for (Piece piece : pieces) {
            if (piece instanceof Guard) {
                if (piece.color == RED && piece.col == 3 && piece.row == 6) {
                    return true;
                }
                if (piece.color == BLUE && piece.col == 3 && piece.row == 0) {
                    return true;
                }
                guardAmount++;
            }
        }
        if (guardAmount != 2) {
            return true;
        }
        return false;
    }

    public void paintComponent (Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        //Board
        board.draw(g2);

        //Pieces
        for (Piece p: simPieces) {
            p.draw(g2);
        }

        if (activeP != null) {
            if (canMove) {
                g2.setColor(Color.white);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                g2.fillRect(activeP.col * Board.SQUARE_SIZE, activeP.row * Board.SQUARE_SIZE, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }

            activeP.draw(g2);
        }

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(new Font("Arial", Font.PLAIN, 40));
        g2.setColor(Color.white);

        if (currentColor == BLUE) {
            g2.drawString("Blue's turn", 750, 550);
        }
        else {
            g2.drawString("Red's turn", 750, 170);
        }

        if (gameover) {
            String s = "";
            if (currentColor == BLUE) {
                s = "Blue Wins!";
            }
            else {
                s = "Red Wins!";
            }
            g2.setFont(new Font("Arial", Font.PLAIN, 50));
            g2.setColor(Color.white);
            g2.drawString(s, 730, 350);
        }
    }

    private void checkFENString(String newFEN) {
        if (!newFEN.matches("[rbRBG1234567/]+")) {
            throw new IllegalArgumentException("incorrect FEN-String: wrong characters in String");
        }

        long count = newFEN.chars().filter(c -> c == '/').count();
        if (count != 6) {
            throw new IllegalArgumentException("incorrect FEN-String: incorrect number of '/'");
        }

        count = newFEN.chars().filter(c -> c == 'R').count();
        if (count != 1) {
            throw new IllegalArgumentException("incorrect FEN-String: incorrect number of 'R'");
        }

        count = newFEN.chars().filter(c -> c == 'B').count();
        if (count != 1) {
            throw new IllegalArgumentException("incorrect FEN-String: incorrect number of 'B'");
        }

        count = newFEN.chars().filter(c -> c == 'G').count();
        if (count != 2) {
            throw new IllegalArgumentException("incorrect FEN-String: incorrect number of 'G'");
        }

        if (!newFEN.contains("BG") || newFEN.indexOf("BG") != newFEN.lastIndexOf("BG")) {
            throw new IllegalArgumentException("incorrect FEN-String: wrong amount of blue guards");
        }
        if (!newFEN.contains("RG") || newFEN.indexOf("RG") != newFEN.lastIndexOf("RG")) {
            throw new IllegalArgumentException("incorrect FEN-String: wrong amount of red guards");
        }
    }
}
