package net.tjeerd;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class RayCaster {
    private static final int GRID_COLUMNS= 10;
    private static final int GRID_ROWS = 8;
    private static final int MAP_WALL_DEFAULT = 1;
    private static final int MAP_WALL_TWO  = 2;
    private static final int MAP_WALL_THREE  = 3;
    private static final int MAP_WALL_FOUR  = 4;
    private static final int MAP_WALL_FIVE  = 5;
    private static final int MAP_WALL_SIX = 6;
    private static final int MAP_WALL_SEVEN  = 7;

    private Point horizontalGridPoint;
    private Point verticalGridPoint;
    private HorizontalFacingDirection horizontalFacingDirection;
    private VerticalFacingDirection verticalFacingDirection;
    private Statistics statistics;

    enum HorizontalFacingDirection {
        UP(-64),
        DOWN(64);

        private int value;

        HorizontalFacingDirection(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    enum VerticalFacingDirection {
        LEFT(-64),
        RIGHT(64);

        private int value;

        VerticalFacingDirection(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private int[][] map = new int[][]{
            { 1, 1, 1, 2, 3, 4, 5, 1, 1, 1 },
            { 1, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 1, 1, 0, 0, 1, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 6, 7, 6, 7, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }
    };

    public RayCaster() {
        horizontalGridPoint = new Point();
        verticalGridPoint = new Point();

        horizontalFacingDirection = HorizontalFacingDirection.UP;
        verticalFacingDirection = VerticalFacingDirection.RIGHT;

        Draw2D draw2D = new Draw2D();
        draw2D.setVisible(true);
    }

    public static void main(String[] args) {
        RayCaster rayCaster = new RayCaster();
    }

    public class Draw2D extends JFrame {
        public Draw2D() {
            statistics = new Statistics();
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setLayout(new BorderLayout());
            add(new DrawPane(), BorderLayout.CENTER);
            pack();
        }
    }

    class DrawPane extends JComponent implements KeyListener {
        private static final int CELL_SIZE = 64;
        private int playerX = 346; //GRID_ROWS * CELL_SIZE  / 2;
        private int playerY = 234; //GRID_COLUMNS * CELL_SIZE  / 2;
        private int playerAngle;

        public DrawPane() {
            setPreferredSize(new Dimension(1280, 1024));
            addKeyListener(this);
            setFocusable(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            g2.setColor(Color.red);
            g2.translate(100, 100);

            drawMap(g2);
            drawPlayer(g2);
            printStatistics(g2);
            drawPOV(60d,1, 320, g2);
        }

        private void printStatistics(Graphics2D g2) {
            g2.drawString(String.format("Player grid x=%d, y=%d", (playerX/CELL_SIZE ), (playerY/CELL_SIZE )), 800, 620);
            g2.drawString(String.format("Player x=%d, y=%d", playerX, playerY), 800, 640);
            g2.drawString(String.format("h.x=%d, h.y=%d", statistics.getHorizontalGridHitX(), statistics.getHorizontalGridHitY()), 800, 660);
            g2.drawString(String.format("v.x=%d, v.y=%d", statistics.getVerticalGridHitX(), statistics.getVerticalGridHitY()), 800, 680);
            g2.drawString(String.format("h.dist=%f, v.dist=%f", statistics.getHorizontalDistance(), statistics.getVerticalDistance()), 800, 700);
            g2.drawString(String.format("h.intersect.x=%d, h.intersect.y=%d", statistics.getFirstHorizontalIntersectionX(), statistics.getFirstHorizontalIntersectionY()), 800, 720);
            g2.drawString(String.format("v.intersect.x=%d, v.intersect.y=%d", statistics.getFirstVerticalIntersectionX(), statistics.getFirstVerticalIntersectionY()), 800, 740);
        }

        /**
         * Determine the horizontal or vertical grid point we are looking at from our player POV and return the grid x, y, coordinates.
         * The determination is done by first checking the horizontal wall/line which is hit and the vertical wall/line which is hit.
         * The distance is then compared and the closest wall grid point is returned.
         *
         * @param rayColumnStart starting degree for the POV
         * @param rayColumnStop  ending degree for the POV
         * @param g2              graphics component to draw on
         */
        private void drawPOV(double fovAngle, int rayColumnStart, int rayColumnStop, Graphics2D g2) {
            double radians;
            int nextY=0;
            boolean isWall;
            Point gridPointToDraw = new Point();
            int distance = 1;
            int distanceProjectionPlane = (int) ((rayColumnStop / 2) / Math.tan(fovAngle/2));

            for (int rayColumnIndex=rayColumnStart; rayColumnIndex<rayColumnStop; rayColumnIndex++) {
                int angle = (int) (fovAngle + (rayColumnIndex * (fovAngle/rayColumnStop)));

                radians = Math.toRadians(angle);

                if (angle > 0 && angle < 180) {
                    horizontalFacingDirection = HorizontalFacingDirection.UP;
                    nextY = -CELL_SIZE;

                } else if (angle > 180 && angle < 360) {
                    horizontalFacingDirection = HorizontalFacingDirection.DOWN;
                    nextY = CELL_SIZE;
                }

                if (angle > 90 && angle < 270) {
                    verticalFacingDirection = VerticalFacingDirection.LEFT;
                } else {
                    verticalFacingDirection = VerticalFacingDirection.RIGHT;
                }

                // Horizontal wall searching
                // Determine horizontal Y
                int firstHorizontalIntersectionY = (playerY / CELL_SIZE) * (CELL_SIZE) + (horizontalFacingDirection == HorizontalFacingDirection.UP ? -1 : CELL_SIZE);
                horizontalGridPoint.y = firstHorizontalIntersectionY / CELL_SIZE;

                // Determine horizontal X
                int firstHorizontalIntersectionX = (int) (playerX + (playerY - firstHorizontalIntersectionY) / Math.tan(radians));
                horizontalGridPoint.x = firstHorizontalIntersectionX / CELL_SIZE;

                int nextX = (int) (CELL_SIZE / Math.tan(radians));

                while (horizontalGridPoint.y >= 0 && horizontalGridPoint.y < GRID_COLUMNS && map[horizontalGridPoint.y][horizontalGridPoint.x] == 0) {
                    firstHorizontalIntersectionX += nextX;
                    firstHorizontalIntersectionY += nextY;

                    horizontalGridPoint.x = firstHorizontalIntersectionX / CELL_SIZE;
                    horizontalGridPoint.y = firstHorizontalIntersectionY / CELL_SIZE;
                }

                // Vertical wall searching
                // Determine vertical X (goed)
                int firstVerticalIntersectionX = (playerX / CELL_SIZE) * (CELL_SIZE) + verticalFacingDirection.getValue();
                verticalGridPoint.x = firstVerticalIntersectionX / CELL_SIZE;

                // Determine vertical Y (niet goed?)
                int firstVerticalIntersectionY = (int) (playerY + (playerX - firstVerticalIntersectionX) * Math.tan(radians));
                verticalGridPoint.y = firstVerticalIntersectionY / CELL_SIZE;

                nextX = verticalFacingDirection == VerticalFacingDirection.LEFT ? -CELL_SIZE : CELL_SIZE;
                nextY = -(int) (CELL_SIZE * Math.tan(radians));

                while (verticalGridPoint.y >= 0 && verticalGridPoint.y < GRID_ROWS && map[verticalGridPoint.y][verticalGridPoint.x] == 0) {
                    firstVerticalIntersectionX += nextX;
                    firstVerticalIntersectionY += nextY;

                    verticalGridPoint.x = firstVerticalIntersectionX / CELL_SIZE;
                    verticalGridPoint.y = firstVerticalIntersectionY / CELL_SIZE;
                }

                // determine the closest point relative to the player position
                // TODO: discriminate between hit or not here, now it just checks the closest and returns it
                float horizontalDistance = (float) Math.abs((playerX - firstHorizontalIntersectionX) / Math.cos(radians));
                float verticalDistance = (float) Math.abs((playerX - firstVerticalIntersectionX) / Math.cos(radians));

                statistics.setHorizontalGridHitX(horizontalGridPoint.x);
                statistics.setHorizontalGridHitY(horizontalGridPoint.y);
                statistics.setVerticalGridHitX(verticalGridPoint.x);
                statistics.setVerticalGridHitY(verticalGridPoint.y);
                statistics.setHorizontalDistance(horizontalDistance);
                statistics.setVerticalDistance(verticalDistance);

                statistics.setFirstVerticalIntersectionX(firstVerticalIntersectionX);
                statistics.setFirstVerticalIntersectionY(firstVerticalIntersectionY);

                statistics.setFirstHorizontalIntersectionX(firstHorizontalIntersectionX);
                statistics.setFirstHorizontalIntersectionY(firstHorizontalIntersectionY);


                if (horizontalDistance < verticalDistance) {
                    distance = (int) horizontalDistance;
                    gridPointToDraw = horizontalGridPoint;
                } else if (verticalDistance < horizontalDistance) {
                    distance = (int) verticalDistance;
                    gridPointToDraw = verticalGridPoint;
                } else if (horizontalDistance == verticalDistance) {
                    // because both are the same, doesn't matter which one to return
                    distance = (int) horizontalDistance;
                    gridPointToDraw = horizontalGridPoint;
                }


                if (gridPointToDraw.x >= 0 && gridPointToDraw.y < GRID_COLUMNS && gridPointToDraw.y >= 0 && gridPointToDraw.y < GRID_ROWS) {
                    isWall = map[gridPointToDraw.y][gridPointToDraw.x] > 0;

                    g2.setColor(getWallColor(map[gridPointToDraw.y][gridPointToDraw.x]));

                    System.out.println("column = " + rayColumnIndex + "/walltype=" +map[gridPointToDraw.y][gridPointToDraw.x] + "/angle=" + angle + "/grid.x=" + gridPointToDraw.x + "/grid.y="+gridPointToDraw.y);

                    if (isWall && distance > 0) {

                        int wallHeight = (CELL_SIZE / distance * distanceProjectionPlane);
                        int center = 200;
                        int topOfWall = center + wallHeight;
                        int bottomOfWall = center - wallHeight;

                        g2.drawLine(1100 - rayColumnIndex, 100, 1100-rayColumnIndex, 200);
                    }
                }

            }

        }

        private Color getWallColor(int wallType) {
            switch (wallType) {
                case MAP_WALL_DEFAULT : return Color.RED;
                case MAP_WALL_TWO : return Color.BLUE;
                case MAP_WALL_THREE : return Color.YELLOW;
                case MAP_WALL_FOUR : return Color.GREEN;
                case MAP_WALL_FIVE : return Color.CYAN;
                case MAP_WALL_SIX : return Color.MAGENTA;
                case MAP_WALL_SEVEN: return Color.ORANGE;

                default: return Color.DARK_GRAY;
            }
        }



        private void drawPlayer(Graphics2D g2) {
            final int BOUNDING_BOX_SIZE = 5;

            g2.setColor(Color.WHITE);
            g2.drawLine(playerX, playerY, playerX, playerY);
            g2.drawRect(playerX-BOUNDING_BOX_SIZE, playerY-BOUNDING_BOX_SIZE, BOUNDING_BOX_SIZE*2, BOUNDING_BOX_SIZE*2);

            // Draw angles camera
            int angle = 60;
            int angleRight = 120;

            int length = -250;

            int endX = (int) (playerX + Math.cos(Math.toRadians(angle)) * length);
            int endY = (int) (playerY + Math.sin(Math.toRadians(angle)) * length);

            int endX2 = (int) (playerX + Math.cos(Math.toRadians(angleRight)) * length);
            int endY2 = (int) (playerY + Math.sin(Math.toRadians(angleRight)) * length);

            g2.drawLine(playerX, playerY, endX, endY);
            g2.drawLine(playerX, playerY, endX2, endY2);
        }


        private void drawMap(Graphics2D graphics2D) {

            for (int x = 0; x<GRID_COLUMNS; x++) {
                for (int y = 0; y<GRID_ROWS; y++) {
                    // Draw the walls orange
                    graphics2D.setColor(getWallColor(map[y][x]));

                    // Draw the cell
                    graphics2D.fillRect(x*CELL_SIZE , y*CELL_SIZE , CELL_SIZE , CELL_SIZE  );

                    // Draw rectangle around the cell
                    graphics2D.setColor(Color.BLACK);
                    graphics2D.drawRect(x*CELL_SIZE , y*CELL_SIZE , CELL_SIZE , CELL_SIZE  );

                }
            }

        }

        @Override
        public void keyTyped(KeyEvent e) {

        }

        @Override
        public void keyPressed(KeyEvent e) {
            if ( e.getKeyCode() == KeyEvent.VK_LEFT) {
                if (playerX > 0) {
                    playerX--;
                }
            } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                if (playerX < GRID_COLUMNS*CELL_SIZE ) {
                    playerX++;
                }
            } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                if (playerY > 0) {
                    playerY--;
                }
            } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                if (playerY < GRID_ROWS*CELL_SIZE ) {
                    playerY++;
                }
            }

            repaint();

        }

        @Override
        public void keyReleased(KeyEvent e) {

        }
    }

    private class Statistics {
        private float horizontalDistance;
        private float verticalDistance;
        private int horizontalGridHitX;
        private int horizontalGridHitY;
        private int verticalGridHitX;
        private int verticalGridHitY;
        private int firstHorizontalIntersectionX;
        private int firstHorizontalIntersectionY;
        private int firstVerticalIntesectionX;
        private int firstVerticalIntersectionY;

        public int getFirstHorizontalIntersectionX() {
            return firstHorizontalIntersectionX;
        }

        public void setFirstHorizontalIntersectionX(int firstHorizontalIntersectionX) {
            this.firstHorizontalIntersectionX = firstHorizontalIntersectionX;
        }

        public int getFirstHorizontalIntersectionY() {
            return firstHorizontalIntersectionY;
        }

        public void setFirstHorizontalIntersectionY(int firstHorizontalIntersectionY) {
            this.firstHorizontalIntersectionY = firstHorizontalIntersectionY;
        }

        public int getFirstVerticalIntersectionX() {
            return firstVerticalIntesectionX;
        }

        public void setFirstVerticalIntersectionX(int firstVerticalIntesectionX) {
            this.firstVerticalIntesectionX = firstVerticalIntesectionX;
        }

        public int getFirstVerticalIntersectionY() {
            return firstVerticalIntersectionY;
        }

        public void setFirstVerticalIntersectionY(int firstVerticalIntersectionY) {
            this.firstVerticalIntersectionY = firstVerticalIntersectionY;
        }

        public float getHorizontalDistance() {
            return horizontalDistance;
        }

        public void setHorizontalDistance(float horizontalDistance) {
            this.horizontalDistance = horizontalDistance;
        }

        public float getVerticalDistance() {
            return verticalDistance;
        }

        public void setVerticalDistance(float verticalDistance) {
            this.verticalDistance = verticalDistance;
        }

        public int getHorizontalGridHitX() {
            return horizontalGridHitX;
        }

        public void setHorizontalGridHitX(int horizontalGridHitX) {
            this.horizontalGridHitX = horizontalGridHitX;
        }

        public int getHorizontalGridHitY() {
            return horizontalGridHitY;
        }

        public void setHorizontalGridHitY(int horizontalGridHitY) {
            this.horizontalGridHitY = horizontalGridHitY;
        }

        public int getVerticalGridHitX() {
            return verticalGridHitX;
        }

        public void setVerticalGridHitX(int verticalGridHitX) {
            this.verticalGridHitX = verticalGridHitX;
        }

        public int getVerticalGridHitY() {
            return verticalGridHitY;
        }

        public void setVerticalGridHitY(int verticalGridHitY) {
            this.verticalGridHitY = verticalGridHitY;
        }
    }
}
