/*
Round Calendar
Copyright (C) 2020 Mil Tolstoy <miltolstoy@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.miltolstoy.roundcalendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import androidx.core.graphics.ColorUtils;
import androidx.appcompat.widget.AppCompatImageView;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.DAY_OF_WEEK;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;


public class ClockView extends AppCompatImageView {

    private Map<String, Paint> paints;
    private ClockWidget clockWidget;
    private static final int backgroundColor = Color.TRANSPARENT;
    private static final int refreshTimeoutMillis = 1800000; // 30 minutes - minimal valid value
    private CalendarAdapter calendarAdapter = null;
    private boolean useCalendarColors = false;

    private TimeInfo sleepStartTime;
    private TimeInfo sleepEndTime;

    public ClockView(Context context) throws IllegalStateException {
        super(context);
        throw new IllegalStateException("Use another constructor");
    }

    public ClockView(Context context, Point screenSize, boolean useCalendarColors, TimeInfo sleepStartTime,
                     TimeInfo sleepEndTime) {
        super(context);
        this.useCalendarColors = useCalendarColors;
        this.sleepStartTime = sleepStartTime;
        this.sleepEndTime = sleepEndTime;
        clockWidget = new ClockWidget(screenSize);
        paints = initPaints();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(backgroundColor);
        drawClock(canvas);
        drawDate(canvas);
        if (calendarAdapter != null) {
            drawEvents(canvas);
            if (!calendarAdapter.isCalendarShifted()) {
                drawHand(canvas);
            }
        }

        postInvalidateDelayed(refreshTimeoutMillis);
    }

    void setCalendarAdapter(CalendarAdapter adapter) {
        calendarAdapter = adapter;
        invalidate();
    }


    private Map<String, Paint> initPaints() {
        Map <String, Paint> paints = new HashMap<>();

        Paint dotsPaint = new Paint();
        dotsPaint.setColor(clockWidget.getBorderColor());
        paints.put("dots", dotsPaint);

        Paint fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(clockWidget.getFillColor());
        paints.put("fill", fillPaint);

        Paint borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(clockWidget.getBorderColor());
        borderPaint.setStrokeWidth(clockWidget.getBorderWidth());
        paints.put("border", borderPaint);

        Paint handPaint = new Paint();
        handPaint.setColor(Color.RED);
        handPaint.setStrokeWidth(clockWidget.getHandWidth());
        paints.put("hand", handPaint);

        Paint smallDigitsPaint = new Paint();
        smallDigitsPaint.setTextSize(clockWidget.getSmallDigitSize());
        smallDigitsPaint.setTextAlign(Paint.Align.CENTER);
        smallDigitsPaint.setColor(clockWidget.getDigitColor());
        paints.put("smallDigits", smallDigitsPaint);

        Paint bigDigitsPaint = new Paint();
        bigDigitsPaint.setTextSize(clockWidget.getBigDigitSize());
        bigDigitsPaint.setTextAlign(Paint.Align.CENTER);
        bigDigitsPaint.setColor(clockWidget.getDigitColor());
        paints.put("bigDigits", bigDigitsPaint);

        Paint datePaint = new Paint();
        datePaint.setTextSize(clockWidget.getDateSize());
        datePaint.setTextAlign(Paint.Align.LEFT);
        datePaint.setColor(clockWidget.getDigitColor());
        paints.put("date", datePaint);

        Paint eventLinePaint = new Paint();
        eventLinePaint.setColor(clockWidget.getEventArcColor()); // can be redefined in case of calendar color usage
        eventLinePaint.setAlpha(100);
        eventLinePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        paints.put("eventLine", eventLinePaint);

        Paint sleepEventLinePaint = new Paint();
        sleepEventLinePaint.setColor(Color.GRAY);
        sleepEventLinePaint.setAlpha(100);
        sleepEventLinePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        paints.put("sleepEventLine", sleepEventLinePaint);

        Paint textTitlePaint = new Paint();
        textTitlePaint.setTextSize(clockWidget.getTitleSize());
        textTitlePaint.setColor(clockWidget.getEventTitleColor());
        paints.put("title", textTitlePaint);

        for (Paint p : paints.values()) {
            p.setAntiAlias(true);
        }

        return paints;
    }

    private void drawClock(Canvas canvas) {
//        body
        canvas.drawCircle(clockWidget.getCenter().x,
                clockWidget.getCenter().y,
                clockWidget.getRadius(),
                paints.get("fill"));
//        border
        canvas.drawCircle(clockWidget.getCenter().x,
                clockWidget.getCenter().y,
                clockWidget.getRadius(),
                paints.get("border"));
//        markers
        List<List<Point>> markers = clockWidget.getHourMarkersCoordinates();
        for (List<Point> marker : markers) {
            canvas.drawLine(marker.get(0).x, marker.get(0).y,
                    marker.get(1).x, marker.get(1).y,
                    paints.get("border"));
        }
//        dots
        List<Point> dots = clockWidget.getHourDotsCoordinates();
        for (Point dot : dots) {
            canvas.drawCircle(dot.x, dot.y, clockWidget.getDotRadius(), paints.get("dots"));
        }
//        digits
        List<Point> digits = clockWidget.getDigitsCoordinates();
        for (int i = 0; i < digits.size(); i++) {
            Paint paint;
            if (i % 3 == 0) {
                paint = paints.get("bigDigits");
            }
            else {
                paint = paints.get("smallDigits");
            }
            Point coords = digits.get(i);
            canvas.drawText(Integer.toString(i), coords.x, coords.y, paint);
        }
    }

    private void drawHand(Canvas canvas) {
        List<Point> hand = clockWidget.getCurrentTimeHandCoordinates();
        canvas.drawLine(hand.get(0).x, hand.get(0).y, hand.get(1).x, hand.get(1).y, paints.get("hand"));
        canvas.drawCircle(hand.get(0).x, hand.get(0).y, clockWidget.getDotRadius(), paints.get("dots"));
    }

    private void drawDate(Canvas canvas) {
        Calendar calendar = calendarAdapter.getDayStartCalendar();
        String date = String.format(Locale.US, "%2d.%2d.%d", calendar.get(Calendar.DAY_OF_MONTH),
                (calendar.get(Calendar.MONTH) + 1), calendar.get(YEAR)).replace(' ', '0');
        Point datePoint = clockWidget.getDateCoordinates();
        canvas.drawText(date, datePoint.x, datePoint.y, paints.get("date"));

        DateFormatSymbols dateFormatSymbols = new DateFormatSymbols();
        String[] dayNames = dateFormatSymbols.getShortWeekdays();
        int dayNumber = calendar.get(DAY_OF_WEEK);
        Point dayOfWeekPoint = clockWidget.getDayOfWeekCoordinates();
        canvas.drawText(dayNames[dayNumber], dayOfWeekPoint.x, dayOfWeekPoint.y, paints.get("date"));
    }

    private void drawEvents(Canvas canvas) {
        RectF widgetCircle = clockWidget.getWidgetCircleObject();

        for (Event event : getSleepEvents()) {
            ClockWidget.EventDegreeData degrees = clockWidget.getEventDegrees(event);
            canvas.drawArc(widgetCircle, degrees.getStart(), degrees.getSweep(), true, paints.get("sleepEventLine"));
        }

        List<Event> todayEvents = calendarAdapter.getTodayEvents();
        List<List<Event>> sameTimeEventsList = extractSameTimeEvents(todayEvents);
        for (List<Event> sameTimeEvents : sameTimeEventsList) {
            drawSameTimeEvents(canvas, widgetCircle, sameTimeEvents);
        }

        List<Event> allDayEvents = new ArrayList<>();
        for (Event event : todayEvents) {
            if (event.isAllDay()) {
                allDayEvents.add(event);
                continue;
            }
            drawEvent(canvas, widgetCircle, event);
        }

        if (allDayEvents.isEmpty()) {
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("All-day: ");
        for (Event event : allDayEvents) {
            builder.append(event.getTitle());
            builder.append(", ");
        }
        builder.setLength(builder.length() - 2); // cut out last comma
        Point allDayEventsPoint = clockWidget.getAllDayEventListCoordinates();
        canvas.drawText(cutAllDayEventsTitlesIfNeeded(builder.toString()), allDayEventsPoint.x, allDayEventsPoint.y,
                paints.get("title"));
    }

    private List<Event> getSleepEvents() {
        Calendar calendar = Calendar.getInstance();
        List<Event> events = new ArrayList<>();

        calendar.set(calendar.get(YEAR), calendar.get(MONTH), calendar.get(DAY_OF_MONTH), sleepStartTime.getHours(),
                    sleepStartTime.getMinutes(), 0);
        long startTimeBeforeMidnight = calendar.getTimeInMillis();
        calendar.set(calendar.get(YEAR), calendar.get(MONTH), calendar.get(DAY_OF_MONTH) + 1, 0, 0, 0);
        long endTimeBeforeMidnight = calendar.getTimeInMillis();
        long beforeMidnightduration = endTimeBeforeMidnight - startTimeBeforeMidnight;
        events.add(new Event("sleep before midnight", startTimeBeforeMidnight, endTimeBeforeMidnight,
                beforeMidnightduration, false));

        calendar.set(calendar.get(YEAR), calendar.get(MONTH), calendar.get(DAY_OF_MONTH), 0, 0, 0);
        long startTimeAfterMidnight = calendar.getTimeInMillis();
        calendar.set(calendar.get(YEAR), calendar.get(MONTH), calendar.get(DAY_OF_MONTH), sleepEndTime.getHours(),
                sleepEndTime.getMinutes(), 0);
        long endTimeAfterMidnight = calendar.getTimeInMillis();
        long afterMidnightDuration = endTimeAfterMidnight - startTimeAfterMidnight;
        events.add(new Event("sleep after midnight", startTimeAfterMidnight, endTimeAfterMidnight,
                afterMidnightDuration, false));

        return events;
    }

    private String cutEventTitleIfNeeded(String title) {
        return normalizeEventTitle(title, clockWidget.getRadius());
    }

    private String cutAllDayEventsTitlesIfNeeded(String titles) {
        return normalizeEventTitle(titles, clockWidget.getWidgetWidth());
    }

    private String normalizeEventTitle(String title, float maxWidth) {
        float textWidth = paints.get("title").measureText(title);
        if (textWidth > maxWidth) {
            float maxSymbols = (maxWidth * title.length()) / textWidth;
            maxSymbols -= 4; // "..." + one padding char
            title = title.substring(0, Math.round(maxSymbols));
            title += "...";
        }
        return title;
    }

    private void drawEvent(Canvas canvas, RectF widgetCircle, Event event) {
        drawEventGeneralized(canvas, widgetCircle, clockWidget.getEventDegrees(event), event.getColor(),
                event.isFinishedInFirstDayHalf(), event.getTitle());
    }

    private void drawSameTimeEvents(Canvas canvas, RectF widgetCircle, List<Event> events) {
        StringBuilder titleBuilder = new StringBuilder();
        titleBuilder.append(events.size());
        titleBuilder.append(": ");
        for (Event event : events) {
            titleBuilder.append(event.getTitle());
            titleBuilder.append(", ");
        }
        titleBuilder.setLength(titleBuilder.length() - 2); // cut out last comma

        Event event = events.get(0);
        drawEventGeneralized(canvas, widgetCircle, clockWidget.getEventDegrees(event), event.getColor(),
                event.isFinishedInFirstDayHalf(), titleBuilder.toString());
    }

    private void drawEventGeneralized(Canvas canvas, RectF widgetCircle, ClockWidget.EventDegreeData degrees, int color,
                                      boolean isFinishedFirstDayHalf, String title) {
        Paint eventPaint = paints.get("eventLine");
        if (useCalendarColors) {
            eventPaint.setColor(color);
        }

        final float minSweep = (float) 0.5;
        float sweepAngle = Math.max(degrees.getSweep(), minSweep);
        float startAngle = degrees.getStart();
        canvas.drawArc(widgetCircle, startAngle, sweepAngle, true, eventPaint);

        Point widgetCenter = clockWidget.getCenter();
        List<Point> circlePoints = clockWidget.calculateEventCirclePoints(startAngle + 90, sweepAngle);
        eventPaint.setColor(ColorUtils.blendARGB(eventPaint.getColor(), Color.BLACK, 0.1f));
        canvas.drawLine(widgetCenter.x, widgetCenter.y, circlePoints.get(0).x, circlePoints.get(0).y, eventPaint);
        canvas.drawLine(widgetCenter.x, widgetCenter.y, circlePoints.get(1).x, circlePoints.get(1).y, eventPaint);

        canvas.save();

        final String titleNormalized = cutEventTitleIfNeeded(title);
        /*
            α = arcsin(l / (2 * R)) * 360 / π
            where l - horde length (text height)
        */
        double titleTextAngle = Math.toDegrees(Math.asin(Math.toRadians(calculateTextHeight(titleNormalized) /
                (2 * clockWidget.getRadius())))) * (double) 360 / Math.PI;
        titleTextAngle /= 2; // half of text angle is needed to center it
        float titleAngle = degrees.getStart() + degrees.getSweep() / 2 + 90;
        final float rotateAngle;
        final int padding;
        if (isFinishedFirstDayHalf)
        {
            titleAngle += (float) titleTextAngle; // move forward on half of text angle
            rotateAngle = titleAngle - 90;
            padding = calculateTextWidth(titleNormalized); // title text: center->radius
        } else {
            titleAngle -= (float) titleTextAngle; // move backward on half of text angle
            rotateAngle = titleAngle - 270;
            padding = 0; // title text: radius->center
        }

        Point eventTitlePoint = clockWidget.calculateEventTitlePoint(titleAngle, padding);
        canvas.rotate(rotateAngle, eventTitlePoint.x, eventTitlePoint.y);
        canvas.drawText(titleNormalized, eventTitlePoint.x, eventTitlePoint.y, paints.get("title"));
        canvas.restore();
    }

    private int calculateTextWidth(String text) {
        Rect bounds = new Rect();
        paints.get("title").getTextBounds(text, 0, text.length(), bounds);
        return bounds.width();
    }

    private int calculateTextHeight(String text) {
        Rect bounds = new Rect();
        paints.get("title").getTextBounds(text, 0, text.length(), bounds);
        return bounds.height();
    }

    private List<List<Event>> extractSameTimeEvents(List<Event> events) {
        List<List<Event>> result = findSameTimeEvents(events);
        for (List<Event> sameTimeEvents : result) {
            removeEventsFromList(events, sameTimeEvents);
        }
        return result;
    }

    private List<List<Event>> findSameTimeEvents(List<Event> events) {
        List<List<Event>> result = new ArrayList<>();
        List<Integer> foundIndexes = new ArrayList<>();
        for (int outerIndex = 0; outerIndex < events.size(); outerIndex++) {
            Event event = events.get(outerIndex);
            if (event.isAllDay()) {
                continue;
            }
            String startTime = event.getStartTime();
            String finishTime = event.getFinishTime();
            List<Event> sameTimeEvents = new ArrayList<>();
            sameTimeEvents.add(event);
            for (int innerIndex = 0; innerIndex < events.size(); innerIndex++) {
                if ((innerIndex == outerIndex) || foundIndexes.contains(innerIndex)) {
                    continue;
                }
                Event anotherEvent = events.get(innerIndex);
                if (anotherEvent.getStartTime().equals(startTime) && anotherEvent.getFinishTime().equals(finishTime)) {
                    sameTimeEvents.add(anotherEvent);
                    foundIndexes.add(innerIndex);
                }
            }
            if (sameTimeEvents.size() > 1) {
                result.add(sameTimeEvents);
                foundIndexes.add(outerIndex);
            }
        }
        return result;
    }

    private void removeEventsFromList(List<Event> eventsList, List<Event> eventsToRemove) {
        for (Iterator<Event> iter = eventsList.listIterator(); iter.hasNext(); ) {
            if (eventsToRemove.contains(iter.next())) {
                iter.remove();
            }
        }
    }
}
