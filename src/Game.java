import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Polished PaddleBall game.
 * Controls:
 *  - Up / Down arrows to move paddle
 *  - P to pause / resume
 *  - R to reset during gameplay
 *  - Enter to restart after Game Over
 */
public class Game extends JPanel implements Runnable, KeyListener {
    private final int WIDTH = 800;
    private final int HEIGHT = 600;

    private final int PADDLE_X = 40;
    private int paddleY = 250;
    private final int PADDLE_W = 12;
    private final int PADDLE_H = 100;
    private final int PADDLE_SPEED = 6;

    private final int BALL_SIZE = 18;
    private double ballX, ballY;
    private double ballDX = -4.0, ballDY = 3.0;

    private boolean up, down;
    private int score = 0;
    private int highScore = 0;
    private int lives = 3;

    private volatile boolean paused = false;
    private volatile boolean gameOver = false;

    private Thread gameThread;

    public Game() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        initGame();
    }

    private void initGame() {
        resetBall();
        score = 0;
        lives = 3;
        paused = false;
        gameOver = false;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        requestFocusInWindow();
        start();
    }

    private void start() {
        if (gameThread == null) {
            gameThread = new Thread(this, "Game-Thread");
            gameThread.setDaemon(true);
            gameThread.start();
        }
    }

    @Override
    public void run() {
        final int targetFps = 60;
        final long targetDelay = 1000 / targetFps;
        while (true) {
            long t0 = System.currentTimeMillis();
            if (!paused && !gameOver) {
                updateState();
            }
            repaint();
            long took = System.currentTimeMillis() - t0;
            long sleep = Math.max(2, targetDelay - took);
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void updateState() {
        if (up) {
            paddleY -= PADDLE_SPEED;
        }
        if (down) {
            paddleY += PADDLE_SPEED;
        }
        paddleY = Math.max(0, Math.min(HEIGHT - PADDLE_H, paddleY));

        ballX += ballDX;
        ballY += ballDY;

        if (ballY <= 0) {
            ballY = 0;
            ballDY = -ballDY;
        } else if (ballY + BALL_SIZE >= HEIGHT) {
            ballY = HEIGHT - BALL_SIZE;
            ballDY = -ballDY;
        }

        if (ballX + BALL_SIZE >= WIDTH) {
            ballX = WIDTH - BALL_SIZE;
            ballDX = -Math.abs(ballDX);
            score++;
            if (score > highScore) {
                highScore = score;
            }
            ballDX *= 1.02;
            ballDY *= 1.02;
        }

        if (ballDX < 0 && ballX <= PADDLE_X + PADDLE_W && ballX + BALL_SIZE >= PADDLE_X) {
            if (ballY + BALL_SIZE >= paddleY && ballY <= paddleY + PADDLE_H) {
                ballX = PADDLE_X + PADDLE_W;

                double relativeIntersectY = (paddleY + PADDLE_H / 2.0) - (ballY + BALL_SIZE / 2.0);
                double normalizedRelativeIntersectionY = relativeIntersectY / (PADDLE_H / 2.0);
                double maxBounceAngle = Math.toRadians(75);
                double bounceAngle = normalizedRelativeIntersectionY * maxBounceAngle;

                double speed = Math.hypot(ballDX, ballDY);
                speed = Math.min(12.0, speed * 1.05);

                ballDX = speed * Math.cos(bounceAngle);
                ballDY = -speed * Math.sin(bounceAngle);

                score++;
                if (score > highScore) {
                    highScore = score;
                }
            }
        }

        if (ballX < 0) {
            lives--;
            if (lives <= 0) {
                gameOver = true;
                paused = false;
            } else {
                resetBall();
                paused = true;
                new Thread(() -> {
                    try {
                        Thread.sleep(800);
                    } catch (InterruptedException ignored) {
                    }
                    paused = false;
                }, "Unpause-Timer").start();
            }
        }
    }

    private void resetBall() {
        ballX = WIDTH / 2.0 - BALL_SIZE / 2.0;
        ballY = HEIGHT / 2.0 - BALL_SIZE / 2.0;
        double angle = Math.toRadians((Math.random() * 120) - 60);
        double speed = 5.0 + Math.random() * 2.0;
        ballDX = -Math.abs(speed * Math.cos(angle));
        ballDY = speed * Math.sin(angle);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Color.WHITE);
        g2.fillRect(PADDLE_X, paddleY, PADDLE_W, PADDLE_H);
        g2.fillOval((int) Math.round(ballX), (int) Math.round(ballY), BALL_SIZE, BALL_SIZE);

        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.drawString("Score: " + score, WIDTH - 150, 28);
        g2.drawString("High: " + highScore, WIDTH - 150, 52);
        g2.drawString("Lives: " + lives, 10, 28);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2.drawString("Up/Down: Move  P: Pause  R: Reset  Enter: Restart", 10, HEIGHT - 10);

        if (paused && !gameOver) {
            drawCenteredString(g2, "PAUSED", new Rectangle(0, 0, WIDTH, HEIGHT), new Font("SansSerif", Font.BOLD, 48));
        }

        if (gameOver) {
            drawCenteredString(g2, "GAME OVER", new Rectangle(0, 0, WIDTH, HEIGHT), new Font("SansSerif", Font.BOLD, 48));
            drawCenteredString(g2, "Press Enter to restart", new Rectangle(0, 60, WIDTH, HEIGHT), new Font("SansSerif", Font.PLAIN, 20));
        }

        g2.dispose();
    }

    private void drawCenteredString(Graphics2D g, String text, Rectangle rect, Font font) {
        FontMetrics metrics = g.getFontMetrics(font);
        int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
        int y = rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent();
        g.setFont(font);
        g.setColor(Color.WHITE);
        g.drawString(text, x, y);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            up = true;
        }
        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            down = true;
        }
        if (e.getKeyCode() == KeyEvent.VK_P) {
            paused = !paused;
        }
        if (e.getKeyCode() == KeyEvent.VK_R) {
            initGame();
            resetBall();
        }
        if (e.getKeyCode() == KeyEvent.VK_ENTER && gameOver) {
            initGame();
            paused = false;
            gameOver = false;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            up = false;
        }
        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            down = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("PaddleBall - Polished");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            Game panel = new Game();
            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
