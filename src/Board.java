package com.zetcode;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.io.File;

public class Board extends JPanel {

    private final int BOARD_WIDTH = 10;
    private final int BOARD_HEIGHT = 22;
    private final int PERIOD_INTERVAL = 300;

    private Timer timer;
    private boolean isFallingFinished = false;
    private boolean isPaused = false;
    private int numLinesRemoved = 0;
    private int curX = 0;
    private int curY = 0;
    private JLabel statusbar;
    private Shape curPiece;
    private Shape.Tetrominoe[] board;

    public Board(Tetris parent) {
        initBoard(parent);
    }

    private void initBoard(Tetris parent) {
        setFocusable(true);
        statusbar = parent.getStatusBar();
        addKeyListener(new TAdapter());
    }

    private int squareWidth() {
        return (int) getSize().getWidth() / BOARD_WIDTH;
    }

    private int squareHeight() {
        return (int) getSize().getHeight() / BOARD_HEIGHT;
    }

    private Shape.Tetrominoe shapeAt(int x, int y) {
        return board[(y * BOARD_WIDTH) + x];
    }

    void start() {
        playMusic("C:\\Users\\Dell\\Downloads\\Tetriss.wav");
        curPiece = new Shape();
        board = new Shape.Tetrominoe[BOARD_WIDTH * BOARD_HEIGHT];
        clearBoard();
        newPiece();
        timer = new Timer(PERIOD_INTERVAL, new GameCycle());
        timer.start();
    }

    private void sekilyenile() {
        newPiece();
    }

    void restart() {
        timer.stop();
        isPaused = false;
        numLinesRemoved = 0;
        curX = 0;
        curY = 0;
        curPiece = new Shape();
        clearBoard();
        newPiece();
        isFallingFinished = false;
        statusbar.setText("0");
        timer.start();
        requestFocus();
    }

    private void pause() {
        isPaused = !isPaused;
        if (isPaused) {
            statusbar.setText("paused");
        } else {
            statusbar.setText(String.valueOf(numLinesRemoved));
        }
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        doDrawing(g);
    }

    private void doDrawing(Graphics g) {
        setBackground(Color.white);
        var size = getSize();
        int boardTop = (int) size.getHeight() - BOARD_HEIGHT * squareHeight();

        for (int i = 0; i < BOARD_HEIGHT; i++) {
            for (int j = 0; j < BOARD_WIDTH; j++) {
                Shape.Tetrominoe shape = shapeAt(j, BOARD_HEIGHT - i - 1);
                if (shape != Shape.Tetrominoe.NoShape) {
                    drawSquare(g, j * squareWidth(), boardTop + i * squareHeight(), shape);
                }
            }
        }

        if (curPiece.getShape() != Shape.Tetrominoe.NoShape) {
            for (int i = 0; i < 4; i++) {
                int x = curX + curPiece.x(i);
                int y = curY - curPiece.y(i);
                drawSquare(g, x * squareWidth(), boardTop + (BOARD_HEIGHT - y - 1) * squareHeight(), curPiece.getShape());
            }
        }
    }

    private void dropDown() {
        int newY = curY;
        while (newY > 0) {
            if (!tryMove(curPiece, curX, newY - 1)) {
                break;
            }
            newY--;
        }
        pieceDropped();
    }

    private void oneLineDown() {
        if (!tryMove(curPiece, curX, curY - 1)) {
            pieceDropped();
        }
    }

    private void clearBoard() {
        for (int i = 0; i < BOARD_HEIGHT * BOARD_WIDTH; i++) {
            board[i] = Shape.Tetrominoe.NoShape;
        }
    }

    private void pieceDropped() {
        for (int i = 0; i < 4; i++) {
            int x = curX + curPiece.x(i);
            int y = curY - curPiece.y(i);
            board[(y * BOARD_WIDTH) + x] = curPiece.getShape();
        }
        removeFullLines();
        if (!isFallingFinished) {
            newPiece();
        }
    }

    private void newPiece() {
        curPiece.setRandomShape();
        curX = BOARD_WIDTH / 2 + 1;
        curY = BOARD_HEIGHT - 1 + curPiece.minY();
        if (!tryMove(curPiece, curX, curY)) {
            curPiece.setShape(Shape.Tetrominoe.NoShape);
            timer.stop();
            var msg = String.format("Game over. Score: %d", numLinesRemoved);
            statusbar.setText(msg);
        }
    }

