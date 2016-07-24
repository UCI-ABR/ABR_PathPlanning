package abr.teleop;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;


/**
 * Created by tiffany on 7/10/16.
 */
public class PixelGridView extends View {
    private int numColumns, numRows;
    private int cellWidth, cellHeight;
    private Paint paint = new Paint();
    private boolean[][] cellChecked;
    private int[][] gridColors = new int[0][0];

    public PixelGridView(Context context) {
        this(context, null);
    }

    public PixelGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    public int getNumColumns() {
        return numColumns;
    }

    public void setNumColumns(int numColumns) {
        this.numColumns = numColumns;
        calculateDimensions();
    }

    public int getNumRows() {
        return numRows;
    }

    public void setNumRows(int numRows) {
        this.numRows = numRows;
        calculateDimensions();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculateDimensions();
    }

    private void calculateDimensions() {
        if (numColumns < 1 || numRows < 1) {
            return;
        }

        cellWidth = getWidth() / numColumns;
        cellHeight = getHeight() / numRows;

        cellChecked = new boolean[numColumns][numRows];

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);

        if (numColumns == 0 || numRows == 0) {
            return;
        }

        int width = getWidth();
        int height = getHeight();

        for (int i = 0; i < numColumns; i++) {
            for (int j = 0; j < numRows; j++) {
                float[] col = {(float)gridColors[j][i],(float)1,(float)1};
                paint.setColor(Color.HSVToColor(col));
                canvas.drawRect(i * cellWidth, j * cellHeight,
                        (i + 1) * cellWidth, (j + 1) * cellHeight,
                        paint);
            }
        }

        for (int i = 1; i < numColumns; i++) {
            canvas.drawLine(i * cellWidth, 0, i * cellWidth, height, paint);
        }

        for (int i = 1; i < numRows; i++) {
            canvas.drawLine(0, i * cellHeight, width, i * cellHeight, paint);
        }
    }

    public void setGridColors(int[][] cost, int route[][], int currR, int currC){
        numRows = cost.length;
        numColumns = cost[0].length;
        gridColors = new int[cost.length][cost[0].length];
        int maxCost = Integer.MIN_VALUE;
        int minCost = Integer.MAX_VALUE;
        for(int i = 0; i < cost.length; i++){
            for(int j = 0; j < cost[0].length; j++){
                if(cost[i][j] > maxCost)
                    maxCost = cost[i][j];
                if(cost[i][j] < minCost)
                    minCost = cost[i][j];
            }
        }
        for(int i = 0; i < cost.length; i++){
            for(int j = 0; j < cost[0].length; j++){
                int hue = (int)(((float)(cost[i][j] - minCost))/(maxCost-minCost)*180)+180;
                gridColors[i][j] = hue;
            }
        }
        for(int i = 0; i < route.length; i++){
            gridColors[route[i][0]][route[i][1]] = 60;//yellow
        }
        gridColors[currR][currC] = 120;//green
        invalidate();
    }

    public void setMapColors(double[][] map, int route[][], int currR, int currC){
        numRows = map.length;
        numColumns = map[0].length;
        gridColors = new int[numRows][numColumns];
        double maxCost = Double.MIN_VALUE;
        double minCost = Double.MAX_VALUE;
        for(int i = 0; i < numRows; i++){
            for(int j = 0; j < numColumns; j++){
                if(map[i][j] > maxCost)
                    maxCost = map[i][j];
                if(map[i][j] < minCost)
                    minCost = map[i][j];
            }
        }
        for(int i = 0; i < numRows; i++){
            for(int j = 0; j < numColumns; j++){
                int hue = (int)((map[i][j] - minCost)/(maxCost-minCost)*180.0) + 180;
                gridColors[i][j] = hue;
            }
        }
        for(int i = 0; i < route.length; i++){
            gridColors[route[i][0]][route[i][1]] = 60;//yellow
        }
        gridColors[currR][currC] = 120;//green
        invalidate();
    }
}
