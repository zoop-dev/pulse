package nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;

public class ActivitySummaryTableBuilder {
    private final String group;
    private final String headerId;
    private final List<String> headerColumns;
    private final Map<String, List<ActivitySummaryValue>> rows = new LinkedHashMap<>();

    public ActivitySummaryTableBuilder(final String group, final String headerId, final List<String> headerColumns) {
        this.group = group;
        this.headerId = headerId;
        this.headerColumns = headerColumns;
    }

    public void addRow(final String rowId, final List<ActivitySummaryValue> row) {
        if (row.size() != headerColumns.size()) {
            throw new IllegalArgumentException("Invalid number of row columns " + row.size());
        }

        rows.put(rowId, row);
    }

    public void addToSummaryData(final ActivitySummaryData summaryData) {
        final int numColumns = headerColumns.size();
        final boolean[] anyNonNull = new boolean[numColumns];

        for (final List<ActivitySummaryValue> row : rows.values()) {
            for (int i = 0; i < numColumns; i++) {
                anyNonNull[i] |= row.get(i) != null;
            }
        }

        final List<ActivitySummaryValue> finalHeader = new ArrayList<>(numColumns);
        for (int i = 0; i < numColumns; i++) {
            if (anyNonNull[i]) {
                finalHeader.add(new ActivitySummaryValue(headerColumns.get(i)));
            }
        }

        summaryData.add(
                headerId,
                new ActivitySummaryTableRowEntry(
                        group,
                        finalHeader,
                        true,
                        true
                )
        );

        for (final Map.Entry<String, List<ActivitySummaryValue>> e : rows.entrySet()) {
            final String key = e.getKey();
            final List<ActivitySummaryValue> row = e.getValue();
            final List<ActivitySummaryValue> finalRow = new ArrayList<>(numColumns);

            for (int i = 0; i < numColumns; i++) {
                if (anyNonNull[i]) {
                    finalRow.add(row.get(i));
                }
            }

            summaryData.add(
                    key,
                    new ActivitySummaryTableRowEntry(
                            group,
                            finalRow,
                            false,
                            true
                    )
            );
        }
    }
}
