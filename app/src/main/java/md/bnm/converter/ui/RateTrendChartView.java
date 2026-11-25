package md.bnm.converter.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import md.bnm.converter.model.HistoricalRatePoint;
import md.bnm.converter.model.PeriodStats;
import md.bnm.converter.model.TrendDirection;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Custom Android Canvas View that renders time-series exchange rate charts
 * for both the Weekly (7-day) and Monthly (30-day) comparison screens.
 */
public class RateTrendChartView extends View {
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint avgLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private List<HistoricalRatePoint> points = Collections.emptyList();
    private BigDecimal minRate = BigDecimal.ZERO;
    private BigDecimal maxRate = BigDecimal.ONE;
    private BigDecimal avgRate = BigDecimal.ZERO;
    private TrendDirection trendDirection = TrendDirection.FLAT;
    private String pairLabel = "EUR / MDL";
    private int selectedIndex = -1;

    public RateTrendChartView(Context context) {
        super(context);
        init();
    }

    public RateTrendChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        gridPaint.setColor(Color.parseColor("#E2E8F0"));
        gridPaint.setStrokeWidth(dp(1f));
        gridPaint.setStyle(Paint.Style.STROKE);

        axisTextPaint.setColor(Color.parseColor("#64748B"));
        axisTextPaint.setTextSize(sp(11f));

        linePaint.setColor(Color.parseColor("#2563EB"));
        linePaint.setStrokeWidth(dp(2.8f));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        fillPaint.setStyle(Paint.Style.FILL);

        avgLinePaint.setColor(Color.parseColor("#94A3B8"));
        avgLinePaint.setStrokeWidth(dp(1.2f));
        avgLinePaint.setStyle(Paint.Style.STROKE);
        avgLinePaint.setPathEffect(new DashPathEffect(new float[]{dp(5f), dp(4f)}, 0f));

        dotPaint.setStyle(Paint.Style.FILL);

        dotStrokePaint.setColor(Color.WHITE);
        dotStrokePaint.setStrokeWidth(dp(2f));
        dotStrokePaint.setStyle(Paint.Style.STROKE);

        tooltipBgPaint.setColor(Color.parseColor("#0F172A"));
        tooltipBgPaint.setStyle(Paint.Style.FILL);

