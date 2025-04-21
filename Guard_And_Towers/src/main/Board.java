package main;

import java.awt.*;

public class Board {
    final int MAX_COL = 7;
    final int MAX_ROW = 7;
    public static final int SQUARE_SIZE = 100;
    public static final int HALF_SQUARE_SIZE = SQUARE_SIZE/2;

    public void draw(Graphics2D g2) {
        boolean c = true;

        for (int row = 0; row < MAX_ROW; row++) {
            for (int col = 0; col < MAX_COL; col++) {
                if (c) {
                    g2.setColor(new Color(210,165,125));
                }
                else {
                    g2.setColor(new Color(175,115,70));
                }
                c = !c;
                g2.fillRect(col * SQUARE_SIZE, row * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
            }
        }
        g2.setColor(new Color(86, 173, 72));
        g2.fillRect(3 * SQUARE_SIZE, 0 * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
        g2.fillRect(3 * SQUARE_SIZE, 6 * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
    }
}
