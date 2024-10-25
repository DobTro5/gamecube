package org.example;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class DiceGame extends JFrame {
    private static final int WINNING_SCORE = 1000; // Очки для победы
    private static final int OPENING_SCORE = 75;    // Минимальная сумма очков для открытия
    private static final int DICE_COUNT = 5;         // Количество кубиков
    private static final int ANIMATION_FRAMES = 30;  // Количество кадров анимации (3 секунды)

    private static class Player {
        String name;
        int totalScore;      // Итоговый счет
        int currentScore;    // Очки за текущий ход
        boolean isGameOpened; // Флаг, открыта ли игра игроком

        Player(String name) {
            this.name = name;
            this.totalScore = 0;
            this.currentScore = 0;
            this.isGameOpened = false;
        }
    }

    private ArrayList<Player> players;              // Список игроков
    private int currentPlayerIndex;                 // Индекс текущего игрока
    private JLabel infoLabel;                        // Метка для отображения информации о текущем игроке
    private JButton rollButton;                      // Кнопка для броска кубиков
    private JPanel dicePanel;                        // Панель для отображения кубиков
    private Image[] diceImages = new Image[6];     // Массив изображений кубиков
    private int[] currentDice;                      // Текущий набор кубиков
    private Timer timer;                             // Таймер для анимации
    private int animationFrame;                      // Для отслеживания кадров анимации

    public DiceGame() {
        loadResources(); // Загрузка изображений кубиков
        setTitle("Dice Game");
        setSize(600, 300); // Увеличил ширину окна для большей площади
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new FlowLayout());

        // Запрашиваем количество игроков
        int numPlayers = getNumberOfPlayers();
        players = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            String name = JOptionPane.showInputDialog("Введите имя игрока " + (i + 1) + ":");
            players.add(new Player(name));
        }

        infoLabel = new JLabel();
        rollButton = new JButton("Бросить кубики");
        dicePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.clearRect(0, 0, getWidth(), getHeight()); // Очистка фона перед отрисовкой
                if (currentDice != null) {
                    for (int i = 0; i < currentDice.length; i++) {
                        // Рисуем кубики с размером (100x100)
                        if (diceImages[currentDice[i]] != null) {
                            g.drawImage(diceImages[currentDice[i]], i * 110 + 40, 50, 100, 100, null); // Изменены размеры
                        } else {
                            g.drawString("Изображение не найдено", 50, 50);
                        }
                    }
                } else {
                    g.drawString("Кубики не загружены", 50, 50);
                }
            }
        };
        dicePanel.setPreferredSize(new Dimension(500, 150)); // Увеличил высоту панели для большего пространства

        add(infoLabel);
        add(rollButton);
        add(dicePanel);

        rollButton.addActionListener(e -> startRollAnimation());

        currentPlayerIndex = 0;
        updateInfo();
    }

    private int getNumberOfPlayers() {
        while (true) {
            String input = JOptionPane.showInputDialog("Введите количество игроков (от 2 до 8):");
            try {
                int numPlayers = Integer.parseInt(input);
                if (numPlayers >= 2 && numPlayers <= 8) {
                    return numPlayers;
                } else {
                    JOptionPane.showMessageDialog(this, "Количество игроков должно быть от 2 до 8!");
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Пожалуйста, введите корректное число.");
            }
        }
    }

    private void loadResources() {
        for (int i = 0; i < 6; i++) {
            String path = "dice" + (i + 1) + ".png"; // Путь к изображениям кубиков
            diceImages[i] = loadImage(path);
        }
    }

    private Image loadImage(String imagePath) {
        URL resourceUrl = getClass().getResource("/" + imagePath);
        if (resourceUrl != null) {
            return new ImageIcon(resourceUrl).getImage();
        } else {
            System.err.println("Изображение не найдено: " + imagePath);
            return null;
        }
    }

    private void startRollAnimation() {
        animationFrame = 0;
        currentDice = new int[DICE_COUNT]; // Сбрасываем текущие кубики
        timer = new Timer(100, e -> {
            Random rand = new Random();
            for (int i = 0; i < currentDice.length; i++) {
                currentDice[i] = rand.nextInt(6); // Случайные значения для анимации
            }
            dicePanel.repaint(); // Перерисовываем панель

            animationFrame++;
            if (animationFrame >= ANIMATION_FRAMES) {
                timer.stop(); // Остановить анимацию
                currentDice = rollDice(); // Бросок кубиков
                dicePanel.repaint(); // Перерисовываем с настоящими значениями
                analyzeRoll(); // Анализируем бросок
            }
        });
        timer.start(); // Запускаем анимацию
    }

    private void updateInfo() {
        Player currentPlayer = players.get(currentPlayerIndex);
        infoLabel.setText("Ход: " + currentPlayer.name + " (Очки: " + currentPlayer.totalScore + ")");
    }

    private void analyzeRoll() {
        Player currentPlayer = players.get(currentPlayerIndex);
        int score = calculateScore(currentDice);

        if (score > 0) {
            currentPlayer.currentScore += score;

            // Проверка на открытие игры
            if (!currentPlayer.isGameOpened && currentPlayer.currentScore >= OPENING_SCORE) {
                currentPlayer.isGameOpened = true;
                JOptionPane.showMessageDialog(this, currentPlayer.name + " открыл игру!");
            }

            updateInfo();

            // Предложить решить, бросать ли еще раз
            int decision = JOptionPane.showConfirmDialog(this,
                    currentPlayer.name + ", у вас " + currentPlayer.currentScore + " очков. Бросить еще раз?",
                    "Продолжение хода", JOptionPane.YES_NO_OPTION);

            if (decision == JOptionPane.YES_OPTION) {
                // Игрок продолжает бросать
                startRollAnimation();
            } else {
                // Игрок завершает ход
                currentPlayer.totalScore += currentPlayer.currentScore;
                checkWinCondition(currentPlayer);
                currentPlayer.currentScore = 0; // Сбрасываем текущие очки
                currentPlayerIndex = (currentPlayerIndex + 1) % players.size(); // Переход к следующему игроку
                updateInfo();
            }
        } else {
            // Нулевая комбинация, сбросить текущие очки
            JOptionPane.showMessageDialog(this, "Нулевая комбинация! Ваши очки сгорают.");
            currentPlayer.currentScore = 0; // Сбрасываем текущие очки
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size(); // Переход к следующему игроку
            updateInfo();
        }
    }

    private void checkWinCondition(Player currentPlayer) {
        if (currentPlayer.totalScore >= WINNING_SCORE) {
            JOptionPane.showMessageDialog(this, currentPlayer.name + " выиграл с " + currentPlayer.totalScore + " очками!");
            System.exit(0); // Завершение игры
        }
    }

    private int[] rollDice() {
        Random rand = new Random();
        int[] dice = new int[DICE_COUNT];
        for (int i = 0; i < DICE_COUNT; i++) {
            dice[i] = rand.nextInt(6); // Индексы изображений кубиков
        }
        return dice;
    }

    private int calculateScore(int[] dice) {
        HashMap<Integer, Integer> countMap = new HashMap<>();
        for (int die : dice) {
            countMap.put(die, countMap.getOrDefault(die, 0) + 1);
        }

        int score = 0;
        score += countMap.getOrDefault(1, 0) * 10; // Один 1
        score += countMap.getOrDefault(5, 0) * 5;  // Один 5

        // Учет комбинаций по 3 и более
        for (int i = 1; i <= 6; i++) {
            int count = countMap.getOrDefault(i, 0);
            if (count >= 3) {
                score += (i * 10) * (count / 3); // Комбинации по 3 и более
            }
        }

        return score;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DiceGame app = new DiceGame();
            app.setVisible(true);
        });
    }
}