        tooltipTextPaint.setColor(Color.WHITE);
        tooltipTextPaint.setTextSize(sp(11.5f));
        tooltipTextPaint.setFakeBoldText(true);
    }

    public void setStats(PeriodStats stats) {
        if (stats == null || stats.getPoints().isEmpty()) {
            this.points = Collections.emptyList();
            this.selectedIndex = -1;
            invalidate();
            return;
        }
        this.points = stats.getPoints();
        this.minRate = stats.getMinRate();
        this.maxRate = stats.getMaxRate();
        this.avgRate = stats.getAverageRate();
        this.trendDirection = stats.getTrendDirection();
        this.pairLabel = stats.getBaseCode() + " / " + stats.getQuoteCode();
        this.selectedIndex = this.points.size() - 1;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        if (points == null || points.isEmpty()) {
            String emptyMsg = "Нет исторических данных для отображения графика";
            float textWidth = axisTextPaint.measureText(emptyMsg);
            canvas.drawText(emptyMsg, (width - textWidth) / 2f, height / 2f, axisTextPaint);
            return;
        }

        float leftPad = dp(64f);
        float rightPad = dp(18f);
        float topPad = dp(34f);
        float bottomPad = dp(32f);

        float chartWidth = Math.max(1f, width - leftPad - rightPad);
        float chartHeight = Math.max(1f, height - topPad - bottomPad);

        double minVal = minRate.doubleValue();
        double maxVal = maxRate.doubleValue();
        double range = maxVal - minVal;
        if (range < 0.0001) {
            range = Math.max(0.01, minVal * 0.01);
            minVal -= range / 2.0;
            maxVal += range / 2.0;
        } else {
            double pad = range * 0.12;
            minVal -= pad;
            maxVal += pad;
            range = maxVal - minVal;
        }

        // Draw 4 horizontal grid lines and Y-axis rate labels
        int gridSteps = 4;
        for (int i = 0; i <= gridSteps; i++) {
            float fraction = i / (float) gridSteps;
            float y = topPad + fraction * chartHeight;
            canvas.drawLine(leftPad, y, width - rightPad, y, gridPaint);

            double labelVal = maxVal - fraction * range;
            String label = String.format(Locale.US, "%.4f", labelVal);
            canvas.drawText(label, dp(6f), y + dp(4f), axisTextPaint);
        }

        // Draw dashed average line
        float avgY = valueToY(avgRate.doubleValue(), minVal, range, topPad, chartHeight);
        canvas.drawLine(leftPad, avgY, width - rightPad, avgY, avgLinePaint);

        // Select line color based on trend direction
        int primaryColor;
        int gradientStart;
        if (trendDirection == TrendDirection.UP) {
            primaryColor = Color.parseColor("#16A34A");
            gradientStart = Color.argb(68, 22, 163, 74);
        } else if (trendDirection == TrendDirection.DOWN) {
            primaryColor = Color.parseColor("#DC2626");
            gradientStart = Color.argb(68, 220, 38, 38);
        } else {
            primaryColor = Color.parseColor("#2563EB");
            gradientStart = Color.argb(68, 37, 99, 235);
        }
        linePaint.setColor(primaryColor);

        int n = points.size();
        float[] xs = new float[n];
        float[] ys = new float[n];

        for (int i = 0; i < n; i++) {
            xs[i] = (n == 1)
                    ? leftPad + chartWidth / 2f
                    : leftPad + (i / (float) (n - 1)) * chartWidth;
            ys[i] = valueToY(points.get(i).getRate().doubleValue(), minVal, range, topPad, chartHeight);
        }

        // Build line & fill paths
        Path linePath = new Path();
        Path fillPath = new Path();
        linePath.moveTo(xs[0], ys[0]);
        fillPath.moveTo(xs[0], topPad + chartHeight);
        fillPath.lineTo(xs[0], ys[0]);

        for (int i = 1; i < n; i++) {
            linePath.lineTo(xs[i], ys[i]);
            fillPath.lineTo(xs[i], ys[i]);
        }
        fillPath.lineTo(xs[n - 1], topPad + chartHeight);
        fillPath.close();

        fillPaint.setShader(new LinearGradient(
                0f, topPad, 0f, topPad + chartHeight,
                gradientStart, Color.argb(4, 255, 255, 255),
                Shader.TileMode.CLAMP
        ));
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);

        // Draw X-axis date labels (start, middle, end)
        drawDateLabel(canvas, points.get(0).getDate(), xs[0], height - dp(8f), Paint.Align.LEFT);
        if (n >= 3) {
            int mid = n / 2;
            drawDateLabel(canvas, points.get(mid).getDate(), xs[mid], height - dp(8f), Paint.Align.CENTER);
        }
        if (n >= 2) {
            drawDateLabel(canvas, points.get(n - 1).getDate(), xs[n - 1], height - dp(8f), Paint.Align.RIGHT);
        }

        // Draw points & highlight min/max/selected
        float pointRadius = (n <= 10) ? dp(4.5f) : dp(3.0f);
        for (int i = 0; i < n; i++) {
            BigDecimal r = points.get(i).getRate();
            boolean isMin = r.compareTo(minRate) == 0;
            boolean isMax = r.compareTo(maxRate) == 0;
            boolean isSelected = (i == selectedIndex);

            if (n <= 10 || isMin || isMax || isSelected || i == 0 || i == n - 1) {
                if (isMax) {
                    dotPaint.setColor(Color.parseColor("#15803D"));
                } else if (isMin) {
                    dotPaint.setColor(Color.parseColor("#B91C1C"));
                } else {
                    dotPaint.setColor(primaryColor);
                }
                float radius = (isSelected || isMin || isMax) ? dp(5.5f) : pointRadius;
                canvas.drawCircle(xs[i], ys[i], radius, dotPaint);
                canvas.drawCircle(xs[i], ys[i], radius, dotStrokePaint);
            }
        }

        // Draw interactive tooltip for selected point
        if (selectedIndex >= 0 && selectedIndex < n) {
            HistoricalRatePoint sel = points.get(selectedIndex);
            String tooltip = sel.getDate() + " • " + sel.getRate().toPlainString() + " (" + pairLabel + ")";
            float textW = tooltipTextPaint.measureText(tooltip);
            float boxW = textW + dp(16f);
            float boxH = dp(24f);
            float boxX = Math.max(leftPad, Math.min(xs[selectedIndex] - boxW / 2f, width - rightPad - boxW));
            float boxY = dp(4f);

            RectF rect = new RectF(boxX, boxY, boxX + boxW, boxY + boxH);
            canvas.drawRoundRect(rect, dp(6f), dp(6f), tooltipBgPaint);
            canvas.drawText(tooltip, boxX + dp(8f), boxY + dp(16f), tooltipTextPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (points == null || points.isEmpty()) {
            return super.onTouchEvent(event);
        }
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            float leftPad = dp(64f);
            float rightPad = dp(18f);
            float chartWidth = Math.max(1f, getWidth() - leftPad - rightPad);
            float ratio = (event.getX() - leftPad) / chartWidth;
            ratio = Math.max(0f, Math.min(1f, ratio));
            int idx = Math.round(ratio * (points.size() - 1));
            if (idx != selectedIndex) {
                selectedIndex = idx;
                invalidate();
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void drawDateLabel(Canvas canvas, String fullDate, float x, float y, Paint.Align align) {
        Paint.Align oldAlign = axisTextPaint.getTextAlign();
        axisTextPaint.setTextAlign(align);
        String shortDate = (fullDate != null && fullDate.length() >= 5) ? fullDate.substring(0, 5) : fullDate;
        canvas.drawText(shortDate != null ? shortDate : "", x, y, axisTextPaint);
        axisTextPaint.setTextAlign(oldAlign);
    }

    private static float valueToY(double val, double minVal, double range, float topPad, float chartHeight) {
        double normalized = (val - minVal) / range;
        normalized = Math.max(0.0, Math.min(1.0, normalized));
        return (float) (topPad + (1.0 - normalized) * chartHeight);
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }
}