    private boolean tryMove(Shape newPiece, int newX, int newY) {
        for (int i = 0; i < 4; i++) {
            int x = newX + newPiece.x(i);
            int y = newY - newPiece.y(i);
            if (x < 0 || x >= BOARD_WIDTH || y < 0 || y >= BOARD_HEIGHT) {
                return false;
            }
            if (shapeAt(x, y) != Shape.Tetrominoe.NoShape) {
                return false;
            }
        }
        curPiece = newPiece;
        curX = newX;
        curY = newY;
        repaint();
        return true;
    }

    public static void playMusic(String filePath) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(filePath).getAbsoluteFile());
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeFullLines() {
        int numFullLines = 0;
        for (int i = BOARD_HEIGHT - 1; i >= 0; i--) {
            boolean lineIsFull = true;
            for (int j = 0; j < BOARD_WIDTH; j++) {
                if (shapeAt(j, i) == Shape.Tetrominoe.NoShape) {
                    lineIsFull = false;
                    break;
                }
            }
            if (lineIsFull) {
                numFullLines++;
                animateLineRemoval(i);
                for (int k = i; k < BOARD_HEIGHT - 1; k++) {
                    for (int j = 0; j < BOARD_WIDTH; j++) {
                        board[(k * BOARD_WIDTH) + j] = shapeAt(j, k + 1);
                    }
                }
            }
        }
        if (numFullLines > 0) {
            numLinesRemoved += numFullLines;
            statusbar.setText(String.valueOf(numLinesRemoved));
            isFallingFinished = true;
            curPiece.setShape(Shape.Tetrominoe.NoShape);
        }
    }

    private void animateLineRemoval(int line) {
        Timer animationTimer = new Timer(50, new ActionListener() {
            int step = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (step < 5) {
                    for (int j = 0; j < BOARD_WIDTH; j++) {
                        board[(line * BOARD_WIDTH) + j] = (step % 2 == 0) ? Shape.Tetrominoe.NoShape : Shape.Tetrominoe.ZShape;
                    }
                    repaint();
                    step++;
                } else {
                    ((Timer) e.getSource()).stop();
                }
            }
        });
        animationTimer.start();
    }

    private void drawSquare(Graphics g, int x, int y, Shape.Tetrominoe shape) {
        Color colors[] = {new Color(0, 0, 0), new Color(204, 102, 102), new Color(102, 204, 102), new Color(102, 102, 204),
                new Color(204, 204, 102), new Color(204, 102, 204), new Color(102, 204, 204), new Color(218, 170, 0)};
        var color = colors[shape.ordinal()];
        g.setColor(color);
        g.fillRect(x + 1, y + 1, squareWidth() - 2, squareHeight() - 2);
        g.setColor(color.brighter());
        g.drawLine(x, y + squareHeight() - 1, x, y);
        g.drawLine(x, y, x + squareWidth() - 1, y);
        g.setColor(color.darker());
        g.drawLine(x + 1, y + squareHeight() - 1, x + squareWidth() - 1, y + squareHeight() - 1);
        g.drawLine(x + squareWidth() - 1, y + squareHeight() - 1, x + squareWidth() - 1, y + 1);
    }

    private class GameCycle implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            doGameCycle();
        }
    }

    private void doGameCycle() {
        update();
        repaint();
    }

    private void update() {
        if (isPaused) {
            return;
        }
        if (isFallingFinished) {
            isFallingFinished = false;
            newPiece();
        } else {
            oneLineDown();
        }
    }

    class TAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (curPiece.getShape() == Shape.Tetrominoe.NoShape) {
                return;
            }
            int keycode = e.getKeyCode();
            switch (keycode) {
                case KeyEvent.VK_R -> restart();
                case KeyEvent.VK_S -> sekilyenile();
                case KeyEvent.VK_P -> pause();
                case KeyEvent.VK_LEFT -> tryMove(curPiece, curX - 1, curY);
                case KeyEvent.VK_RIGHT -> tryMove(curPiece, curX + 1, curY);
                case KeyEvent.VK_DOWN -> tryMove(curPiece.rotateRight(), curX, curY);
                case KeyEvent.VK_UP -> tryMove(curPiece.rotateLeft(), curX, curY);
                case KeyEvent.VK_SPACE -> dropDown();
                case KeyEvent.VK_D -> oneLineDown();
            }
        }
    }
}